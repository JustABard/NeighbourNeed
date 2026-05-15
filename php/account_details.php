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
    $stmt = $conn->prepare(
        "SELECT user_id, full_name, email, user_type, default_location, created_at
         FROM users
         WHERE user_id = :user_id"
    );

    $stmt->execute([
        ":user_id" => $user_id
    ]);

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        echo json_encode([
            "success" => false,
            "message" => "Account not found"
        ]);
        exit;
    }

    echo json_encode([
        "success" => true,
        "user" => $user
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load account"
    ]);
}
?>
