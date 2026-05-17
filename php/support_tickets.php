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

function is_support_admin($role) {
    $role = strtolower(trim($role));
    return $role == "support team" || $role == "support" || $role == "developer";
}

try {
    $user_stmt = $conn->prepare(
        "SELECT u.user_type, a.admin_role, COALESCE(a.verified, TRUE) AS admin_verified
         FROM users u
         LEFT JOIN admins a ON a.user_id = u.user_id
         WHERE u.user_id = :user_id"
    );
    $user_stmt->execute([
        ":user_id" => $user_id
    ]);
    $user = $user_stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        echo json_encode([
            "success" => false,
            "message" => "User not found"
        ]);
        exit;
    }

    $is_admin = $user["user_type"] == "admin";
    if ($is_admin) {
        $verified = !($user["admin_verified"] === false || $user["admin_verified"] === "f" || $user["admin_verified"] === "0");
        if (!$verified || !is_support_admin($user["admin_role"])) {
            echo json_encode([
                "success" => false,
                "message" => "Only support-team admins can view support tickets"
            ]);
            exit;
        }

        $stmt = $conn->prepare(
            "SELECT t.ticket_id, t.subject, t.status, t.created_at, t.updated_at,
                    u.full_name, u.user_type,
                    (
                        SELECT m.message
                        FROM support_ticket_messages m
                        WHERE m.ticket_id = t.ticket_id
                        ORDER BY m.created_at DESC
                        LIMIT 1
                    ) AS last_message
             FROM support_tickets t
             JOIN users u ON u.user_id = t.user_id
             WHERE t.status <> 'closed'
             ORDER BY t.updated_at DESC, t.created_at DESC"
        );
        $stmt->execute();
    } else {
        $stmt = $conn->prepare(
            "SELECT t.ticket_id, t.subject, t.status, t.created_at, t.updated_at,
                    u.full_name, u.user_type,
                    (
                        SELECT m.message
                        FROM support_ticket_messages m
                        WHERE m.ticket_id = t.ticket_id
                        ORDER BY m.created_at DESC
                        LIMIT 1
                    ) AS last_message
             FROM support_tickets t
             JOIN users u ON u.user_id = t.user_id
             WHERE t.user_id = :user_id
               AND t.status <> 'closed'
             ORDER BY t.updated_at DESC, t.created_at DESC"
        );
        $stmt->execute([
            ":user_id" => $user_id
        ]);
    }

    echo json_encode([
        "success" => true,
        "tickets" => $stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load support tickets"
    ]);
}
?>
