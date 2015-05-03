package org.oxerr.btcchina.syncer.syncer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractSyncer implements Syncer {

	private final Logger logger = Logger.getLogger(AbstractSyncer.class.getName());
	private long interval;

	public AbstractSyncer(long interval) {
		this.interval = interval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		logger.info("Running...");
		init();

		while (!Thread.interrupted()) {
			try {
				sync();
				logger.log(Level.FINEST, "Sleeping {0} milliseconds.", interval);
				Thread.sleep(interval);
				logger.log(Level.FINEST, "Being awake.");
			} catch (IOException e) {
				logger.log(Level.WARNING, e.getMessage());
			} catch (InterruptedException e) {
				logger.fine(e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
		logger.info("exit.");
	}

	protected void init() {
	}

	protected abstract void sync() throws IOException;

}
