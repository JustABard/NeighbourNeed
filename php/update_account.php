<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$full_name = trim($_POST["full_name"] ?? "");
$default_location = trim($_POST["default_location"] ?? "");
$default_latitude = trim($_POST["default_latitude"] ?? "");
$default_longitude = trim($_POST["default_longitude"] ?? "");

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
             default_location = :default_location,
             default_latitude = NULLIF(:default_latitude, '')::double precision,
             default_longitude = NULLIF(:default_longitude, '')::double precision
         WHERE user_id = :user_id"
    );

    $stmt->execute([
        ":full_name" => $full_name,
        ":default_location" => $default_location,
        ":default_latitude" => $default_latitude,
        ":default_longitude" => $default_longitude,
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
