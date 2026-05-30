const hre = require("hardhat");

async function main() {
  const tokenAddress = "0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0";
  const lendingAddress = "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9";

  const token = await hre.ethers.getContractAt("MockERC20", tokenAddress);

  const mintAmount = 1000000;
  const tx1 = await token.mint(lendingAddress, mintAmount);
  await tx1.wait();

  console.log("Minted", mintAmount, "tokens to LendingProtocol");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});