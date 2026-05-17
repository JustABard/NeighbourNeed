<?php
header("Content-Type: application/json");
require "db.php";

$ticket_id = $_POST["ticket_id"] ?? "";
$user_id = $_POST["user_id"] ?? "";

if ($ticket_id == "" || $user_id == "") {
    echo json_encode([
        "success" => false,
        "message" => "Missing ticket or user"
    ]);
    exit;
}

function is_support_admin($role) {
    $role = strtolower(trim($role));
    return $role == "support team" || $role == "support" || $role == "developer";
}

function can_access_ticket($conn, $ticket_id, $user_id) {
    $access_stmt = $conn->prepare(
        "SELECT t.user_id AS owner_user_id, u.user_type, a.admin_role,
                COALESCE(a.verified, TRUE) AS admin_verified
         FROM support_tickets t
         CROSS JOIN users u
         LEFT JOIN admins a ON a.user_id = u.user_id
         WHERE t.ticket_id = :ticket_id
           AND u.user_id = :user_id"
    );
    $access_stmt->execute([
        ":ticket_id" => $ticket_id,
        ":user_id" => $user_id
    ]);
    $access = $access_stmt->fetch(PDO::FETCH_ASSOC);

    if (!$access) {
        return false;
    }

    if ((string)$access["owner_user_id"] == (string)$user_id) {
        return true;
    }

    $verified = !($access["admin_verified"] === false || $access["admin_verified"] === "f" || $access["admin_verified"] === "0");
    return $access["user_type"] == "admin" && $verified && is_support_admin($access["admin_role"]);
}

try {
    if (!can_access_ticket($conn, $ticket_id, $user_id)) {
        echo json_encode([
            "success" => false,
            "message" => "You cannot view this support ticket"
        ]);
        exit;
    }

    $ticket_stmt = $conn->prepare(
        "SELECT t.ticket_id, t.subject, t.status, t.created_at, t.updated_at,
                u.full_name, u.user_type
         FROM support_tickets t
         JOIN users u ON u.user_id = t.user_id
         WHERE t.ticket_id = :ticket_id"
    );
    $ticket_stmt->execute([
        ":ticket_id" => $ticket_id
    ]);
    $ticket = $ticket_stmt->fetch(PDO::FETCH_ASSOC);

    $messages_stmt = $conn->prepare(
        "SELECT m.support_message_id, m.message, m.created_at,
                u.full_name AS sender_name, u.user_type AS sender_type
         FROM support_ticket_messages m
         JOIN users u ON u.user_id = m.sender_user_id
         WHERE m.ticket_id = :ticket_id
         ORDER BY m.created_at ASC"
    );
    $messages_stmt->execute([
        ":ticket_id" => $ticket_id
    ]);

    echo json_encode([
        "success" => true,
        "ticket" => $ticket,
        "messages" => $messages_stmt->fetchAll(PDO::FETCH_ASSOC)
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Could not load support ticket"
    ]);
}
?>
