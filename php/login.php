<?php
header("Content-Type: application/json");
require "db.php";

$email = trim($_POST["email"] ?? "");
$password = $_POST["password"] ?? "";

if ($email == "" || $password == "") {
    echo json_encode([
        "success" => false,
        "message" => "Please enter email and password"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "SELECT user_id, full_name, password_hash, user_type
         FROM users
         WHERE email = :email"
    );

    $stmt->execute([
        ":email" => $email
    ]);

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($user && password_verify($password, $user["password_hash"])) {
        echo json_encode([
            "success" => true,
            "message" => "Login successful",
            "user_id" => $user["user_id"],
            "full_name" => $user["full_name"],
            "user_type" => $user["user_type"]
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Invalid email or password"
        ]);
    }
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Login failed"
    ]);
}
?>
