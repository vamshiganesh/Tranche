-- Email verification, investor KYC, and issuer KYB status for onboarding gates.

SET NAMES utf8mb4;

ALTER TABLE users
    ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER enabled,
    ADD COLUMN email_verification_code VARCHAR(6) NULL AFTER email_verified,
    ADD COLUMN email_verification_expires_at TIMESTAMP(6) NULL AFTER email_verification_code;

ALTER TABLE investor_profiles
    ADD COLUMN kyc_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' AFTER currency;

ALTER TABLE issuers
    ADD COLUMN verification_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' AFTER registration_number;

-- Seeded demo users: pre-verified and approved so existing demos keep working.
UPDATE users
SET email_verified = 1,
    email_verification_code = NULL,
    email_verification_expires_at = NULL
WHERE email IN (
    'admin@tranche.local',
    'issuer@tranche.local',
    'investor1@tranche.local',
    'investor2@tranche.local'
);

UPDATE investor_profiles ip
    INNER JOIN users u ON u.id = ip.user_id
SET ip.kyc_status = 'APPROVED'
WHERE u.email IN ('investor1@tranche.local', 'investor2@tranche.local');

UPDATE issuers i
    INNER JOIN users u ON u.id = i.user_id
SET i.verification_status = 'APPROVED'
WHERE u.email = 'issuer@tranche.local';
