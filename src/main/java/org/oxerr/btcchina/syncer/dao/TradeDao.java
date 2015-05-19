package org.oxerr.btcchina.syncer.dao;

import java.math.BigDecimal;

import com.xeiam.xchange.btcchina.dto.marketdata.BTCChinaTrade;

public interface TradeDao {

	long getLastId();

	int[] insert(BTCChinaTrade[] trades);

	/**
	 * Returns the lowest price between the {@code beginTid} and the {@code endTid}.
	 *
	 * @param beginTid the begin trade ID(inclusive).
	 * @param endTid the end trade ID(inclusive).
	 * @return the lowest price.
	 */
	BigDecimal getLow(long beginId, long endId);

	/**
	 * Returns the highest price between the {@code beginTid} and the {@code endTid}.
	 *
	 * @param beginTid the begin trade ID(inclusive).
	 * @param endTid the end trade ID(inclusive).
	 * @return the highest price.
	 */
	BigDecimal getHigh(long beginTid, long endTid);

}
