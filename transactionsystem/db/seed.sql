INSERT INTO customer (name, email, role) VALUES
    ('Alice Mercer', 'alice.mercer@example.com', 'ADMIN'),
    ('Bob Carter', 'bob.carter@example.com', 'CUSTOMER'),
    ('Carol Diaz', 'carol.diaz@example.com', 'CUSTOMER'),
    ('David Patel', 'david.patel@example.com', 'CUSTOMER');

INSERT INTO account (customer_id, balance, status) VALUES
    ((SELECT customer_id FROM customer WHERE email = 'bob.carter@example.com'), 10000.00, 'ACTIVE'),
    ((SELECT customer_id FROM customer WHERE email = 'carol.diaz@example.com'), 5000.00, 'ACTIVE'),
    ((SELECT customer_id FROM customer WHERE email = 'david.patel@example.com'), 2000.00, 'ACTIVE');
