package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.btcchina.syncer.dao.TransactionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaTransaction;
import com.xeiam.xchange.btcchina.dto.trade.response.BTCChinaTransactionsResponse;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaTradeServiceRaw;

@Component
public class TransactionSyncer extends AbstractSyncer {

	private static final Logger log = Logger.getLogger(TransactionSyncer.class.getName());

	private final BTCChinaTradeServiceRaw rawTradeService;
	private final TransactionDao transactionDao;
	private long lastId;

	@Autowired
	public TransactionSyncer(
			BTCChinaTradeServiceRaw rawTradeService,
			TransactionDao transactionDao,
			@Value("${btcchina.transaction.interval}") long interval) {
		super(interval);
		this.rawTradeService = rawTradeService;
		this.transactionDao = transactionDao;
	}

	@Override
	protected void init() {
		lastId = transactionDao.getLastId();
		log.log(Level.FINE, "Last ID: {0}", lastId);
	}

	@Override
	protected void sync() throws IOException {
		List<BTCChinaTransaction> transactions = getTransactions();
		log.log(Level.FINER, "transactions.count: {0}", transactions.size());
		transactionDao.merge(transactions);
		lastId = transactions.stream().map(t -> t.getId()).max(Long::compareTo).orElse(lastId);
	}

	private List<BTCChinaTransaction> getTransactions() throws IOException {
		int limit = 0, offset = 0, since = (int) lastId - 1;
		String type = "all", sincetype = "id";
		BTCChinaTransactionsResponse resp = rawTradeService.getTransactions(
				type, limit, offset, since, sincetype);
		List<BTCChinaTransaction> transactions = resp.getResult().getTransactions();

		long minId = transactions.stream().mapToLong(t -> t.getId()).min().getAsLong();
		if (minId > lastId) {
			throw new IllegalStateException("lastId: " + lastId + ", minId: " + minId);
		}

		return transactions;
	}

}
