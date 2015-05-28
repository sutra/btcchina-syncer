package org.oxerr.btcchina.syncer.dao.jdbc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.oxerr.btcchina.syncer.dao.AccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

import com.xeiam.xchange.btcchina.dto.BTCChinaValue;
import com.xeiam.xchange.btcchina.dto.account.BTCChinaAccountInfo;

@Repository
public class JdbcAccountDao extends JdbcDaoSupport implements AccountDao {

	private static final String INSERT_SQL = "insert into account(id, version, currency, date, balance, frozen, loan) values(?, ?, ?, ?, ?, ?, ?)";

	@Autowired
	public JdbcAccountDao(DataSource dataSource) {
		setDataSource(dataSource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveAccount(BTCChinaAccountInfo account) {
		final List<Object[]> rows = new ArrayList<>(account.getBalances().size());
		final Date now = Date.from(Instant.now());
		final long version = now.getTime();

		account.getBalances().forEach((currency, value) -> {
			final BigDecimal balance = getAmount(value);
			final BigDecimal frozen = getAmount(account.getFrozens().get(currency));
			final BigDecimal loan = getAmount(account.getLoans().get(currency));
			final Object[] row = new Object[] { UUID.randomUUID(), version,
				currency.toUpperCase(), now, balance, frozen, loan, };
			rows.add(row);
		});

		getJdbcTemplate().batchUpdate(INSERT_SQL, rows);
	}

	private BigDecimal getAmount(BTCChinaValue v) {
		if (v == null) {
			return BigDecimal.ZERO;
		}

		return v.getAmount();
	}

}
