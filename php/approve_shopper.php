<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";

if ($user_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing shopper"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "UPDATE shoppers
         SET approved = TRUE
         WHERE user_id = :user_id"
    );

    $stmt->execute([
        ":user_id" => $user_id
    ]);

    echo json_encode([
        "success" => true,
        "message" => "Shopper approved"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not approve shopper"
    ]);
}
?>
