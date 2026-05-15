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
    $approval_stmt = $conn->prepare(
        "SELECT approved
         FROM shoppers
         WHERE user_id = :user_id"
    );
    $approval_stmt->execute([
        ":user_id" => $user_id
    ]);
    $approved = $approval_stmt->fetchColumn();

    if ($approved !== true && $approved !== "t" && $approved !== "1" && $approved !== 1) {
        echo json_encode([
            "success" => false,
            "message" => "Your shopper account must be approved first"
        ]);
        exit;
    }

    $conn->beginTransaction();

    $stmt = $conn->prepare(
        "UPDATE orders
         SET shopper_user_id = :user_id,
             status = 'taken'
         WHERE order_id = :order_id
           AND shopper_user_id IS NULL
           AND status = 'pending'"
    );
    $stmt->execute([
        ":user_id" => $user_id,
        ":order_id" => $order_id
    ]);

    if ($stmt->rowCount() == 0) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "This request has already been taken"
        ]);
        exit;
    }

    $history_stmt = $conn->prepare(
        "INSERT INTO order_status_history (order_id, status)
         VALUES (:order_id, 'taken')"
    );
    $history_stmt->execute([
        ":order_id" => $order_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Request taken"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not take request"
    ]);
}
?>
