# NeighbourNeed Implementation Reference

## Assignment Target

Topic: Surrogate Shopper.

Core requirements:

- Customers request flexible free-text shopping items.
- Customers store their delivery location on their profile using phone location services.
- Volunteers/shoppers view all open requests and open request details.
- Shoppers mark one request as taken so only one shopper collects it.
- Shoppers view the requestor location for map navigation.
- Customers post thank-you messages on volunteer profiles.
- Shoppers must be approved by an admin before collecting orders.

Rubric priorities:

- Working login and registration with validation.
- Web-service data sent and received through PHP/PostgreSQL.
- Nicely formatted downloaded lists.
- Database-driven list items open detail views.
- Data saved in the app reaches the database, and database data returns to the app.
- Navigation works and the GUI is consistent.

## Current Architecture

- Android Java app uses OkHTTP.
- PHP files live in `php/` locally and must be uploaded to the Wits `PHP/` folder.
- Base API URL: `https://wmc.ms.wits.ac.za/students/sgroup2677/PHP/`.
- Android session stores logged-in `user_id`, `full_name`, and now `user_type`.
- Customers use `MainActivity` as the dashboard.
- Shoppers and admins receive role-specific dashboards after login.

## Database Expectations

The concrete migration SQL for the extra columns/table added in this pass is in `php/database_migration.sql`.

Existing core tables:

```sql
users(
  user_id,
  full_name,
  email,
  password_hash,
  user_type,
  default_location,
  default_latitude,
  default_longitude,
  created_at
)

customers(customer_id, user_id)

shoppers(
  shopper_id,
  user_id,
  id_number,
  vehicle_type,
  approved
)

admins(admin_id, user_id, employee_id, admin_role)

orders(
  order_id,
  customer_user_id,
  shopper_user_id,
  order_description,
  pickup_address,
  delivery_address,
  delivery_latitude,
  delivery_longitude,
  notes,
  status,
  created_at,
  completed_at
)

order_status_history(status_history_id, order_id, status, changed_at)
```

New table for thank-you messages:

```sql
volunteer_thanks(
  thanks_id SERIAL PRIMARY KEY,
  order_id INT REFERENCES orders(order_id) ON DELETE CASCADE,
  customer_user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
  shopper_user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
  message TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

Recommended migration SQL:

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS default_location TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS default_latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS default_longitude DOUBLE PRECISION;

ALTER TABLE shoppers ADD COLUMN IF NOT EXISTS approved BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_latitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_longitude DOUBLE PRECISION;

CREATE TABLE IF NOT EXISTS volunteer_thanks (
  thanks_id SERIAL PRIMARY KEY,
  order_id INT REFERENCES orders(order_id) ON DELETE CASCADE,
  customer_user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
  shopper_user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
  message TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## PHP Endpoints

Authentication:

- `login.php` returns `user_id`, `full_name`, and `user_type`.
- `register.php` creates users and shopper/admin/customer subtype rows.

Customer:

- `create_order.php`
- `current_order.php`
- `order_history.php`
- `account_details.php`
- `update_account.php`
- `delete_account.php`
- `post_thanks.php`

Shopper:

- `shopper_requests.php`
- `order_details.php`
- `take_order.php`
- `shopper_current_order.php`
- `complete_order.php`
- `volunteer_profile.php`

Admin:

- `pending_shoppers.php`
- `approve_shopper.php`

## UI Consistency Rules

- Main card containers should use `android:layout_width="0dp"` and `android:layout_height="0dp"` with 10dp constraints/margins where practical.
- Avoid `wrap_content` cards for main screens.
- Use existing background/image style and `sora_extrabold` for headings/buttons.
- Role colors:
  - Customer: `#84C8FF`
  - Shopper: `#71D7C7`
  - Admin: `#F6C86E`
- Bold text preference should make text slightly larger and bold across operational screens.

## Implementation Notes

- Customer item list is serialized into `orders.order_description` as formatted free text to preserve the assignment's flexible item-entry requirement.
- Preferred store is stored in `orders.pickup_address`.
- Customer delivery profile uses Android `LocationManager` and stores both display text and latitude/longitude.
- Map navigation uses `geo:` intents with coordinates where available, otherwise delivery address text.
- Shopper order taking is guarded server-side by `shoppers.approved = TRUE` and a pending order update condition.
