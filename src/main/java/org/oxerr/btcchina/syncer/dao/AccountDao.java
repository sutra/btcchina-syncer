package org.oxerr.btcchina.syncer.dao;

import com.xeiam.xchange.btcchina.dto.account.BTCChinaAccountInfo;

public interface AccountDao {

	void saveAccount(BTCChinaAccountInfo account);

}
