<?php
header("Content-Type: application/json");
require "db.php";

$ticket_id = $_POST["ticket_id"] ?? "";
$user_id = $_POST["user_id"] ?? "";
$message = trim($_POST["message"] ?? "");

if ($ticket_id == "" || $user_id == "" || $message == "") {
    echo json_encode([
        "success" => false,
        "message" => "Enter a reply"
    ]);
    exit;
}

function is_support_admin($role) {
    $role = strtolower(trim($role));
    return $role == "support team" || $role == "support" || $role == "developer";
}

try {
    $access_stmt = $conn->prepare(
        "SELECT t.user_id AS owner_user_id, t.status, u.user_type, a.admin_role,
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
        echo json_encode([
            "success" => false,
            "message" => "Support ticket not found"
        ]);
        exit;
    }

    if ($access["status"] == "closed") {
        echo json_encode([
            "success" => false,
            "message" => "This support ticket is closed"
        ]);
        exit;
    }

    $is_owner = (string)$access["owner_user_id"] == (string)$user_id;
    $admin_verified = !($access["admin_verified"] === false || $access["admin_verified"] === "f" || $access["admin_verified"] === "0");
    $is_support_admin = $access["user_type"] == "admin" && $admin_verified && is_support_admin($access["admin_role"]);

    if (!$is_owner && !$is_support_admin) {
        echo json_encode([
            "success" => false,
            "message" => "You cannot reply to this support ticket"
        ]);
        exit;
    }

    $conn->beginTransaction();

    $message_stmt = $conn->prepare(
        "INSERT INTO support_ticket_messages (ticket_id, sender_user_id, message)
         VALUES (:ticket_id, :user_id, :message)"
    );
    $message_stmt->execute([
        ":ticket_id" => $ticket_id,
        ":user_id" => $user_id,
        ":message" => $message
    ]);

    $status = $is_support_admin ? "answered" : "open";
    $ticket_stmt = $conn->prepare(
        "UPDATE support_tickets
         SET status = :status,
             updated_at = CURRENT_TIMESTAMP
         WHERE ticket_id = :ticket_id"
    );
    $ticket_stmt->execute([
        ":status" => $status,
        ":ticket_id" => $ticket_id
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Reply posted"
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not post reply"
    ]);
}
?>
