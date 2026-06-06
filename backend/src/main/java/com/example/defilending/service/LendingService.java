package com.example.defilending.service;

import com.example.defilending.config.BlockchainProperties;
import com.example.defilending.dto.CreateLoanRequest;
import com.example.defilending.dto.LoanResponse;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LendingService {
    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    private final AtomicLong counter = new AtomicLong(0);
    private final Map<BigInteger, LoanResponse> demoStorage = new ConcurrentHashMap<>();
    // address (lowercase) -> list of loan IDs owned by that address
    private final Map<String, List<BigInteger>> loansByAddress = new ConcurrentHashMap<>();
    private final BlockchainProperties props;

    public LendingService(BlockchainProperties props) {
        this.props = props;
    }

    public boolean blockchainEnabled() {
        return props.isEnabled()
                && props.getContractAddress() != null
                && !props.getContractAddress().isBlank()
                && !ZERO_ADDRESS.equalsIgnoreCase(props.getContractAddress())
                && props.getPrivateKey() != null
                && !props.getPrivateKey().isBlank();
    }

    public LoanResponse createLoan(CreateLoanRequest request, String callerAddress) {
        if (blockchainEnabled()) {
            try {
                Function function = new Function(
                        "createLoan",
                        Arrays.asList(
                                new Uint256(request.collateral()),
                                new Uint256(request.durationDays())
                        ),
                        List.of(new TypeReference<Uint256>() {})
                );
                sendTransaction(props.getContractAddress(), function);

                BigInteger id = callUint(
                        props.getContractAddress(),
                        new Function(
                                "loanCounter",
                                List.of(),
                                List.of(new TypeReference<Uint256>() {})
                        )
                );

                recordLoan(callerAddress, id);
                return getLoan(id);
            } catch (Exception e) {
                throw new RuntimeException("Blockchain createLoan failed: " + e.getMessage(), e);
            }
        }
        return createDemoLoan(request, callerAddress);
    }

    public List<LoanResponse> getLoansByAddress(String address) {
        List<BigInteger> ids;
        if (blockchainEnabled()) {
            try {
                Function function = new Function(
                        "getLoanIdsByBorrower",
                        List.of(new Address(address)),
                        List.of(new TypeReference<DynamicArray<Uint256>>() {})
                );
                List<Type> values = call(props.getContractAddress(), function);
                if (!values.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    DynamicArray<Uint256> arr = (DynamicArray<Uint256>) values.get(0);
                    ids = arr.getValue().stream()
                            .map(u -> (BigInteger) u.getValue())
                            .collect(java.util.stream.Collectors.toList());
                } else {
                    ids = List.of();
                }
            } catch (Exception e) {
                ids = loansByAddress.getOrDefault(address.toLowerCase(), List.of());
            }
        } else {
            ids = loansByAddress.getOrDefault(address.toLowerCase(), List.of());
        }
        return ids.stream()
                .map(id -> {
                    try { return getLoan(id); } catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    private void recordLoan(String address, BigInteger id) {
        loansByAddress.computeIfAbsent(address.toLowerCase(),
                k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(id);
    }

    public LoanResponse getLoan(BigInteger id) {
        if (blockchainEnabled()) {
            try {
                Function function = new Function(
                        "loans",
                        List.of(new Uint256(id)),
                        Arrays.asList(
                                new TypeReference<Address>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Uint256>() {}
                        )
                );
                List<Type> values = call(props.getContractAddress(), function);
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("Loan not found: " + id);
                }
                return toLoanResponse(id, values);
            } catch (Exception e) {
                throw new RuntimeException("Blockchain getLoan failed: " + e.getMessage(), e);
            }
        }
        return requireLoan(id);
    }

    public BigInteger calculateInterest(BigInteger id) {
        if (blockchainEnabled()) {
            try {
                return callUint(props.getContractAddress(), new Function(
                        "calculateInterest",
                        List.of(new Uint256(id)),
                        List.of(new TypeReference<Uint256>() {})
                ));
            } catch (Exception e) {
                throw new RuntimeException("Blockchain calculateInterest failed: " + e.getMessage(), e);
            }
        }
        LoanResponse loan = requireLoan(id);
        return loan.amount().multiply(loan.interestRate()).divide(BigInteger.valueOf(100));
    }

    public int assessRisk(BigInteger id) {
        if (blockchainEnabled()) {
            try {
                return callUint(props.getContractAddress(), new Function(
                        "assessLoanRisk",
                        List.of(new Uint256(id)),
                        List.of(new TypeReference<Uint256>() {})
                )).intValue();
            } catch (Exception e) {
                throw new RuntimeException("Blockchain assessRisk failed: " + e.getMessage(), e);
            }
        }
        LoanResponse loan = requireLoan(id);
        BigInteger collateralValue = loan.collateral().multiply(BigInteger.valueOf(2));
        BigInteger ltv = loan.amount().multiply(BigInteger.valueOf(100)).divide(collateralValue);
        if (ltv.compareTo(BigInteger.valueOf(50)) < 0) return 1;
        if (ltv.compareTo(BigInteger.valueOf(75)) < 0) return 2;
        return 3;
    }

    public LoanResponse repay(BigInteger id) {
        if (blockchainEnabled()) {
            try {
                sendTransaction(props.getContractAddress(), new Function("repayLoan", List.of(new Uint256(id)), List.of()));
                return getLoan(id);
            } catch (Exception e) {
                throw new RuntimeException("Blockchain repay failed: " + e.getMessage(), e);
            }
        }
        LoanResponse loan = requireLoan(id);
        LoanResponse updated = new LoanResponse(loan.id(), loan.borrower(), loan.amount(), loan.collateral(), loan.interestRate(), loan.durationDays(), "CLOSED", loan.startTime(), loan.endTime(), loan.repaymentAmount());
        demoStorage.put(id, updated);
        return updated;
    }

    public LoanResponse liquidate(BigInteger id) {
        if (blockchainEnabled()) {
            try {
                sendTransaction(props.getContractAddress(), new Function("liquidateLoan", List.of(new Uint256(id)), List.of()));
                return getLoan(id);
            } catch (Exception e) {
                throw new RuntimeException("Blockchain liquidate failed: " + e.getMessage(), e);
            }
        }
        LoanResponse loan = requireLoan(id);
        LoanResponse updated = new LoanResponse(loan.id(), loan.borrower(), loan.amount(), loan.collateral(), loan.interestRate(), loan.durationDays(), "LIQUIDATED", loan.startTime(), loan.endTime(), loan.repaymentAmount());
        demoStorage.put(id, updated);
        return updated;
    }

    public Map<String, BigInteger> getBalances(String address) {
        BigInteger walletBalance;
        if (!blockchainEnabled()) {
            walletBalance = BigInteger.valueOf(100000);
        } else {
            try {
                walletBalance = callUint(props.getCollateralTokenAddress(), new Function(
                        "balanceOf",
                        List.of(new Address(address)),
                        List.of(new TypeReference<Uint256>() {})
                ));
            } catch (Exception e) {
                throw new RuntimeException("Failed to get balance: " + e.getMessage(), e);
            }
        }

        // Считаем сумму залогов по активным займам пользователя
        List<BigInteger> ids = loansByAddress.getOrDefault(address.toLowerCase(), List.of());
        BigInteger lockedCollateral = BigInteger.ZERO;
        for (BigInteger id : ids) {
            try {
                LoanResponse loan = getLoan(id);
                if ("ACTIVE".equals(loan.status())) {
                    lockedCollateral = lockedCollateral.add(loan.collateral());
                }
            } catch (Exception ignored) {}
        }

        return Map.of(
                "walletBalance", walletBalance,
                "lockedCollateral", lockedCollateral
        );
    }

    public String mintTokens(String toAddress, BigInteger amount) {
        if (!blockchainEnabled()) throw new IllegalStateException("Blockchain mode is disabled");
        try {
            Function function = new Function(
                    "mint",
                    Arrays.asList(new Address(toAddress), new Uint256(amount)),
                    List.of()
            );
            return sendTransaction(props.getCollateralTokenAddress(), function);
        } catch (Exception e) {
            throw new RuntimeException("Mint failed: " + e.getMessage(), e);
        }
    }

    public String approveCollateral(BigInteger amount) {
        return approve(props.getCollateralTokenAddress(), amount);
    }

    public String approveLoanToken(BigInteger amount) {
        return approve(props.getLoanTokenAddress(), amount);
    }

    public Map<String, Object> status() {
        return Map.of(
                "blockchainEnabled", blockchainEnabled(),
                "rpcUrl", props.getRpcUrl() == null ? "" : props.getRpcUrl(),
                "contractAddress", props.getContractAddress() == null ? "" : props.getContractAddress(),
                "loanTokenAddress", props.getLoanTokenAddress() == null ? "" : props.getLoanTokenAddress(),
                "collateralTokenAddress", props.getCollateralTokenAddress() == null ? "" : props.getCollateralTokenAddress()
        );
    }

    private String approve(String tokenAddress, BigInteger amount) {
        if (!blockchainEnabled()) throw new IllegalStateException("Blockchain mode is disabled");
        if (tokenAddress == null || tokenAddress.isBlank() || ZERO_ADDRESS.equalsIgnoreCase(tokenAddress)) {
            throw new IllegalStateException("Token address is not configured");
        }
        try {
            Function function = new Function(
                    "approve",
                    Arrays.asList(new Address(props.getContractAddress()), new Uint256(amount)),
                    List.of(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
            );
            return sendTransaction(tokenAddress, function);
        } catch (Exception e) {
            throw new RuntimeException("Approve failed: " + e.getMessage(), e);
        }
    }

    private static final BigInteger INTEREST_RATE = BigInteger.valueOf(10);

    private LoanResponse createDemoLoan(CreateLoanRequest request, String callerAddress) {
        BigInteger id = BigInteger.valueOf(counter.incrementAndGet());
        BigInteger amount = request.collateral().multiply(BigInteger.valueOf(70)).divide(BigInteger.valueOf(100));
        BigInteger now = BigInteger.valueOf(Instant.now().getEpochSecond());
        BigInteger end = now.add(request.durationDays().multiply(BigInteger.valueOf(86400)));
        BigInteger repayment = amount.add(amount.multiply(INTEREST_RATE).divide(BigInteger.valueOf(100)));
        LoanResponse loan = new LoanResponse(id, callerAddress.toLowerCase(), amount, request.collateral(), INTEREST_RATE, request.durationDays(), "ACTIVE", now, end, repayment);
        demoStorage.put(id, loan);
        recordLoan(callerAddress, id);
        return loan;
    }

    private LoanResponse requireLoan(BigInteger id) {
        LoanResponse loan = demoStorage.get(id);
        if (loan == null) throw new IllegalArgumentException("Loan not found: " + id);
        return loan;
    }

    private Web3j web3j() {
        return Web3j.build(new HttpService(props.getRpcUrl()));
    }

    private Credentials credentials() {
        return Credentials.create(props.getPrivateKey());
    }

    private String sendTransaction(String to, Function function) throws Exception {
        Web3j web3j = web3j();
        try {
            TransactionManager txManager = new RawTransactionManager(web3j, credentials());
            String data = FunctionEncoder.encode(function);
            return txManager.sendTransaction(
                    DefaultGasProvider.GAS_PRICE,
                    BigInteger.valueOf(3_000_000),
                    to,
                    data,
                    BigInteger.ZERO
            ).getTransactionHash();
        } finally {
            web3j.shutdown();
        }
    }

    private List<Type> call(String to, Function function) throws Exception {
        Web3j web3j = web3j();
        try {
            String data = FunctionEncoder.encode(function);
            Transaction tx = Transaction.createEthCallTransaction(credentials().getAddress(), to, data);
            String value = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send().getValue();
            return FunctionReturnDecoder.decode(value, function.getOutputParameters());
        } finally {
            web3j.shutdown();
        }
    }

    private BigInteger callUint(String to, Function function) throws Exception {
        List<Type> values = call(to, function);
        if (values.isEmpty()) throw new IllegalStateException("Empty contract response");
        return (BigInteger) values.get(0).getValue();
    }

    private LoanResponse toLoanResponse(BigInteger id, List<Type> values) {
        String borrower = values.get(0).getValue().toString();
        BigInteger amount = (BigInteger) values.get(1).getValue();
        BigInteger collateral = (BigInteger) values.get(2).getValue();
        BigInteger interestRate = (BigInteger) values.get(3).getValue();
        BigInteger durationDays = (BigInteger) values.get(4).getValue();
        BigInteger statusCode = (BigInteger) values.get(5).getValue();
        BigInteger startTime = (BigInteger) values.get(6).getValue();
        BigInteger endTime = (BigInteger) values.get(7).getValue();
        BigInteger repaymentAmount = (BigInteger) values.get(8).getValue();
        return new LoanResponse(id, borrower, amount, collateral, interestRate, durationDays, statusName(statusCode), startTime, endTime, repaymentAmount);
    }

    private String statusName(BigInteger statusCode) {
        if (BigInteger.ZERO.equals(statusCode)) return "CLOSED";
        if (BigInteger.ONE.equals(statusCode)) return "ACTIVE";
        if (BigInteger.TWO.equals(statusCode)) return "LIQUIDATED";
        return "UNKNOWN";
    }
}
