<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";

if ($user_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing user"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "SELECT o.order_id, o.order_description, o.pickup_address, o.delivery_address,
                o.delivery_latitude, o.delivery_longitude, o.notes, o.status,
                o.created_at, o.completed_at, o.shopper_user_id,
                shopper.full_name AS shopper_name,
                COALESCE(r.average_rating, 0) AS shopper_average_rating,
                COALESCE(r.rating_count, 0) AS shopper_rating_count
         FROM orders o
         LEFT JOIN users shopper ON shopper.user_id = o.shopper_user_id
         LEFT JOIN (
             SELECT shopper_user_id, ROUND(AVG(rating)::numeric, 1) AS average_rating, COUNT(*) AS rating_count
             FROM shopper_ratings
             GROUP BY shopper_user_id
         ) r ON r.shopper_user_id = o.shopper_user_id
         WHERE o.customer_user_id = :user_id
           AND o.status IN ('completed', 'cancelled')
         ORDER BY o.created_at DESC"
    );

    $stmt->execute([
        ":user_id" => $user_id
    ]);

    echo json_encode([
        "success" => true,
        "orders" => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load order history"
    ]);
}
?>
