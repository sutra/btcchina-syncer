package org.oxerr.btcchina.syncer;

import org.apache.commons.daemon.support.DaemonLoader;

public class Main {

	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		DaemonLoader.load(SyncerDaemon.class.getName(), args);
		DaemonLoader.start();
	}

}
