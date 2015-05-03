package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oxerr.btcchina.syncer.dao.OrderDao;
import org.oxerr.btcchina.syncer.service.BTCChinaTradeServiceRawExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaTradeServiceRaw;

@Component
public class OrderSyncer extends AbstractSyncer {

	private final Logger logger = Logger.getLogger(OrderSyncer.class.getName());

	private final BTCChinaTradeServiceRaw rawTradeService;
	private final BTCChinaTradeServiceRawExt extRawTradeService;
	private final OrderDao orderDao;
	private final String market;
	private long lastId;
	private long startOffset = 0;
	private int limit = 1_000;
	private final long intervalBetweenOrders;

	@Autowired
	public OrderSyncer(
			BTCChinaTradeServiceRaw rawTradeService,
			BTCChinaTradeServiceRawExt extRawTradeService,
			OrderDao orderDao,
			@Value("${btcchina.order.interval}") long interval,
			@Value("${btcchina.order.market}") String market,
			@Value("${btcchina.order.startOffset}") long startOffset,
			@Value("${btcchina.order.limit}") int limit,
			@Value("${btcchina.order.intervalBetweenOrders}") long intervalBetweenOrders) {
		super(interval);
		this.rawTradeService = rawTradeService;
		this.extRawTradeService = extRawTradeService;
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
				syncOrders("pending");
			}
			if (!Thread.interrupted()) {
				syncOrders("open");
			}
		} catch (InterruptedException e) {
			logger.warning(e.getMessage());
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

	private void syncOrders(String status) {
		logger.log(Level.FINE, "Syncing {0} orders...", status);
		List<BTCChinaOrder> orders;
		long sinceId = 0;
		do {
			orders = orderDao.getOrders(status, sinceId, limit);
			if (!orders.isEmpty()) {
				syncOrders(orders);
				sinceId = orders.get(orders.size() - 1).getId();
			}
		} while (!Thread.interrupted() && orders.size() > 0);
	}

	private void syncOrders(List<BTCChinaOrder> orders) {
		orders.forEach(order -> {
			syncOrder(order);
			try {
				Thread.sleep(intervalBetweenOrders);
			} catch (Exception e) {
				logger.fine(e.getMessage());
				Thread.currentThread().interrupt();
			}
		});
	}

	private void syncOrder(BTCChinaOrder order) {
		try {
			BTCChinaOrder newStatus = this.rawTradeService.getBTCChinaOrder(order.getId(), market, Boolean.TRUE).getResult().getOrder();
			logger.log(Level.FINEST, "Syncing order {0}({1}): {2} -> {3}",
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
			logger.log(Level.WARNING, e.getMessage());
			try {
				TimeUnit.MINUTES.sleep(1);
			} catch (InterruptedException ie) {
				logger.fine(e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

}
