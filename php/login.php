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
        "SELECT u.user_id, u.full_name, u.password_hash, u.user_type,
                COALESCE(u.suspended, FALSE) AS suspended,
                COALESCE(a.verified, TRUE) AS admin_verified
         FROM users u
         LEFT JOIN admins a ON a.user_id = u.user_id
         WHERE u.email = :email"
    );

    $stmt->execute([
        ":email" => $email
    ]);

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($user && password_verify($password, $user["password_hash"])) {
        if ($user["suspended"] === true || $user["suspended"] === "t" || $user["suspended"] === "1") {
            echo json_encode([
                "success" => false,
                "message" => "This account has been suspended"
            ]);
            exit;
        }

        if ($user["user_type"] == "admin"
                && ($user["admin_verified"] === false || $user["admin_verified"] === "f" || $user["admin_verified"] === "0")) {
            echo json_encode([
                "success" => false,
                "message" => "This admin account is not verified"
            ]);
            exit;
        }

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
