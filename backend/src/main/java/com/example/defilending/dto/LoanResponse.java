package com.example.defilending.dto;

import java.math.BigInteger;

public record LoanResponse(
        BigInteger id,
        String borrower,
        BigInteger amount,
        BigInteger collateral,
        BigInteger interestRate,
        BigInteger durationDays,
        String status,
        BigInteger startTime,
        BigInteger endTime,
        BigInteger repaymentAmount
) {}
