-- Run this on the Wits PostgreSQL database after uploading the PHP files.
-- It is safe to run more than once because every change uses IF NOT EXISTS.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS default_location TEXT;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS default_latitude DOUBLE PRECISION;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS default_longitude DOUBLE PRECISION;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS suspended BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shoppers
ADD COLUMN IF NOT EXISTS approved BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shoppers
ADD COLUMN IF NOT EXISTS profile_image_base64 TEXT;

ALTER TABLE admins
ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS orders (
    order_id SERIAL PRIMARY KEY,
    customer_user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    shopper_user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    order_description TEXT,
    pickup_address TEXT,
    delivery_address TEXT,
    delivery_latitude DOUBLE PRECISION,
    delivery_longitude DOUBLE PRECISION,
    notes TEXT,
    status VARCHAR(30) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS customer_user_id INT REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS shopper_user_id INT REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS order_description TEXT;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pickup_address TEXT;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS delivery_address TEXT;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS delivery_latitude DOUBLE PRECISION;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS delivery_longitude DOUBLE PRECISION;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'pending';

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS shopper_latitude DOUBLE PRECISION;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS shopper_longitude DOUBLE PRECISION;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS shopper_location_updated_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS order_status_history (
    status_history_id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(order_id) ON DELETE CASCADE,
    status VARCHAR(30),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE order_status_history
ADD COLUMN IF NOT EXISTS order_id INT REFERENCES orders(order_id) ON DELETE CASCADE;

ALTER TABLE order_status_history
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

ALTER TABLE order_status_history
ADD COLUMN IF NOT EXISTS changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS volunteer_thanks (
    thanks_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    customer_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    shopper_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_messages (
    message_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    sender_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shopper_ratings (
    rating_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    customer_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    shopper_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    thanks_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id, customer_user_id)
);

CREATE TABLE IF NOT EXISTS support_tickets (
    ticket_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    subject VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE support_tickets
ADD COLUMN IF NOT EXISTS user_id INT REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE support_tickets
ADD COLUMN IF NOT EXISTS subject VARCHAR(120);

ALTER TABLE support_tickets
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'open';

ALTER TABLE support_tickets
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE support_tickets
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS support_ticket_messages (
    support_message_id SERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES support_tickets(ticket_id) ON DELETE CASCADE,
    sender_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE support_ticket_messages
ADD COLUMN IF NOT EXISTS ticket_id INT REFERENCES support_tickets(ticket_id) ON DELETE CASCADE;

ALTER TABLE support_ticket_messages
ADD COLUMN IF NOT EXISTS sender_user_id INT REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE support_ticket_messages
ADD COLUMN IF NOT EXISTS message TEXT;

ALTER TABLE support_ticket_messages
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
