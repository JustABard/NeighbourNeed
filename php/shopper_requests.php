<?php
header("Content-Type: application/json");
require "db.php";

try {
    $stmt = $conn->prepare(
        "SELECT o.order_id, o.order_description, o.pickup_address, o.delivery_address,
                o.delivery_latitude, o.delivery_longitude, o.notes, o.status, o.created_at,
                customer.full_name AS customer_name
         FROM orders o
         JOIN users customer ON customer.user_id = o.customer_user_id
         WHERE o.status = 'pending'
           AND o.shopper_user_id IS NULL
         ORDER BY o.created_at ASC"
    );

    $stmt->execute();

    echo json_encode([
        "success" => true,
        "orders" => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load open requests"
    ]);
}
?>
