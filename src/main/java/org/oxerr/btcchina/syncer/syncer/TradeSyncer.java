package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.btcchina.syncer.dao.TradeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xeiam.xchange.btcchina.dto.marketdata.BTCChinaTrade;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaMarketDataServiceRaw;

@Component
public class TradeSyncer extends AbstractSyncer {

	private final Logger log = Logger.getLogger(TradeSyncer.class.getName());

	private final BTCChinaMarketDataServiceRaw rawMdService;
	private final TradeDao tradeDao;
	private final String market;
	private volatile int limit = 5_000;
	private long lastId;

	@Autowired
	public TradeSyncer(
			BTCChinaMarketDataServiceRaw rawMdService,
			TradeDao tradeDao,
			@Value("${btcchina.trade.interval}") long interval,
			@Value("${btcchina.trade.market}") String market) {
		super(interval);
		this.rawMdService = rawMdService;
		this.tradeDao = tradeDao;
		this.market = market;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	protected void init() {
		lastId = tradeDao.getLastId();
		if (lastId == 0) {
			// If pass 0, BTCChina will return the recent trades that is not
			// we expected. Pass -1, will return the trades from tid = 1.
			lastId = -1;
		}
		log.log(Level.FINE, "Last ID: {0}", lastId);
	}

	@Override
	protected void sync() throws IOException {
		BTCChinaTrade[] trades;
		do {
			log.log(Level.INFO, "Last ID: {0}", lastId);
			trades = rawMdService.getBTCChinaHistoryData(market, lastId, limit, "id");
			log.log(Level.FINE, "trades.length: {0}", trades.length);
			tradeDao.insert(trades);
			lastId = Arrays.stream(trades).mapToLong(t -> t.getTid()).max().orElse(lastId);
		} while (!Thread.interrupted() && trades.length > 0);
	}

}
