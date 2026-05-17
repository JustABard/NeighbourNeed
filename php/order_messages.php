<?php
header("Content-Type: application/json");
require "db.php";

$order_id = $_POST["order_id"] ?? "";
$user_id = $_POST["user_id"] ?? "";

if ($order_id == "" || $user_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing order or user"
    ]);
    exit;
}

try {
    $access_stmt = $conn->prepare(
        "SELECT order_id
         FROM orders
         WHERE order_id = :order_id
           AND (customer_user_id = :user_id OR shopper_user_id = :user_id)"
    );
    $access_stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);

    if (!$access_stmt->fetchColumn()) {
        echo json_encode([
            "success" => false,
            "message" => "You cannot view messages for this order"
        ]);
        exit;
    }

    $stmt = $conn->prepare(
        "SELECT m.message_id, m.message, m.created_at, u.full_name AS sender_name, u.user_type AS sender_type
         FROM order_messages m
         JOIN users u ON u.user_id = m.sender_user_id
         WHERE m.order_id = :order_id
         ORDER BY m.created_at ASC"
    );
    $stmt->execute([
        ":order_id" => $order_id
    ]);

    echo json_encode([
        "success" => true,
        "messages" => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load messages"
    ]);
}
?>
