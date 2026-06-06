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

  // Fund the protocol with loan tokens so it can issue loans (raw units, no decimals scaling)
  const mintAmount = BigInt("1000000");
  const tx = await token.mint(lendingAddress, mintAmount);
  await tx.wait();
  console.log("Minted 1000000 tokens to LendingProtocol");

  // Fund the deployer with collateral tokens so they can create loans
  const userMintAmount = BigInt("100000");
  const tx2 = await token.mint(deployer.address, userMintAmount);
  await tx2.wait();
  console.log("Minted 100000 tokens to deployer", deployer.address);

  // Auto-approve the lending contract to spend deployer's collateral tokens
  const tx3 = await token.approve(lendingAddress, userMintAmount);
  await tx3.wait();
  console.log("Approved LendingProtocol to spend deployer collateral tokens");

  // Fund all extra addresses from EXTRA_ADDRESSES env var (comma-separated)
  const extraAddresses = process.env.EXTRA_ADDRESSES
    ? process.env.EXTRA_ADDRESSES.split(",").map(a => a.trim()).filter(a => a.length > 0)
    : [];
  for (const addr of extraAddresses) {
    // Mint mUSD tokens
    const txMint = await token.mint(addr, userMintAmount);
    await txMint.wait();
    console.log("Minted 100000 tokens to", addr);

    // Send ETH for gas fees (1 ETH)
    const txEth = await deployer.sendTransaction({
      to: addr,
      value: hre.ethers.parseEther("1.0")
    });
    await txEth.wait();
    console.log("Sent 1 ETH to", addr);
  }

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
