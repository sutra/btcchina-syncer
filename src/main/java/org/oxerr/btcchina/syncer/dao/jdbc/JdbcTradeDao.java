package org.oxerr.btcchina.syncer.dao.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import javax.sql.DataSource;

import org.oxerr.btcchina.syncer.dao.TradeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

import com.xeiam.xchange.btcchina.dto.marketdata.BTCChinaTrade;

@Repository
public class JdbcTradeDao extends JdbcDaoSupport implements TradeDao {

	private static final String GET_MAX_TID_SQL = "select max(tid) from trade";
	private static final String INSERT_TRADE_SQL = "insert into trade(tid, date, type, amount, price) values(?, ?, ?, ?, ?)";

	@Autowired
	public JdbcTradeDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getLastId() {
		Long lastId = getJdbcTemplate().queryForObject(GET_MAX_TID_SQL, Long.class);
		return lastId == null ? 0L : lastId.longValue();
	}

	@Override
	public int[] insert(BTCChinaTrade[] trades) {
		return getJdbcTemplate().batchUpdate(INSERT_TRADE_SQL,
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					BTCChinaTrade trade = trades[i];
					ps.setLong(1, trade.getTid());
					ps.setTimestamp(2, Timestamp.from(Instant.ofEpochSecond(trade.getDate())));
					ps.setString(3, trade.getOrderType());
					ps.setBigDecimal(4, trade.getAmount());
					ps.setBigDecimal(5, trade.getPrice());
				}

				@Override
				public int getBatchSize() {
					return trades.length;
				}
			}
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BigDecimal getLow(long beginTid, long endTid) {
		return getJdbcTemplate().queryForObject(
			"select min(price) from trade where tid >= ? and tid <= ?",
			BigDecimal.class,
			beginTid,
			endTid);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BigDecimal getHigh(long beginTid, long endTid) {
		return getJdbcTemplate().queryForObject(
			"select max(price) from trade where tid >= ? and tid <= ?",
			BigDecimal.class,
			beginTid,
			endTid);
	}

}
