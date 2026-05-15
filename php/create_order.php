<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$order_description = trim($_POST["order_description"] ?? "");
$pickup_address = trim($_POST["pickup_address"] ?? "");
$delivery_address = trim($_POST["delivery_address"] ?? "");
$notes = trim($_POST["notes"] ?? "");

if ($user_id == "" || $order_description == "" || $delivery_address == "") {
    echo json_encode([
        "success" => false,
        "message" => "Please fill in the required order fields"
    ]);
    exit;
}

try {
    $conn->beginTransaction();

    $stmt = $conn->prepare(
        "INSERT INTO orders (customer_user_id, order_description, pickup_address, delivery_address, notes, status)
         VALUES (:customer_user_id, :order_description, :pickup_address, :delivery_address, :notes, 'pending')
         RETURNING order_id"
    );

    $stmt->execute([
        ":customer_user_id" => $user_id,
        ":order_description" => $order_description,
        ":pickup_address" => $pickup_address,
        ":delivery_address" => $delivery_address,
        ":notes" => $notes
    ]);

    $order_id = $stmt->fetchColumn();

    $history_stmt = $conn->prepare(
        "INSERT INTO order_status_history (order_id, status)
         VALUES (:order_id, 'pending')"
    );
    $history_stmt->execute([
        ":order_id" => $order_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Order created",
        "order_id" => $order_id
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not create order"
    ]);
}
?>
