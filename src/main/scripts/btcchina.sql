create table trade (
	tid bigint primary key,
	date timestamp with time zone not null,
	-- buy/sell
	type varchar(4) not null,
	amount numeric(16,8) not null,
	price numeric(32,8) not null
);
create index trade_date on trade(date);
create index trade_type on trade(type);

create table transaction (
	id bigint primary key,
	date timestamp with time zone not null,
	-- Type of transaction.
	-- 'fundbtc | withdrawbtc | fundmoney | withdrawmoney | refundmoney
	-- | buybtc | sellbtc | refundbtc | tradefee | rebate | fundltc
	-- | refundltc | withdrawltc | buyltc | sellltc'
	type varchar(16) not null,
	amount numeric(16,8) not null,
	money numeric(32,8) not null
);
create index transaction_date on transaction(date);
create index transaction_type on transaction(type);

create table "order" (
	id bigint primary key,
	date timestamp with time zone not null,
	-- bid/ask/buyfiat
	type varchar(7) not null,
	price numeric(32, 8) not null,
	currency char(3) not null,
	amount numeric(16, 8) not null,
	amount_original numeric(16, 8) not null,
	status varchar(20) not null
);
create index order_date on "order"(date);
create index order_type on "order"(type);
create index order_status on "order"(status);

create table order_detail (
	id uuid primary key,
	order_id bigint not null,
	date timestamp with time zone not null,
	amount numeric(16, 8) not null,
	price numeric(32, 8) not null
);
create index order_detail_order_id on order_detail(order_id);
