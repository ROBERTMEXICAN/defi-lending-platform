package com.example.defilending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainProperties {
    private boolean enabled;
    private String rpcUrl;
    private String contractAddress;
    private String loanTokenAddress;
    private String collateralTokenAddress;
    private String privateKey;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getRpcUrl() { return rpcUrl; }
    public void setRpcUrl(String rpcUrl) { this.rpcUrl = rpcUrl; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public String getLoanTokenAddress() { return loanTokenAddress; }
    public void setLoanTokenAddress(String loanTokenAddress) { this.loanTokenAddress = loanTokenAddress; }

    public String getCollateralTokenAddress() { return collateralTokenAddress; }
    public void setCollateralTokenAddress(String collateralTokenAddress) { this.collateralTokenAddress = collateralTokenAddress; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
}