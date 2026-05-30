const hre = require("hardhat");

async function main() {
  const [deployer] = await hre.ethers.getSigners();

  console.log("Deploying with account:", deployer.address);

  const MockERC20 = await hre.ethers.getContractFactory("MockERC20");

  const token = await MockERC20.deploy("Mock USD", "mUSD");
  await token.waitForDeployment();

  const tokenAddress = await token.getAddress();
  console.log("MockERC20 deployed to:", tokenAddress);

  const LendingProtocol = await hre.ethers.getContractFactory("LendingProtocol");

  const lending = await LendingProtocol.deploy(
    tokenAddress,
    tokenAddress
  );

  await lending.waitForDeployment();

  const lendingAddress = await lending.getAddress();
  console.log("LendingProtocol deployed to:", lendingAddress);

  console.log("");
  console.log("Use these values for backend:");
  console.log("BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545");
  console.log("TOKEN_CONTRACT_ADDRESS=" + tokenAddress);
  console.log("LENDING_CONTRACT_ADDRESS=" + lendingAddress);
  console.log("PRIVATE_KEY=take it from hardhat node account #0");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});