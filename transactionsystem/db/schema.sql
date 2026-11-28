CREATE TABLE IF NOT EXISTS customer (
    customer_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS account (
    account_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(customer_id),
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'REJECTED')),
    source_account_id BIGINT REFERENCES account(account_id),
    destination_account_id BIGINT REFERENCES account(account_id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transaction_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    thread_name VARCHAR(100),
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_customer_id ON account(customer_id);
CREATE INDEX IF NOT EXISTS idx_transaction_source_account_id ON transaction(source_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_destination_account_id ON transaction(destination_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_audit_transaction_id ON transaction_audit(transaction_id);
