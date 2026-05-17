<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$profile_image_base64 = $_POST["profile_image_base64"] ?? "";

if ($user_id == "" || $profile_image_base64 == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing profile image"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "UPDATE shoppers
         SET profile_image_base64 = :profile_image_base64
         WHERE user_id = :user_id"
    );
    $stmt->execute([
        ":profile_image_base64" => $profile_image_base64,
        ":user_id" => $user_id
    ]);

    echo json_encode([
        "success" => true,
        "message" => "Profile photo updated"
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not update profile photo"
    ]);
}
?>
