<?php
header("Content-Type: application/json");
require "db.php";

try {
    $stmt = $conn->prepare(
        "SELECT u.user_id, u.full_name, u.email, s.id_number, s.vehicle_type
         FROM shoppers s
         JOIN users u ON u.user_id = s.user_id
         WHERE s.approved = FALSE
         ORDER BY u.created_at ASC"
    );

    $stmt->execute();

    echo json_encode([
        "success" => true,
        "shoppers" => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load pending shoppers"
    ]);
}
?>
