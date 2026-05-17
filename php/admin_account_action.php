<?php
header("Content-Type: application/json");
require "db.php";

$actor_user_id = $_POST["actor_user_id"] ?? "";
$target_user_id = $_POST["target_user_id"] ?? "";
$action = $_POST["action"] ?? "";

$valid_actions = [
    "suspend_customer",
    "unsuspend_customer",
    "verify_shopper",
    "unverify_shopper",
    "verify_admin",
    "unverify_admin"
];

if ($actor_user_id == "" || $target_user_id == "" || !in_array($action, $valid_actions)) {
    echo json_encode([
        "success" => false,
        "message" => "Missing action details"
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

    if (($action == "verify_admin" || $action == "unverify_admin") && !$actor_is_developer) {
        echo json_encode([
            "success" => false,
            "message" => "Only developers can verify or unverify admins"
        ]);
        exit;
    }

    if ($action == "verify_admin" || $action == "unverify_admin") {
        $target_stmt = $conn->prepare(
            "SELECT admin_role
             FROM admins
             WHERE user_id = :target_user_id"
        );
        $target_stmt->execute([
            ":target_user_id" => $target_user_id
        ]);
        $target_role = $target_stmt->fetchColumn();

        if (!$target_role) {
            echo json_encode([
                "success" => false,
                "message" => "Admin not found"
            ]);
            exit;
        }
    }

    if ($action == "suspend_customer" || $action == "unsuspend_customer") {
        $suspended = $action == "suspend_customer" ? "TRUE" : "FALSE";
        $stmt = $conn->prepare(
            "UPDATE users
             SET suspended = $suspended
             WHERE user_id = :target_user_id
               AND user_type = 'customer'"
        );
    } else if ($action == "verify_shopper" || $action == "unverify_shopper") {
        $approved = $action == "verify_shopper" ? "TRUE" : "FALSE";
        $stmt = $conn->prepare(
            "UPDATE shoppers
             SET approved = $approved
             WHERE user_id = :target_user_id"
        );
    } else {
        $verified = $action == "verify_admin" ? "TRUE" : "FALSE";
        $stmt = $conn->prepare(
            "UPDATE admins
             SET verified = $verified
             WHERE user_id = :target_user_id"
        );
    }

    $stmt->execute([
        ":target_user_id" => $target_user_id
    ]);

    if ($stmt->rowCount() == 0) {
        echo json_encode([
            "success" => false,
            "message" => "No matching account was updated"
        ]);
        exit;
    }

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
