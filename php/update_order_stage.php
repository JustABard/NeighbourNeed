<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$order_id = $_POST["order_id"] ?? "";
$stage = $_POST["stage"] ?? "";
$shopper_latitude = trim($_POST["shopper_latitude"] ?? "");
$shopper_longitude = trim($_POST["shopper_longitude"] ?? "");

$allowed = ["shopping", "delivering", "completed"];

if ($user_id == "" || $order_id == "" || !in_array($stage, $allowed)) {
    echo json_encode([
        "success" => false,
        "message" => "Missing stage details"
    ]);
    exit;
}

try {
    $conn->beginTransaction();

    $current_stmt = $conn->prepare(
        "SELECT status
         FROM orders
         WHERE order_id = :order_id
           AND shopper_user_id = :user_id"
    );
    $current_stmt->execute([
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);
    $current_status = $current_stmt->fetchColumn();

    if (!$current_status) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "Only the assigned shopper can update this order"
        ]);
        exit;
    }

    $valid_transition =
        ($current_status == "taken" && $stage == "shopping") ||
        ($current_status == "shopping" && $stage == "delivering") ||
        ($current_status == "delivering" && $stage == "completed");

    if (!$valid_transition) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "Invalid stage change"
        ]);
        exit;
    }

    $completed_sql = $stage == "completed" ? ", completed_at = CURRENT_TIMESTAMP" : "";
    $location_sql = $stage == "delivering"
        ? ", shopper_latitude = NULLIF(:shopper_latitude, '')::double precision,
             shopper_longitude = NULLIF(:shopper_longitude, '')::double precision,
             shopper_location_updated_at = CASE
                 WHEN :shopper_latitude <> '' AND :shopper_longitude <> '' THEN CURRENT_TIMESTAMP
                 ELSE shopper_location_updated_at
             END"
        : "";

    $stmt = $conn->prepare(
        "UPDATE orders
         SET status = :stage
             $completed_sql
             $location_sql
         WHERE order_id = :order_id
           AND shopper_user_id = :user_id"
    );

    $params = [
        ":stage" => $stage,
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ];

    if ($stage == "delivering") {
        $params[":shopper_latitude"] = $shopper_latitude;
        $params[":shopper_longitude"] = $shopper_longitude;
    }

    $stmt->execute($params);

    $history_stmt = $conn->prepare(
        "INSERT INTO order_status_history (order_id, status)
         VALUES (:order_id, :stage)"
    );
    $history_stmt->execute([
        ":order_id" => $order_id,
        ":stage" => $stage
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Order stage updated"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not update order stage"
    ]);
}
?>
