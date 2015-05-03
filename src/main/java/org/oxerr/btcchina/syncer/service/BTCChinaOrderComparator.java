package org.oxerr.btcchina.syncer.service;

import java.util.Comparator;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaOrder;

public class BTCChinaOrderComparator implements Comparator<BTCChinaOrder> {

	public static final BTCChinaOrderComparator INSTANCE = new BTCChinaOrderComparator();

	private BTCChinaOrderComparator() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compare(BTCChinaOrder o1, BTCChinaOrder o2) {
		return o1.getId() - o2.getId();
	}

}
