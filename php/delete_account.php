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
    $conn->beginTransaction();

    $history_stmt = $conn->prepare(
        "DELETE FROM order_status_history
         WHERE order_id IN (
             SELECT order_id
             FROM orders
             WHERE customer_user_id = :user_id
                OR shopper_user_id = :user_id
         )"
    );
    $history_stmt->execute([
        ":user_id" => $user_id
    ]);

    $orders_stmt = $conn->prepare(
        "DELETE FROM orders
         WHERE customer_user_id = :user_id
            OR shopper_user_id = :user_id"
    );
    $orders_stmt->execute([
        ":user_id" => $user_id
    ]);

    $stmt = $conn->prepare(
        "DELETE FROM users
         WHERE user_id = :user_id"
    );

    $stmt->execute([
        ":user_id" => $user_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Account deleted"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not delete account"
    ]);
}
?>
