CREATE TABLE trade (
	tid bigint PRIMARY KEY,
	date timestamp with time zone NOT NULL,
	-- buy/sell
	type varchar(4) NOT NULL,
	amount numeric(16,8) NOT NULL,
	price numeric(32,8) NOT NULL
);
CREATE INDEX trade_date ON trade(date);
CREATE INDEX trade_type ON trade(type);

CREATE TABLE transaction (
	id bigint PRIMARY KEY,
	date timestamp with time zone NOT NULL,
	-- Type of transaction.
	-- 'fundbtc | withdrawbtc | fundmoney | withdrawmoney | refundmoney
	-- | buybtc | sellbtc | refundbtc | tradefee | rebate | fundltc
	-- | refundltc | withdrawltc | buyltc | sellltc'
	type varchar(16) NOT NULL,
	amount numeric(16,8) NOT NULL,
	money numeric(32,8) NOT NULL
);
CREATE INDEX transaction_date ON transaction(date);
CREATE INDEX transaction_type ON transaction(type);

CREATE TABLE "order" (
	id bigint PRIMARY KEY,
	date timestamp with time zone NOT NULL,
	-- bid/ask/buyfiat
	type varchar(7) NOT NULL,
	price numeric(32, 8) NOT NULL,
	currency char(3) NOT NULL,
	amount numeric(16, 8) NOT NULL,
	amount_original numeric(16, 8) NOT NULL,
	status varchar(20) NOT NULL
);
CREATE INDEX order_date ON "order"(date);
CREATE INDEX order_type ON "order"(type);
CREATE INDEX order_status ON "order"(status);

CREATE TABLE order_detail (
	id uuid PRIMARY KEY,
	order_id bigint NOT NULL,
	date timestamp with time zone NOT NULL,
	amount numeric(16, 8) NOT NULL,
	price numeric(32, 8) NOT NULL
);
CREATE INDEX order_detail_order_id ON order_detail(order_id);

CREATE TABLE account(
	id uuid PRIMARY KEY,
	version bigint NOT NULL,
	currency char(3) NOT NULL,
	date timestamp with time zone NOT NULL,
	balance numeric(32, 8) NOT NULL,
	frozen numeric(32, 8) NOT NULL,
	loan numeric(32, 8) NOT NULL
);
CREATE UNIQUE INDEX account_version_currency ON account(version, currency);
