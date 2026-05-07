#!/usr/bin/env bash
# Sample cURL calls against order-service (default: http://localhost:8080).
# Usage:
#   ./scripts/samples/api-curl-samples.sh
#   BASE_URL=http://localhost:8080 TRADER=demo-trader ./scripts/samples/api-curl-samples.sh
#
# Requires: curl. Optional: jq (to capture order ids for fill/cancel steps).

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TRADER="${TRADER:-1001}"

echo "=== Base URL: ${BASE_URL}  Trader: ${TRADER} ==="
echo

# --- Add position (so a later SELL can validate) ----------------------------

echo "--- Add position: JPM ---"
curl -sS -X POST "${BASE_URL}/portfolios/${TRADER}/positions" \
  -H 'Content-Type: application/json' \
  -d '{"stock":"JPM","sector":"FINANCE","quantity":100}' | tee /tmp/sample-portfolio-1.json
echo
echo

# --- Place up to 3 PENDING orders (cap) -------------------------------------

echo "--- Place BUY AAPL (PENDING) ---"
curl -sS -X POST "${BASE_URL}/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"traderId\":\"${TRADER}\",\"stock\":\"AAPL\",\"sector\":\"TECH\",\"quantity\":10,\"side\":\"BUY\"}" | tee /tmp/sample-order-buy-aapl.json
echo
echo

echo "--- Place BUY MSFT (PENDING) ---"
curl -sS -X POST "${BASE_URL}/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"traderId\":\"${TRADER}\",\"stock\":\"MSFT\",\"sector\":\"TECH\",\"quantity\":5,\"side\":\"BUY\"}" | tee /tmp/sample-order-buy-msft.json
echo
echo

echo "--- Place SELL JPM (PENDING; uses existing position qty) ---"
curl -sS -X POST "${BASE_URL}/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"traderId\":\"${TRADER}\",\"stock\":\"JPM\",\"sector\":\"FINANCE\",\"quantity\":25,\"side\":\"SELL\"}" | tee /tmp/sample-order-sell-jpm.json
echo
echo

echo "--- Fourth PENDING (expect HTTP 400 + JSON error) ---"
HTTP_CODE="$(curl -sS -o /tmp/sample-order-fourth-body.json -w "%{http_code}" -X POST "${BASE_URL}/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"traderId\":\"${TRADER}\",\"stock\":\"NVDA\",\"sector\":\"TECH\",\"quantity\":1,\"side\":\"BUY\"}")"
echo "status=${HTTP_CODE}"
cat /tmp/sample-order-fourth-body.json
echo
echo

# --- Read portfolio & sector overlap ----------------------------------------

echo "--- Get portfolio ---"
curl -sS "${BASE_URL}/portfolios/${TRADER}" | tee /tmp/sample-portfolio-2.json
echo
echo

echo "--- Sector overlap ---"
curl -sS "${BASE_URL}/portfolios/${TRADER}/sector-overlap" | tee /tmp/sample-overlap-1.json
echo
echo

# --- Cancel two PENDING orders (need jq for ids) ------------------------------

if command -v jq >/dev/null 2>&1; then
  ID_MSFT="$(jq -r '.id' /tmp/sample-order-buy-msft.json)"
  ID_JPM_SELL="$(jq -r '.id' /tmp/sample-order-sell-jpm.json)"

  echo "--- Cancel PENDING MSFT BUY (id=${ID_MSFT}) ---"
  curl -sS -X POST "${BASE_URL}/orders/${ID_MSFT}/cancel" | tee /tmp/sample-order-cancelled-msft.json
  echo
  echo

  echo "--- Cancel PENDING JPM SELL (id=${ID_JPM_SELL}) ---"
  curl -sS -X POST "${BASE_URL}/orders/${ID_JPM_SELL}/cancel" | tee /tmp/sample-order-cancelled-jpm.json
  echo
  echo
else
  echo "!!! Install jq to auto-run cancel/fill steps, or run the manual curls in api-curl-samples.txt"
  exit 0
fi

# --- After cancels: only AAPL BUY should be PENDING; add another BUY then fill -

echo "--- Place BUY NVDA (PENDING; cap allows after cancels) ---"
curl -sS -X POST "${BASE_URL}/orders" \
  -H 'Content-Type: application/json' \
  -d "{\"traderId\":\"${TRADER}\",\"stock\":\"NVDA\",\"sector\":\"TECH\",\"quantity\":2,\"side\":\"BUY\"}" | tee /tmp/sample-order-buy-nvda.json
echo
echo

echo "--- Fill PENDING AAPL BUY ---"
ID_AAPL="$(jq -r '.id' /tmp/sample-order-buy-aapl.json)"
curl -sS -X POST "${BASE_URL}/orders/${ID_AAPL}/fill" | tee /tmp/sample-order-filled-aapl.json
echo
echo

echo "--- Fill PENDING NVDA BUY ---"
ID_NVDA="$(jq -r '.id' /tmp/sample-order-buy-nvda.json)"
curl -sS -X POST "${BASE_URL}/orders/${ID_NVDA}/fill" | tee /tmp/sample-order-filled-nvda.json
echo
echo

# --- Add another position & re-read -------------------------------------------

echo "--- Add position: XOM ---"
curl -sS -X POST "${BASE_URL}/portfolios/${TRADER}/positions" \
  -H 'Content-Type: application/json' \
  -d '{"stock":"XOM","sector":"ENERGY","quantity":30}' | tee /tmp/sample-portfolio-3.json
echo
echo

echo "--- Get portfolio (final) ---"
curl -sS "${BASE_URL}/portfolios/${TRADER}"
echo
echo

echo "--- Sector overlap (final) ---"
curl -sS "${BASE_URL}/portfolios/${TRADER}/sector-overlap"
echo
echo

echo "Done."
