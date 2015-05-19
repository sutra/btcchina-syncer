package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.btcchina.syncer.dao.OrderDao;
import org.oxerr.btcchina.syncer.dao.TradeDao;
import org.oxerr.btcchina.syncer.service.BTCChinaTradeServiceRawExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaTradeServiceRaw;

@Component
public class OrderSyncer extends AbstractSyncer {

	private final Logger log = Logger.getLogger(OrderSyncer.class.getName());

	private final BTCChinaTradeServiceRaw rawTradeService;
	private final BTCChinaTradeServiceRawExt extRawTradeService;
	private final TradeDao tradeDao;
	private final OrderDao orderDao;
	private final String market;
	private final long intervalBetweenOrders;
	private long lastId;
	private long startOffset = 0;
	private int limit = 1_000;
	private long lastSyncTradeId;

	@Autowired
	public OrderSyncer(
			BTCChinaTradeServiceRaw rawTradeService,
			BTCChinaTradeServiceRawExt extRawTradeService,
			TradeDao tradeDao,
			OrderDao orderDao,
			@Value("${btcchina.order.interval}") long interval,
			@Value("${btcchina.order.market}") String market,
			@Value("${btcchina.order.startOffset}") long startOffset,
			@Value("${btcchina.order.limit}") int limit,
			@Value("${btcchina.order.intervalBetweenOrders}") long intervalBetweenOrders) {
		super(interval);
		this.rawTradeService = rawTradeService;
		this.extRawTradeService = extRawTradeService;
		this.tradeDao = tradeDao;
		this.orderDao = orderDao;
		this.market = market;
		this.startOffset = startOffset;
		this.limit = limit;
		this.intervalBetweenOrders = intervalBetweenOrders;
	}

	@Override
	protected void init() {
		lastId = orderDao.getLastId();
	}

	@Override
	protected void sync() {
		try {
			if (!Thread.interrupted()) {
				syncOrders();
			}
			if (!Thread.interrupted()) {
				syncOrders("pending", null, null);
			}
			if (!Thread.interrupted()) {
				long currentSyncId = tradeDao.getLastId();
				BigDecimal low = tradeDao.getLow(lastSyncTradeId, currentSyncId);
				BigDecimal high = tradeDao.getHigh(lastSyncTradeId, currentSyncId);
				syncOrders("open", low, high);
				lastSyncTradeId = currentSyncId;
			}
		} catch (InterruptedException e) {
			log.warning(e.getMessage());
			Thread.interrupted();
		}
	}

	private void syncOrders() throws InterruptedException {
		SortedSet<BTCChinaOrder> orders;
		do {
			try {
				orders = extRawTradeService.getOrders(market, lastId, limit, startOffset);

				if (!orders.isEmpty()) {
					orderDao.insert(orders);
					lastId = orders.last().getId();
				}

				startOffset -= limit;
				if (startOffset < 0) {
					startOffset = 0;
				}
			} catch (IOException e) {
				orders = Collections.emptySortedSet();
				TimeUnit.MINUTES.sleep(1);
			}
		} while (!Thread.interrupted() && orders.size() > limit);
	}

	private void syncOrders(String status, BigDecimal low, BigDecimal high) {
		log.log(Level.FINE, "Syncing {0}({1}-{2}) orders...",
			new Object[] { status, low, high, });
		List<BTCChinaOrder> orders;
		long sinceId = 0;
		do {
			if (low == null || high == null) {
				orders = orderDao.getOrders(status, sinceId, limit);
			} else {
				orders = orderDao.getOrders(status, low, high, sinceId, limit);
			}
			log.log(Level.FINE, "{0} orders to sync.", orders.size());
			if (!orders.isEmpty()) {
				syncOrders(orders);
				sinceId = orders.get(orders.size() - 1).getId();
			}
		} while (!Thread.interrupted() && orders.size() > 0);
	}

	private void syncOrders(List<BTCChinaOrder> orders) {
		orders.forEach(order -> {
			if (Thread.interrupted()) {
				log.log(Level.FINER, "Thread was interrupted.");
				return;
			}

			syncOrder(order);
			try {
				Thread.sleep(intervalBetweenOrders);
			} catch (Exception e) {
				log.fine(e.getMessage());
				Thread.currentThread().interrupt();
			}
		});
	}

	private void syncOrder(BTCChinaOrder order) {
		try {
			BTCChinaOrder newStatus = this.rawTradeService.getBTCChinaOrder(order.getId(), market, Boolean.TRUE).getResult().getOrder();
			log.log(Level.FINEST, "Syncing order {0}({1}): {2} -> {3}",
				new Object[] {
					order.getId(),
					Instant.ofEpochSecond(order.getDate()),
					order.getStatus(),
					newStatus.getStatus(),
				}
			);
			if (!newStatus.getStatus().equals(order.getStatus())) {
				orderDao.update(newStatus);
			}
		} catch (IOException e) {
			log.log(Level.WARNING, e.getMessage());
			try {
				TimeUnit.MINUTES.sleep(1);
			} catch (InterruptedException ie) {
				log.fine(e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

}
