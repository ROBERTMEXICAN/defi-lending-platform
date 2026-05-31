#!/bin/sh
set -e

SHARED=/shared
mkdir -p "$SHARED"

echo "Starting Hardhat node..."
npx hardhat node --hostname 0.0.0.0 &
HARDHAT_PID=$!

# Wait until the node is accepting connections
echo "Waiting for Hardhat node to be ready..."
until nc -z localhost 8545 2>/dev/null; do
  sleep 1
done
echo "Hardhat node is ready."

echo "Deploying contracts..."
EXPORT_PATH="$SHARED/contracts.env" npx hardhat run scripts/deploy-and-export.js --network localhost

echo "Deployment complete. Keeping node running..."
wait $HARDHAT_PID
