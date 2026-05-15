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
         SET status = 'completed',
             completed_at = CURRENT_TIMESTAMP
         WHERE order_id = :order_id
           AND shopper_user_id = :user_id
           AND status = 'taken'"
    );
    $stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);

    if ($stmt->rowCount() == 0) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "Only the assigned shopper can complete this request"
        ]);
        exit;
    }

    $history_stmt = $conn->prepare(
        "INSERT INTO order_status_history (order_id, status)
         VALUES (:order_id, 'completed')"
    );
    $history_stmt->execute([
        ":order_id" => $order_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Order marked completed"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not complete order"
    ]);
}
?>
