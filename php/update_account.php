<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$full_name = trim($_POST["full_name"] ?? "");
$default_location = trim($_POST["default_location"] ?? "");

if ($user_id == "" || $full_name == "") {
    echo json_encode([
        "success" => false,
        "message" => "Please enter your full name"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "UPDATE users
         SET full_name = :full_name,
             default_location = :default_location
         WHERE user_id = :user_id"
    );

    $stmt->execute([
        ":full_name" => $full_name,
        ":default_location" => $default_location,
        ":user_id" => $user_id
    ]);

    echo json_encode([
        "success" => true,
        "message" => "Account updated"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not update account"
    ]);
}
?>
