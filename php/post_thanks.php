<?php
header("Content-Type: application/json");
require "db.php";

$order_id = $_POST["order_id"] ?? "";
$customer_user_id = $_POST["customer_user_id"] ?? "";
$shopper_user_id = $_POST["shopper_user_id"] ?? "";
$message = trim($_POST["message"] ?? "");

if ($order_id == "" || $customer_user_id == "" || $shopper_user_id == "" || $message == "") {
    echo json_encode([
        "success" => false,
        "message" => "Please enter a thank-you message"
    ]);
    exit;
}

try {
    $order_stmt = $conn->prepare(
        "SELECT order_id
         FROM orders
         WHERE order_id = :order_id
           AND customer_user_id = :customer_user_id
           AND shopper_user_id = :shopper_user_id
           AND status = 'completed'"
    );
    $order_stmt->execute([
        ":order_id" => $order_id,
        ":customer_user_id" => $customer_user_id,
        ":shopper_user_id" => $shopper_user_id
    ]);

    if (!$order_stmt->fetchColumn()) {
        echo json_encode([
            "success" => false,
            "message" => "You can thank a volunteer after a completed order"
        ]);
        exit;
    }

    $stmt = $conn->prepare(
        "INSERT INTO volunteer_thanks (order_id, customer_user_id, shopper_user_id, message)
         VALUES (:order_id, :customer_user_id, :shopper_user_id, :message)"
    );
    $stmt->execute([
        ":order_id" => $order_id,
        ":customer_user_id" => $customer_user_id,
        ":shopper_user_id" => $shopper_user_id,
        ":message" => $message
    ]);

    echo json_encode([
        "success" => true,
        "message" => "Thank-you posted"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not post thank-you"
    ]);
}
?>
