package com.example.defilending.dto;

import java.math.BigInteger;

public record CreateLoanRequest(
        BigInteger collateral,
        BigInteger durationDays
) {}
