package com.example.defilending.controller;

import com.example.defilending.dto.CreateLoanRequest;
import com.example.defilending.dto.LoanResponse;
import com.example.defilending.service.LendingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LendingController {
    private final LendingService lendingService;

    public LendingController(LendingService lendingService) {
        this.lendingService = lendingService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/blockchain/status")
    public Map<String, Object> blockchainStatus() {
        return lendingService.status();
    }

    @PostMapping("/tokens/collateral/approve/{amount}")
    public Map<String, String> approveCollateral(@PathVariable BigInteger amount) {
        return Map.of("txHash", lendingService.approveCollateral(amount));
    }

    @PostMapping("/tokens/loan/approve/{amount}")
    public Map<String, String> approveLoanToken(@PathVariable BigInteger amount) {
        return Map.of("txHash", lendingService.approveLoanToken(amount));
    }

    @PostMapping("/loans")
    public LoanResponse createLoan(@RequestBody CreateLoanRequest request) {
        return lendingService.createLoan(request);
    }

    @GetMapping("/loans/{id}")
    public LoanResponse getLoan(@PathVariable BigInteger id) {
        return lendingService.getLoan(id);
    }

    @PostMapping("/loans/{id}/repay")
    public LoanResponse repay(@PathVariable BigInteger id) {
        return lendingService.repay(id);
    }

    @PostMapping("/loans/{id}/liquidate")
    public LoanResponse liquidate(@PathVariable BigInteger id) {
        return lendingService.liquidate(id);
    }

    @GetMapping("/loans/{id}/interest")
    public Map<String, BigInteger> interest(@PathVariable BigInteger id) {
        return Map.of("interest", lendingService.calculateInterest(id));
    }

    @GetMapping("/loans/{id}/risk")
    public Map<String, Integer> risk(@PathVariable BigInteger id) {
        return Map.of("riskLevel", lendingService.assessRisk(id));
    }
}
