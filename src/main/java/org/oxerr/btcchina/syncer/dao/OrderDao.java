package org.oxerr.btcchina.syncer.dao;

import java.util.List;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;

public interface OrderDao {

	long getLastId();

	List<BTCChinaOrder> getOrders(String status, long sinceId, long limit);

	void insert(Iterable<BTCChinaOrder> orders);

	void update(BTCChinaOrder order);

}
