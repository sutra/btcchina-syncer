package org.oxerr.btcchina.syncer.dao;

import java.util.Collection;
import java.util.List;

import com.xeiam.xchange.btcchina.dto.trade.BTCChinaTransaction;

public interface TransactionDao {

	long getLastId();

	List<Long> getIds(long start, long end);

	/**
	 * Insert the transactions if does not exist in database.
	 *
	 * @param transactions the transactions to be merged.
	 * @return count of records inserted.
	 */
	int merge(Collection<BTCChinaTransaction> transactions);

	int[] insert(Collection<BTCChinaTransaction> transactions);

	int[] insert(BTCChinaTransaction[] transactions);

}
