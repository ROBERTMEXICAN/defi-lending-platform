const hre = require("hardhat");
const fs = require("fs");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying with account:", deployer.address);

  const MockERC20 = await hre.ethers.getContractFactory("MockERC20");
  const token = await MockERC20.deploy("Mock USD", "mUSD");
  await token.waitForDeployment();
  const tokenAddress = await token.getAddress();
  console.log("MockERC20 deployed to:", tokenAddress);

  const LendingProtocol = await hre.ethers.getContractFactory("LendingProtocol");
  const lending = await LendingProtocol.deploy(tokenAddress, tokenAddress);
  await lending.waitForDeployment();
  const lendingAddress = await lending.getAddress();
  console.log("LendingProtocol deployed to:", lendingAddress);

  // Fund the protocol with loan tokens so it can issue loans
  const mintAmount = hre.ethers.parseUnits("1000000", 0);
  const tx = await token.mint(lendingAddress, mintAmount);
  await tx.wait();
  console.log("Minted", mintAmount.toString(), "tokens to LendingProtocol");

  // Export addresses for backend startup
  const envContent = [
    `CONTRACT_ADDRESS=${lendingAddress}`,
    `LOAN_TOKEN_ADDRESS=${tokenAddress}`,
    `COLLATERAL_TOKEN_ADDRESS=${tokenAddress}`,
    `DEPLOYER_ADDRESS=${deployer.address}`,
  ].join("\n") + "\n";

  const outPath = process.env.EXPORT_PATH || "/shared/contracts.env";
  fs.writeFileSync(outPath, envContent);
  console.log("Exported addresses to", outPath);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
