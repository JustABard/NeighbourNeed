<?php
header("Content-Type: application/json");
require "db.php";

$order_id = $_POST["order_id"] ?? "";

if ($order_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing order"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "SELECT o.order_id, o.customer_user_id, o.shopper_user_id, o.order_description,
                o.pickup_address, o.delivery_address, o.delivery_latitude,
                o.delivery_longitude, o.notes, o.status, o.created_at, o.completed_at,
                customer.full_name AS customer_name,
                shopper.full_name AS shopper_name
         FROM orders o
         JOIN users customer ON customer.user_id = o.customer_user_id
         LEFT JOIN users shopper ON shopper.user_id = o.shopper_user_id
         WHERE o.order_id = :order_id"
    );

    $stmt->execute([
        ":order_id" => $order_id
    ]);

    $order = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$order) {
        echo json_encode([
            "success" => false,
            "message" => "Order not found"
        ]);
        exit;
    }

    echo json_encode([
        "success" => true,
        "order" => $order
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load order details"
    ]);
}
?>
