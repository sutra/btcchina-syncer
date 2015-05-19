package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.btcchina.syncer.dao.OrderDao;
import org.oxerr.btcchina.syncer.dao.TradeDao;
import org.oxerr.btcchina.syncer.service.BTCChinaTradeServiceRawExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;

@Component
public class OrderSyncer extends AbstractSyncer {

	private final Logger log = Logger.getLogger(OrderSyncer.class.getName());

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
			BTCChinaTradeServiceRawExt extRawTradeService,
			TradeDao tradeDao,
			OrderDao orderDao,
			@Value("${btcchina.order.interval}") long interval,
			@Value("${btcchina.order.market}") String market,
			@Value("${btcchina.order.startOffset}") long startOffset,
			@Value("${btcchina.order.limit}") int limit,
			@Value("${btcchina.order.intervalBetweenOrders}") long intervalBetweenOrders) {
		super(interval);
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
		if (!Thread.interrupted()) {
			try {
				syncOrders();
			} catch (IOException e) {
				log.log(Level.WARNING, e.getMessage());
			}
		}
		if (!Thread.interrupted()) {
			try {
				syncOrders("pending", null, null);
			} catch (IOException e) {
				log.log(Level.WARNING, e.getMessage());
			}
		}
		if (!Thread.interrupted()) {
			final long currentSyncId = tradeDao.getLastId();
			final BigDecimal low = tradeDao.getLow(lastSyncTradeId, currentSyncId);
			final BigDecimal high = tradeDao.getHigh(lastSyncTradeId, currentSyncId);
			try {
				syncOrders("open", low, high);
				lastSyncTradeId = currentSyncId;
			} catch (IOException e) {
				log.log(Level.WARNING, e.getMessage());
			}
		}
	}

	private void syncOrders() throws IOException {
		SortedSet<BTCChinaOrder> orders;
		do {
			orders = extRawTradeService.getOrders(market, lastId, limit, startOffset);

			if (!orders.isEmpty()) {
				orderDao.insert(orders);
				lastId = orders.last().getId();
			}

			startOffset -= limit;
			if (startOffset < 0) {
				startOffset = 0;
			}
		} while (!Thread.interrupted() && orders.size() > limit);
	}

	private void syncOrders(String status, BigDecimal low, BigDecimal high)
			throws IOException {
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

	private void syncOrders(List<BTCChinaOrder> orders) throws IOException {
		for (BTCChinaOrder order : orders) {
			if (Thread.interrupted()) {
				log.log(Level.FINER, "Thread was interrupted.");
				break;
			}

			syncOrder(order);

			sleep(intervalBetweenOrders);
		}
	}

	private void syncOrder(BTCChinaOrder order) throws IOException {
		BTCChinaOrder newStatus = this.extRawTradeService.getOrder(order.getId(), market, Boolean.TRUE);
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
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {
			log.fine(e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

}
