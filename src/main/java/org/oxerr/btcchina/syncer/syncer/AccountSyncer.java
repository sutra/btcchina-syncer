package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.btcchina.syncer.dao.AccountDao;
import org.oxerr.btcchina.syncer.service.IOExceptionRetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.xeiam.xchange.btcchina.dto.account.BTCChinaAccountInfo;
import com.xeiam.xchange.btcchina.dto.account.response.BTCChinaGetAccountInfoResponse;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaAccountServiceRaw;

@Component
public class AccountSyncer {

	private final Logger log = Logger.getLogger(AccountSyncer.class.getName());

	private final IOExceptionRetryService retryService;
	private final BTCChinaAccountServiceRaw accountServiceRaw;
	private final AccountDao accountDao;

	@Autowired
	public AccountSyncer(IOExceptionRetryService retryService,
			BTCChinaAccountServiceRaw accountServiceRaw, AccountDao accountDao) {
		this.retryService = retryService;
		this.accountServiceRaw = accountServiceRaw;
		this.accountDao = accountDao;
	}

	public void sync() throws IOException {
		log.log(Level.INFO, "Syncing account...");
		BTCChinaGetAccountInfoResponse resp = retryService.retry(() -> accountServiceRaw.getBTCChinaAccountInfo());
		BTCChinaAccountInfo account = resp.getResult();
		accountDao.saveAccount(account);
	}

}
