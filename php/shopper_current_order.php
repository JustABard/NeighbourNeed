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
                o.created_at, o.completed_at, o.customer_user_id,
                customer.full_name AS customer_name
         FROM orders o
         JOIN users customer ON customer.user_id = o.customer_user_id
         WHERE o.shopper_user_id = :user_id
           AND o.status NOT IN ('completed', 'cancelled')
         ORDER BY o.created_at DESC
         LIMIT 1"
    );

    $stmt->execute([
        ":user_id" => $user_id
    ]);

    $order = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$order) {
        echo json_encode([
            "success" => false,
            "message" => "You have not taken an active request"
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
        "message" => "Could not load taken order"
    ]);
}
?>
