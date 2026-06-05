const hre = require("hardhat");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying with account:", deployer.address);

  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("Account balance:", hre.ethers.formatEther(balance), "ETH");

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

  // Минтим токены на контракт
  const mintAmount = hre.ethers.parseUnits("1000000", 0);
  await (await token.mint(lendingAddress, mintAmount)).wait();
  console.log("Minted 1000000 tokens to LendingProtocol");

  // Минтим токены деплоеру
  const userAmount = hre.ethers.parseUnits("100000", 0);
  await (await token.mint(deployer.address, userAmount)).wait();
  console.log("Minted 100000 tokens to deployer:", deployer.address);

  console.log("\n✅ Деплой завершён! Сохрани эти значения:");
  console.log("SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/cf884a93e8d84984848b43dc303913f9");
  console.log("CONTRACT_ADDRESS=" + lendingAddress);
  console.log("LOAN_TOKEN_ADDRESS=" + tokenAddress);
  console.log("COLLATERAL_TOKEN_ADDRESS=" + tokenAddress);
  console.log("DEPLOYER_PRIVATE_KEY=de36f4953d0bd66bb24cc0e070639bead4fca2b4f595a53f8a1edab0910435ba");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
