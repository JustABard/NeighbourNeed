<?php
header("Content-Type: application/json");
require "db.php";

$order_id = $_POST["order_id"] ?? "";
$customer_user_id = $_POST["customer_user_id"] ?? "";
$shopper_user_id = $_POST["shopper_user_id"] ?? "";
$message = trim($_POST["message"] ?? "");
$rating = trim($_POST["rating"] ?? "");

if ($order_id == "" || $customer_user_id == "" || $shopper_user_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing order or shopper"
    ]);
    exit;
}

if ($message == "" && $rating == "") {
    echo json_encode([
        "success" => false,
        "message" => "Enter a message or rating"
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

    if ($rating != "") {
        $rating_value = intval($rating);
        if ($rating_value < 1 || $rating_value > 5) {
            echo json_encode([
                "success" => false,
                "message" => "Rating must be between 1 and 5"
            ]);
            exit;
        }

        $rating_stmt = $conn->prepare(
            "INSERT INTO shopper_ratings (order_id, customer_user_id, shopper_user_id, rating, thanks_message)
             VALUES (:order_id, :customer_user_id, :shopper_user_id, :rating, :thanks_message)
             ON CONFLICT (order_id, customer_user_id)
             DO UPDATE SET rating = EXCLUDED.rating,
                           thanks_message = EXCLUDED.thanks_message,
                           created_at = CURRENT_TIMESTAMP"
        );
        $rating_stmt->execute([
            ":order_id" => $order_id,
            ":customer_user_id" => $customer_user_id,
            ":shopper_user_id" => $shopper_user_id,
            ":rating" => $rating_value,
            ":thanks_message" => $message
        ]);
    } else {
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
    }

    echo json_encode([
        "success" => true,
        "message" => "Profile feedback posted"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not post thank-you"
    ]);
}
?>
