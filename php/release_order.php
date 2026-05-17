<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$order_id = $_POST["order_id"] ?? "";

if ($user_id == "" || $order_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing user or order"
    ]);
    exit;
}

try {
    $conn->beginTransaction();

    $stmt = $conn->prepare(
        "UPDATE orders
         SET shopper_user_id = NULL,
             status = 'pending',
             shopper_latitude = NULL,
             shopper_longitude = NULL
         WHERE order_id = :order_id
           AND shopper_user_id = :user_id
           AND status IN ('taken', 'shopping')"
    );
    $stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);

    if ($stmt->rowCount() == 0) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "This request cannot be released"
        ]);
        exit;
    }

    $history_stmt = $conn->prepare(
        "INSERT INTO order_status_history (order_id, status)
         VALUES (:order_id, 'released')"
    );
    $history_stmt->execute([
        ":order_id" => $order_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Request released"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not release request"
    ]);
}
?>
