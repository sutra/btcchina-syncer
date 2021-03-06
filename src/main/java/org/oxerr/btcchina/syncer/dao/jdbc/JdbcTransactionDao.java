package org.oxerr.btcchina.syncer.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.oxerr.btcchina.syncer.dao.TransactionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaTransaction;

@Repository
public class JdbcTransactionDao extends JdbcDaoSupport implements
		TransactionDao {

	private static final String GET_MAX_ID_SQL = "select max(id) from transaction";
	private static final String GET_IDS_SQL = "select id from transaction where id between ? and ?";
	private static final String INSERT_TRANSACTION_SQL = "insert into transaction(id, date, type, amount, money) values(?, ?, ?, ?, ?)";

	@Autowired
	public JdbcTransactionDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getLastId() {
		Long lastId = getJdbcTemplate().queryForObject(GET_MAX_ID_SQL, Long.class);
		return lastId == null ? 0L : lastId.longValue();
	}

	@Override
	public List<Long> getIds(long start, long end) {
		return getJdbcTemplate().queryForList(
			GET_IDS_SQL,
			Long.class, start, end);
	}

	@Override
	@Transactional
	public int merge(Collection<BTCChinaTransaction> transactions) {
		long start = transactions.stream().map(t -> t.getId()).min(Long::compareTo).orElse(0L);
		long end =  transactions.stream().map(t -> t.getId()).max(Long::compareTo).orElse(0L);

		List<Long> ids = getIds(start, end);

		return insert(transactions.stream().filter(t -> !ids.contains(t.getId())).collect(Collectors.toList())).length;
	}

	@Override
	public int[] insert(Collection<BTCChinaTransaction> transactions) {
		return insert(transactions.toArray(new BTCChinaTransaction[0]));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int[] insert(BTCChinaTransaction[] transactions) {
		return getJdbcTemplate().batchUpdate(
			INSERT_TRANSACTION_SQL,
			new BatchPreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					BTCChinaTransaction t = transactions[i];
					ps.setLong(1, t.getId());
					ps.setTimestamp(2, Timestamp.from(Instant.ofEpochSecond(t.getDate())));
					ps.setString(3, t.getType());
					if (t.getType().contains("ltc")) {
						ps.setBigDecimal(4, t.getLtcAmount());
					} else {
						ps.setBigDecimal(4, t.getBtcAmount());
					}
					ps.setBigDecimal(5, t.getCnyAmount());
				}

				@Override
				public int getBatchSize() {
					return transactions.length;
				}
			}
		);
	}

}
