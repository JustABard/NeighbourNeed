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
         SET status = 'cancelled',
             cancelled_at = CURRENT_TIMESTAMP
         WHERE order_id = :order_id
           AND customer_user_id = :user_id
           AND status IN ('pending', 'taken', 'shopping')"
    );
    $stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);

    if ($stmt->rowCount() == 0) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "This order cannot be cancelled"
        ]);
        exit;
    }

    $history_stmt = $conn->prepare(
        "INSERT INTO order_status_history (order_id, status)
         VALUES (:order_id, 'cancelled')"
    );
    $history_stmt->execute([
        ":order_id" => $order_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Order cancelled"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not cancel order"
    ]);
}
?>
