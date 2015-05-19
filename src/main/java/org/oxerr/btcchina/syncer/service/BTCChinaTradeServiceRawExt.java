package org.oxerr.btcchina.syncer.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;
import com.xeiam.xchange.btcchina.dto.trade.response.BTCChinaGetOrdersResponse;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaTradeServiceRaw;

public class BTCChinaTradeServiceRawExt {

	private static final long GENESIS_BLOCK_TIME = getGenesisBlockTime();
	private static final int GENESIS_BLOCK_TIME_SECOND = (int) (GENESIS_BLOCK_TIME / 1000);

	private static final long getGenesisBlockTime() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			return df.parse("2009-01-03 18:15:05").getTime();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private final Logger logger = Logger.getLogger(BTCChinaTradeServiceRawExt.class.getName());

	private final IOExceptionRetryService retryService;
	private final BTCChinaTradeServiceRaw rawTradeService;
	/**
	 * The batch size in querying from BTCChina,
	 * the maximum value is 1,000.
	 */
	private int batchSize = 1000;

	public BTCChinaTradeServiceRawExt(
			IOExceptionRetryService retryService,
			BTCChinaTradeServiceRaw rawTradeService) {
		this.retryService = retryService;
		this.rawTradeService = rawTradeService;
	}

	public BTCChinaOrder getOrder(int id, String market, Boolean withdetail)
			throws IOException {
		return retryService.retry(() -> rawTradeService.getBTCChinaOrder(id,
				market, Boolean.TRUE)).getResult().getOrder();
	}

	/**
	 * Returns the orders.
	 *
	 * @param market the market.
	 * @param sinceId the order ID which the orders in the result should be greater than.
	 * @param limit the maximum number of orders in result.
	 * @param startOffset the start offset to query orders, this is useful when you have a lot of orders.
	 * @return all orders which ID are greater than the specified ID.
	 * @throws IOException indicates I/O exception in getting orders from exchange, when the max retry times reached.
	 */
	public SortedSet<BTCChinaOrder> getOrders(
			final String market,
			final long sinceId,
			final int limit,
			final long startOffset) throws IOException {
		logger.log(Level.FINER, "market: {0}, sinceId: {1}, limit: {2}, startOffset: {3}",
				new Object[] { market, sinceId, limit, startOffset });

		final SortedSet<BTCChinaOrder> orders = new TreeSet<>((o1, o2) -> o1.getId() - o2.getId());
		SortedSet<BTCChinaOrder> currentOrders;
		long offset = startOffset;

		do {
			boolean hasGap = false;

			do {
				final BTCChinaGetOrdersResponse resp = getOrders(market, batchSize, offset);
				currentOrders = extractOrders(resp);

				hasGap = !currentOrders.isEmpty()
						&& !orders.isEmpty()
						&& orders.last().getId() < currentOrders.first().getId();

				if (hasGap) {
					offset--;
					if (offset < 0) {
						offset = 0;
						orders.clear();
					}
				}
			} while (!Thread.interrupted() && hasGap);

			if (Thread.interrupted()) {
				logger.log(Level.FINE, "Thread is interrupted, clear currentOrders.");
				currentOrders.clear();
			}

			orders.addAll(currentOrders);
			while (orders.size() > limit + batchSize) {
				orders.remove(orders.last());
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE,
					"orders.count: {0}, order range: {1} - {2}",
					new Object[] {
						orders.size(),
						orders.isEmpty() ? null : Instant.ofEpochSecond(orders.first().getDate()),
						orders.isEmpty() ? null : Instant.ofEpochSecond(orders.last().getDate()),
					});
			}

			offset += batchSize;
		} while (!Thread.interrupted() && currentOrders.size() == batchSize
				&& !orders.isEmpty() && orders.first().getId() > sinceId);

		if (Thread.interrupted()) {
			logger.log(Level.FINE, "Thread is interrupted, clear fetched orders.");
			orders.clear();
		}

		final SortedSet<BTCChinaOrder> ret = new TreeSet<>((o1, o2) -> o1.getId() - o2.getId());
		ret.addAll(orders
			.stream()
			.filter(o -> o.getId() > sinceId)
			.collect(Collectors.toSet())
		);
		logger.log(Level.FINER, "ret.count: {0}", ret.size());
		return ret;
	}

	private BTCChinaGetOrdersResponse getOrders(String market, int limit,
			long offset) throws IOException {
		return retryService.retry(() -> rawTradeService.getBTCChinaOrders(
				false, market, limit, (int) offset, GENESIS_BLOCK_TIME_SECOND, true));
	}

	private SortedSet<BTCChinaOrder> extractOrders(BTCChinaGetOrdersResponse resp) {
		SortedSet<BTCChinaOrder> orders = new TreeSet<>((o1, o2) -> o1.getId() - o2.getId());
		orders.addAll(Arrays.asList(resp.getResult().getOrdersArray()));
		for (Map.Entry<String, BTCChinaOrder[]> entry : resp.getResult().entrySet()) {
			orders.addAll(Arrays.asList(entry.getValue()));
		}
		return orders;
	}

}
