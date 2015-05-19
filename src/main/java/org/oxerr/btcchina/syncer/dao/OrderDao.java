package org.oxerr.btcchina.syncer.dao;

import java.math.BigDecimal;
import java.util.List;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;

public interface OrderDao {

	long getLastId();

	List<BTCChinaOrder> getOrders(String status, long sinceId, long limit);

	/**
	 * Returns orders that
	 * status = {@code status}
	 * and id {@literal >} {@code sinceId}
	 * and price {@literal >=} {@code low}
	 * and price {@literal <=} {@code high}.
	 *
	 * @param status the status: open | closed | cancelled | pending | error | insufficient_balance.
	 * @param low the lowest price(inclusive).
	 * @param high the highest price(inclusive).
	 * @param sinceId the order id(exclusive).
	 * @param limit the maximum count of return list.
	 * @return orders match the condition.
	 */
	List<BTCChinaOrder> getOrders(String status,
		BigDecimal low, BigDecimal high, long sinceId, long limit);

	void insert(Iterable<BTCChinaOrder> orders);

	void update(BTCChinaOrder order);

}
