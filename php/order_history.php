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
        "SELECT order_id, order_description, pickup_address, delivery_address, notes, status, created_at, completed_at
         FROM orders
         WHERE customer_user_id = :user_id
           AND status IN ('completed', 'cancelled')
         ORDER BY created_at DESC"
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
