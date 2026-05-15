<?php
header("Content-Type: application/json");
require "db.php";

$user_type = $_POST["user_type"] ?? "";
$full_name = trim($_POST["full_name"] ?? "");
$email = trim($_POST["email"] ?? "");
$password = $_POST["password"] ?? "";

if ($user_type == "" || $full_name == "" || $email == "" || $password == "") {
    echo json_encode([
        "success" => false,
        "message" => "Please fill in all required fields"
    ]);
    exit;
}

if (!in_array($user_type, ["customer", "shopper", "admin"])) {
    echo json_encode([
        "success" => false,
        "message" => "Invalid user type"
    ]);
    exit;
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        "success" => false,
        "message" => "Invalid email address"
    ]);
    exit;
}

if (strlen($password) < 6) {
    echo json_encode([
        "success" => false,
        "message" => "Password must be at least 6 characters"
    ]);
    exit;
}

$id_number = trim($_POST["id_number"] ?? "");
$vehicle_type = trim($_POST["vehicle_type"] ?? "");
$employee_id = trim($_POST["employee_id"] ?? "");
$admin_role = trim($_POST["admin_role"] ?? "");

if ($user_type == "shopper" && ($id_number == "" || $vehicle_type == "")) {
    echo json_encode([
        "success" => false,
        "message" => "Please fill in all shopper fields"
    ]);
    exit;
}

if ($user_type == "admin" && ($employee_id == "" || $admin_role == "")) {
    echo json_encode([
        "success" => false,
        "message" => "Please fill in all admin fields"
    ]);
    exit;
}

$password_hash = password_hash($password, PASSWORD_DEFAULT);

try {
    $conn->beginTransaction();

    $user_stmt = $conn->prepare(
        "INSERT INTO users (full_name, email, password_hash, user_type)
         VALUES (:full_name, :email, :password_hash, :user_type)
         RETURNING user_id"
    );

    $user_stmt->execute([
        ":full_name" => $full_name,
        ":email" => $email,
        ":password_hash" => $password_hash,
        ":user_type" => $user_type
    ]);

    $user_id = $user_stmt->fetchColumn();

    if ($user_type == "customer") {
        $type_stmt = $conn->prepare(
            "INSERT INTO customers (user_id)
             VALUES (:user_id)"
        );
        $type_stmt->execute([
            ":user_id" => $user_id
        ]);
    } else if ($user_type == "shopper") {
        $type_stmt = $conn->prepare(
            "INSERT INTO shoppers (user_id, id_number, vehicle_type)
             VALUES (:user_id, :id_number, :vehicle_type)"
        );
        $type_stmt->execute([
            ":user_id" => $user_id,
            ":id_number" => $id_number,
            ":vehicle_type" => $vehicle_type
        ]);
    } else if ($user_type == "admin") {
        $type_stmt = $conn->prepare(
            "INSERT INTO admins (user_id, employee_id, admin_role)
             VALUES (:user_id, :employee_id, :admin_role)"
        );
        $type_stmt->execute([
            ":user_id" => $user_id,
            ":employee_id" => $employee_id,
            ":admin_role" => $admin_role
        ]);
    }

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Registration successful"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    if ($e->getCode() == "23505") {
        echo json_encode([
            "success" => false,
            "message" => "Email already exists"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Registration failed"
        ]);
    }
}
?>
