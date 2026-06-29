-- Demo seed data for local development and integration tests.
-- Default password for all seeded users: Password123!
-- BCrypt hash generated with cost factor 10.

SET NAMES utf8mb4;

-- Fixed UUIDs for reproducible demos and API examples
SET @admin_public_id    = 'a0000000-0000-4000-8000-000000000001';
SET @issuer_public_id   = 'a0000000-0000-4000-8000-000000000002';
SET @investor1_public_id = 'a0000000-0000-4000-8000-000000000003';
SET @investor2_public_id = 'a0000000-0000-4000-8000-000000000004';
SET @password_hash = '$2b$10$pwnJ.QxEtC8BFJ3oj/XP0e9vraWkWLE2UK5GralUA2SWTcwTvqmqy';

INSERT INTO users (public_id, email, password_hash, full_name, role)
VALUES
    (@admin_public_id,     'admin@tranche.local',     @password_hash, 'Platform Admin',  'ADMIN'),
    (@issuer_public_id,    'issuer@tranche.local',    @password_hash, 'Acme Issuer',     'ISSUER'),
    (@investor1_public_id, 'investor1@tranche.local', @password_hash, 'Jane Investor',   'INVESTOR'),
    (@investor2_public_id, 'investor2@tranche.local', @password_hash, 'John Investor',   'INVESTOR');

SET @admin_user_id     = (SELECT id FROM users WHERE email = 'admin@tranche.local');
SET @issuer_user_id    = (SELECT id FROM users WHERE email = 'issuer@tranche.local');
SET @investor1_user_id = (SELECT id FROM users WHERE email = 'investor1@tranche.local');
SET @investor2_user_id = (SELECT id FROM users WHERE email = 'investor2@tranche.local');

INSERT INTO issuers (user_id, company_name, registration_number)
VALUES (@issuer_user_id, 'Acme Corp', 'REG-12345');

INSERT INTO investor_profiles (user_id, wallet_balance, locked_balance, currency)
VALUES
    (@investor1_user_id, 500000.0000, 0.0000, 'USD'),
    (@investor2_user_id, 500000.0000, 0.0000, 'USD');
