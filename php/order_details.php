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
                o.delivery_longitude, o.shopper_latitude, o.shopper_longitude,
                o.shopper_location_updated_at, o.notes, o.status, o.created_at, o.completed_at,
                customer.full_name AS customer_name,
                shopper.full_name AS shopper_name,
                COALESCE(r.average_rating, 0) AS shopper_average_rating,
                COALESCE(r.rating_count, 0) AS shopper_rating_count
         FROM orders o
         JOIN users customer ON customer.user_id = o.customer_user_id
         LEFT JOIN users shopper ON shopper.user_id = o.shopper_user_id
         LEFT JOIN (
             SELECT shopper_user_id, ROUND(AVG(rating)::numeric, 1) AS average_rating, COUNT(*) AS rating_count
             FROM shopper_ratings
             GROUP BY shopper_user_id
         ) r ON r.shopper_user_id = o.shopper_user_id
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
