package org.oxerr.btcchina.syncer.dao;

import com.xeiam.xchange.btcchina.dto.marketdata.BTCChinaTrade;

public interface TradeDao {

	long getLastId();

	int[] insert(BTCChinaTrade[] trades);

}
