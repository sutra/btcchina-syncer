package org.oxerr.btcchina.syncer.dao.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.oxerr.btcchina.syncer.dao.OrderDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;
import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrderDetail;

@Repository
public class JdbcOrderDao extends JdbcDaoSupport implements OrderDao {

	private final Logger logger = Logger.getLogger(JdbcOrderDao.class.getName());

	private final String INSERT_ORDER_SQL = "insert into \"order\"(id, date, type, price, currency, amount, amount_original, status) values(?, ?, ?, ?, ?, ?, ?, ?)";
	private final String UPDATE_ORDER_SQL = "update \"order\" set amount = ?, status = ? where id = ?";
	private final String INSERT_DETAIL_SQL = "insert into order_detail(id, order_id, date, amount, price) values(?, ?, ?, ?, ?)";

	@Autowired
	public JdbcOrderDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getLastId() {
		return getJdbcTemplate().queryForObject("select max(id) from \"order\"",
				Long.class);
	}

	@Override
	public List<BTCChinaOrder> getOrders(String status, long sinceId, long limit) {
		logger.log(Level.FINEST, "getOrders({0}, {1}, {2})",
			new Object[] {
				status, sinceId, limit,
			}
		);
		return getJdbcTemplate().query(
			"select id, date, type, price, currency, amount, amount_original, status from \"order\" where status = ? and id > ? order by id limit ?",
			new RowMapper<BTCChinaOrder>() {
				@Override
				public BTCChinaOrder mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					int id = rs.getInt("id");
					String type = rs.getString("type");
					BigDecimal price = rs.getBigDecimal("price");
					String currency = rs.getString("currency");
					BigDecimal amount = rs.getBigDecimal("amount");
					BigDecimal amountOriginal = rs.getBigDecimal("amount_original");
					long date = rs.getTimestamp("date").toInstant().getEpochSecond();
					String status = rs.getString("status");
					BTCChinaOrder order = new BTCChinaOrder(id, type, price, currency, amount, amountOriginal, date, status, null);
					return order;
				}
			}, status, sinceId, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void insert(Iterable<BTCChinaOrder> orders) {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		PreparedStatement orderPs = null, detailPs = null;
		try {
			con.setAutoCommit(false);
			orderPs = con.prepareStatement(INSERT_ORDER_SQL);
			detailPs = con.prepareStatement(INSERT_DETAIL_SQL);
			insert(con, orderPs, detailPs, orders);
		} catch (SQLException e) {
			throw getExceptionTranslator().translate("insert orders", null, e);
		} finally {
			JdbcUtils.closeStatement(orderPs);
			JdbcUtils.closeStatement(detailPs);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	private void insert(
			Connection con,
			PreparedStatement orderPs,
			PreparedStatement detailPs,
			Iterable<BTCChinaOrder> orders)
			throws SQLException {
		for (BTCChinaOrder order : orders) {
			orderPs.setLong(1, order.getId());
			orderPs.setTimestamp(2, Timestamp.from(Instant.ofEpochSecond(order.getDate())));
			orderPs.setString(3, order.getType());
			orderPs.setBigDecimal(4, order.getPrice());
			orderPs.setString(5, order.getCurrency());
			orderPs.setBigDecimal(6, order.getAmount());
			orderPs.setBigDecimal(7, order.getAmountOriginal());
			orderPs.setString(8, order.getStatus());
			orderPs.addBatch();

			if (order.getDetails() != null) {
				for (BTCChinaOrderDetail detail : order.getDetails()) {
					detailPs.setObject(1, UUID.randomUUID());
					detailPs.setLong(2, order.getId());
					detailPs.setTimestamp(3, Timestamp.from(Instant.ofEpochSecond(detail.getDateline())));
					detailPs.setBigDecimal(4, detail.getAmount());
					detailPs.setBigDecimal(5, detail.getPrice());
					detailPs.addBatch();
				}
			}
		}
		orderPs.executeBatch();
		detailPs.executeBatch();
		con.commit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void update(BTCChinaOrder order) {
		logger.log(Level.FINEST, "Updating order {0}.", order.getId());
		if (order.getDetails() != null) {
			deleteDetails(order.getId());
			insertDetails(order.getId(), order.getDetails());
		}
		getJdbcTemplate().update(UPDATE_ORDER_SQL,
			order.getAmount(), order.getStatus(), order.getId());
	}

	public int[] insertDetails(long orderId, BTCChinaOrderDetail[] details) {
		logger.log(Level.FINEST, "Inserting order details of order {0}.", orderId);
		return getJdbcTemplate().batchUpdate(INSERT_DETAIL_SQL, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				BTCChinaOrderDetail detail = details[i];
				ps.setObject(1, UUID.randomUUID());
				ps.setLong(2, orderId);
				ps.setTimestamp(3, Timestamp.from(Instant.ofEpochSecond(detail.getDateline())));
				ps.setBigDecimal(4, detail.getAmount());
				ps.setBigDecimal(5, detail.getPrice());
			}

			@Override
			public int getBatchSize() {
				return details.length;
			}
		});
	}

	public void deleteDetails(long orderId) {
		logger.log(Level.FINEST, "Deleting order details of order {0}.", orderId);
		getJdbcTemplate().update("delete from order_detail where order_id = ?",
				orderId);
	}

}
