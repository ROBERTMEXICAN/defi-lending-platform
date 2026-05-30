package com.example.defilending.dto;

import java.math.BigInteger;

public record CreateLoanRequest(
        BigInteger amount,
        BigInteger collateral,
        BigInteger interestRate,
        BigInteger durationDays
) {}
