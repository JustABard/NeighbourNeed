<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$order_id = $_POST["order_id"] ?? "";
$shopper_latitude = trim($_POST["shopper_latitude"] ?? "");
$shopper_longitude = trim($_POST["shopper_longitude"] ?? "");

if ($user_id == "" || $order_id == "" || $shopper_latitude == "" || $shopper_longitude == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing location snapshot"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "UPDATE orders
         SET shopper_latitude = :shopper_latitude,
             shopper_longitude = :shopper_longitude,
             shopper_location_updated_at = CURRENT_TIMESTAMP
         WHERE order_id = :order_id
           AND shopper_user_id = :user_id
           AND status = 'delivering'"
    );
    $stmt->execute([
        ":shopper_latitude" => $shopper_latitude,
        ":shopper_longitude" => $shopper_longitude,
        ":order_id" => $order_id,
        ":user_id" => $user_id
    ]);

    if ($stmt->rowCount() == 0) {
        echo json_encode([
            "success" => false,
            "message" => "Location can only be updated by the assigned shopper while delivering"
        ]);
        exit;
    }

    echo json_encode([
        "success" => true,
        "message" => "Location snapshot updated"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not update location snapshot"
    ]);
}
?>
