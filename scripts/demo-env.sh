#!/usr/bin/env bash
# Source in every terminal that runs demo curl commands:
#   source scripts/demo-env.sh
#
# Requires: curl, jq, API running at localhost:8080

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASSWORD="${DEMO_PASSWORD:-Password123!}"

login() {
  local email="$1"
  curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$PASSWORD\"}" \
    | jq -r .accessToken
}

export ADMIN_TOKEN="$(login admin@tranche.local)"
export ISSUER_TOKEN="$(login issuer@tranche.local)"
export INV1_TOKEN="$(login investor1@tranche.local)"
export INV2_TOKEN="$(login investor2@tranche.local)"

echo "Tokens loaded: ADMIN_TOKEN ISSUER_TOKEN INV1_TOKEN INV2_TOKEN"
echo "Verify investor1: $(curl -s "$BASE_URL/api/v1/auth/me" -H "Authorization: Bearer $INV1_TOKEN" | jq -r '.email // .error.code')"
