<?php
header("Content-Type: application/json");
require "db.php";

$shopper_user_id = $_POST["shopper_user_id"] ?? "";

if ($shopper_user_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing volunteer"
    ]);
    exit;
}

try {
    $profile_stmt = $conn->prepare(
        "SELECT u.user_id, u.full_name, s.vehicle_type
         FROM shoppers s
         JOIN users u ON u.user_id = s.user_id
         WHERE u.user_id = :shopper_user_id"
    );
    $profile_stmt->execute([
        ":shopper_user_id" => $shopper_user_id
    ]);
    $profile = $profile_stmt->fetch(PDO::FETCH_ASSOC);

    if (!$profile) {
        echo json_encode([
            "success" => false,
            "message" => "Volunteer not found"
        ]);
        exit;
    }

    $thanks_stmt = $conn->prepare(
        "SELECT t.message, t.created_at, customer.full_name AS customer_name
         FROM volunteer_thanks t
         JOIN users customer ON customer.user_id = t.customer_user_id
         WHERE t.shopper_user_id = :shopper_user_id
         ORDER BY t.created_at DESC"
    );
    $thanks_stmt->execute([
        ":shopper_user_id" => $shopper_user_id
    ]);

    echo json_encode([
        "success" => true,
        "profile" => $profile,
        "thanks" => $thanks_stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load volunteer profile"
    ]);
}
?>
