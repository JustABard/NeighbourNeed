<?php
header("Content-Type: application/json");
require "db.php";

$actor_user_id = $_POST["actor_user_id"] ?? "";
$account_type = $_POST["account_type"] ?? "";

if ($actor_user_id == "" || !in_array($account_type, ["customers", "shoppers", "admins"])) {
    echo json_encode([
        "success" => false,
        "message" => "Missing admin or account type"
    ]);
    exit;
}

function is_developer_role($role) {
    return strtolower(trim($role)) == "developer";
}

try {
    $actor_stmt = $conn->prepare(
        "SELECT a.admin_role, COALESCE(a.verified, TRUE) AS verified
         FROM admins a
         JOIN users u ON u.user_id = a.user_id
         WHERE a.user_id = :actor_user_id
           AND u.user_type = 'admin'"
    );
    $actor_stmt->execute([
        ":actor_user_id" => $actor_user_id
    ]);
    $actor = $actor_stmt->fetch(PDO::FETCH_ASSOC);

    if (!$actor || $actor["verified"] === false || $actor["verified"] === "f" || $actor["verified"] === "0") {
        echo json_encode([
            "success" => false,
            "message" => "Admin access is not verified"
        ]);
        exit;
    }

    $actor_is_developer = is_developer_role($actor["admin_role"]);

    if ($account_type == "customers") {
        $stmt = $conn->prepare(
            "SELECT u.user_id, u.full_name, u.email, COALESCE(u.suspended, FALSE) AS suspended, u.created_at
             FROM users u
             JOIN customers c ON c.user_id = u.user_id
             ORDER BY u.full_name ASC"
        );
        $stmt->execute();
    } else if ($account_type == "shoppers") {
        $stmt = $conn->prepare(
            "SELECT u.user_id, u.full_name, u.email, s.id_number, s.vehicle_type,
                    COALESCE(s.approved, FALSE) AS approved, u.created_at
             FROM users u
             JOIN shoppers s ON s.user_id = u.user_id
             ORDER BY u.full_name ASC"
        );
        $stmt->execute();
    } else {
        if ($actor_is_developer) {
            $stmt = $conn->prepare(
                "SELECT u.user_id, u.full_name, u.email, a.employee_id, a.admin_role,
                        COALESCE(a.verified, TRUE) AS verified, u.created_at
                 FROM users u
                 JOIN admins a ON a.user_id = u.user_id
                 ORDER BY a.admin_role ASC, u.full_name ASC"
            );
            $stmt->execute();
        } else {
            $stmt = $conn->prepare(
                "SELECT u.user_id, u.full_name, u.email, a.employee_id, a.admin_role,
                        COALESCE(a.verified, TRUE) AS verified, u.created_at
                 FROM users u
                 JOIN admins a ON a.user_id = u.user_id
                 WHERE LOWER(TRIM(a.admin_role)) <> 'developer'
                 ORDER BY a.admin_role ASC, u.full_name ASC"
            );
            $stmt->execute();
        }
    }

    echo json_encode([
        "success" => true,
        "actor_role" => $actor["admin_role"],
        "accounts" => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load accounts"
    ]);
}
?>
