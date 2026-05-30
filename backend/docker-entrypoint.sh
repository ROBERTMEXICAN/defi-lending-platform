#!/bin/sh
set -e

SHARED=/shared
ENV_FILE="$SHARED/contracts.env"

echo "Waiting for contracts to be deployed..."
until [ -f "$ENV_FILE" ]; do
  sleep 2
done
echo "contracts.env found, loading addresses..."

# Load exported contract addresses
export $(grep -v '^#' "$ENV_FILE" | xargs)

# Hardhat account #0 private key (well-known, only for local dev)
PRIVATE_KEY="${PRIVATE_KEY:-0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}"

exec java -jar app.jar \
  --blockchain.enabled=true \
  --blockchain.rpc-url=http://hardhat:8545 \
  --blockchain.contract-address="${CONTRACT_ADDRESS}" \
  --blockchain.loan-token-address="${LOAN_TOKEN_ADDRESS}" \
  --blockchain.collateral-token-address="${COLLATERAL_TOKEN_ADDRESS}" \
  --blockchain.private-key="${PRIVATE_KEY}"
