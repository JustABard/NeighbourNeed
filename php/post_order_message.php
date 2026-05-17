<?php
header("Content-Type: application/json");
require "db.php";

$order_id = $_POST["order_id"] ?? "";
$user_id = $_POST["user_id"] ?? "";
$message = trim($_POST["message"] ?? "");

if ($order_id == "" || $user_id == "" || $message == "") {
    echo json_encode([
        "success" => false,
        "message" => "Enter a message"
    ]);
    exit;
}

try {
    $access_stmt = $conn->prepare(
        "SELECT order_id
         FROM orders
         WHERE order_id = :order_id
           AND (customer_user_id = :user_id OR shopper_user_id = :user_id)
           AND status <> 'cancelled'"
    );
    $access_stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);

    if (!$access_stmt->fetchColumn()) {
        echo json_encode([
            "success" => false,
            "message" => "You cannot message on this order"
        ]);
        exit;
    }

    $stmt = $conn->prepare(
        "INSERT INTO order_messages (order_id, sender_user_id, message)
         VALUES (:order_id, :user_id, :message)"
    );
    $stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id,
        ":message" => $message
    ]);

    echo json_encode([
        "success" => true,
        "message" => "Message posted"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not post message"
    ]);
}
?>
