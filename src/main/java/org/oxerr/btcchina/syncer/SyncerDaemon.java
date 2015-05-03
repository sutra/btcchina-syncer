package org.oxerr.btcchina.syncer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.oxerr.btcchina.syncer.syncer.Syncer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SyncerDaemon implements Daemon {

	private final Logger logger = Logger.getLogger(SyncerDaemon.class.getName());
	private final ThreadGroup threadGroup = new ThreadGroup("syncers");
	private final List<Thread> syncerThreads = new ArrayList<>();
	private ClassPathXmlApplicationContext ctx;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(DaemonContext context) throws DaemonInitException,
			Exception {
		logger.info("Initializing...");

		String[] names = context.getArguments();
		logger.log(Level.INFO, "syncers: {0}", Arrays.toString(names));

		ctx = new ClassPathXmlApplicationContext(
			"classpath:META-INF/spring/applicationContext.xml");

		Arrays.stream(names).forEach(
			name -> syncerThreads.add(
				new Thread(threadGroup, ctx.getBean(name, Syncer.class), name)));

		logger.info("Initialized.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() throws Exception {
		logger.info("Starting...");

		ctx.start();
		syncerThreads.forEach(thread -> thread.start());

		logger.info("Started.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception {
		logger.info("Stopping...");

		syncerThreads.forEach(thread -> thread.interrupt());
		ctx.stop();

		logger.info("Stopped.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		logger.info("Destroying...");

		ctx.close();
		join(threadGroup);

		logger.info("Destroyed.");
	}

	private void join(ThreadGroup threadGroup) {
		for (Thread thread : getThreads(threadGroup)) {
			logger.log(Level.FINE, "Wating {0} to die...", thread.getName());
			try {
				thread.join();
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, e.getMessage());
			}
			logger.log(Level.FINE, "{0} is dead.", thread.getName());
		}
	}

	private static Thread[] getThreads(ThreadGroup threadGroup) {
		final Thread[] threads = new Thread[threadGroup.activeCount()];
		threadGroup.enumerate(threads);
		return threads;
	}

}
