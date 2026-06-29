-- Increase investor wallets so the demo race (85-unit pre-fill + concurrent last 15)
-- fits within a single investor's balance at $25,000/unit.
SET NAMES utf8mb4;

UPDATE investor_profiles ip
JOIN users u ON u.id = ip.user_id
SET ip.wallet_balance = 3000000.0000,
    ip.locked_balance = 0.0000
WHERE u.email IN ('investor1@tranche.local', 'investor2@tranche.local');
