<?php
header("Content-Type: application/json");
require "db.php";

$user_id = $_POST["user_id"] ?? "";
$subject = trim($_POST["subject"] ?? "");
$message = trim($_POST["message"] ?? "");

if ($user_id == "" || $subject == "" || $message == "") {
    echo json_encode([
        "success" => false,
        "message" => "Enter a subject and message"
    ]);
    exit;
}

try {
    $user_stmt = $conn->prepare(
        "SELECT user_type, COALESCE(suspended, FALSE) AS suspended
         FROM users
         WHERE user_id = :user_id"
    );
    $user_stmt->execute([
        ":user_id" => $user_id
    ]);
    $user = $user_stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user || !in_array($user["user_type"], ["customer", "shopper"])) {
        echo json_encode([
            "success" => false,
            "message" => "Only customers and shoppers can create support tickets"
        ]);
        exit;
    }

    if ($user["suspended"] === true || $user["suspended"] === "t" || $user["suspended"] === "1") {
        echo json_encode([
            "success" => false,
            "message" => "Suspended accounts cannot create support tickets"
        ]);
        exit;
    }

    $conn->beginTransaction();

    $ticket_stmt = $conn->prepare(
        "INSERT INTO support_tickets (user_id, subject, status)
         VALUES (:user_id, :subject, 'open')
         RETURNING ticket_id"
    );
    $ticket_stmt->execute([
        ":user_id" => $user_id,
        ":subject" => $subject
    ]);
    $ticket_id = $ticket_stmt->fetchColumn();

    $message_stmt = $conn->prepare(
        "INSERT INTO support_ticket_messages (ticket_id, sender_user_id, message)
         VALUES (:ticket_id, :user_id, :message)"
    );
    $message_stmt->execute([
        ":ticket_id" => $ticket_id,
        ":user_id" => $user_id,
        ":message" => $message
    ]);

    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Support ticket created",
        "ticket_id" => $ticket_id
    ]);
} catch (PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }

    echo json_encode([
        "success" => false,
        "message" => "Could not create support ticket"
    ]);
}
?>
