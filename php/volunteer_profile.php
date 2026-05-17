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
        "SELECT u.user_id, u.full_name, s.vehicle_type, s.profile_image_base64,
                COALESCE(r.average_rating, 0) AS average_rating,
                COALESCE(r.rating_count, 0) AS rating_count
         FROM shoppers s
         JOIN users u ON u.user_id = s.user_id
         LEFT JOIN (
             SELECT shopper_user_id, ROUND(AVG(rating)::numeric, 1) AS average_rating, COUNT(*) AS rating_count
             FROM shopper_ratings
             GROUP BY shopper_user_id
         ) r ON r.shopper_user_id = s.user_id
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
        "SELECT COALESCE(sr.thanks_message, vt.message) AS message,
                COALESCE(sr.rating, 0) AS rating,
                COALESCE(sr.created_at, vt.created_at) AS created_at,
                customer.full_name AS customer_name
         FROM volunteer_thanks vt
         FULL OUTER JOIN shopper_ratings sr ON sr.order_id = vt.order_id
         JOIN users customer ON customer.user_id = COALESCE(sr.customer_user_id, vt.customer_user_id)
         WHERE COALESCE(sr.shopper_user_id, vt.shopper_user_id) = :shopper_user_id
           AND COALESCE(sr.thanks_message, vt.message, '') <> ''
         ORDER BY COALESCE(sr.created_at, vt.created_at) DESC"
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
