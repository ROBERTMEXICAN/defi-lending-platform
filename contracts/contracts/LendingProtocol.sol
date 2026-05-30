// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface IERC20 {
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
    function transfer(address to, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

contract LendingProtocol {
    enum LoanStatus { Closed, Active, Liquidated }

    struct Loan {
        address borrower;
        uint256 amount;
        uint256 collateral;
        uint256 interestRate; // percent for whole duration, e.g. 10 = 10%
        uint256 durationDays;
        LoanStatus status;
        uint256 startTime;
        uint256 endTime;
        uint256 repaymentAmount;
    }

    IERC20 public immutable loanToken;
    IERC20 public immutable collateralToken;
    uint256 public loanCounter;
    address public owner;

    mapping(uint256 => Loan) public loans;

    event LoanCreated(uint256 indexed loanId, address indexed borrower, uint256 amount, uint256 collateral);
    event LoanRepaid(uint256 indexed loanId, address indexed borrower, uint256 repaymentAmount);
    event LoanLiquidated(uint256 indexed loanId, address indexed liquidator, uint256 collateral);
    event LoanExtended(uint256 indexed loanId, uint256 newEndTime);

    modifier onlyBorrower(uint256 loanId) {
        require(msg.sender == loans[loanId].borrower, "Only borrower");
        _;
    }

    constructor(address _loanToken, address _collateralToken) {
        require(_loanToken != address(0), "Invalid loan token");
        require(_collateralToken != address(0), "Invalid collateral token");
        loanToken = IERC20(_loanToken);
        collateralToken = IERC20(_collateralToken);
        owner = msg.sender;
    }

    function createLoan(
        uint256 amount,
        uint256 collateral,
        uint256 interestRate,
        uint256 durationDays
    ) external returns (uint256) {
        require(amount > 0, "Amount must be positive");
        require(collateral > 0, "Collateral must be positive");
        require(durationDays > 0, "Duration must be positive");

        require(collateralToken.transferFrom(msg.sender, address(this), collateral), "Collateral transfer failed");
        require(loanToken.transfer(msg.sender, amount), "Loan transfer failed");

        loanCounter++;
        uint256 loanId = loanCounter;
        uint256 repaymentAmount = amount + ((amount * interestRate) / 100);

        loans[loanId] = Loan({
            borrower: msg.sender,
            amount: amount,
            collateral: collateral,
            interestRate: interestRate,
            durationDays: durationDays,
            status: LoanStatus.Active,
            startTime: block.timestamp,
            endTime: block.timestamp + durationDays * 1 days,
            repaymentAmount: repaymentAmount
        });

        emit LoanCreated(loanId, msg.sender, amount, collateral);
        return loanId;
    }

    function getLoanDetails(uint256 loanId) external view returns (Loan memory) {
        return loans[loanId];
    }

    function repayLoan(uint256 loanId) external onlyBorrower(loanId) {
        Loan storage loan = loans[loanId];
        require(loan.status == LoanStatus.Active, "Loan is not active");
        require(block.timestamp <= loan.endTime, "Loan expired");

        require(loanToken.transferFrom(msg.sender, address(this), loan.repaymentAmount), "Repayment transfer failed");
        require(collateralToken.transfer(loan.borrower, loan.collateral), "Collateral return failed");

        loan.status = LoanStatus.Closed;
        emit LoanRepaid(loanId, msg.sender, loan.repaymentAmount);
    }

    function liquidateLoan(uint256 loanId) external {
        Loan storage loan = loans[loanId];
        require(loan.status == LoanStatus.Active, "Loan is not active");
        require(block.timestamp > loan.endTime, "Loan is not expired");

        require(collateralToken.transfer(msg.sender, loan.collateral), "Collateral transfer failed");
        loan.status = LoanStatus.Liquidated;

        emit LoanLiquidated(loanId, msg.sender, loan.collateral);
    }

    function extendLoanDuration(uint256 loanId, uint256 extensionDays) external onlyBorrower(loanId) {
        Loan storage loan = loans[loanId];
        require(loan.status == LoanStatus.Active, "Loan is not active");
        require(block.timestamp <= loan.endTime, "Loan expired");
        require(extensionDays > 0, "Extension must be positive");

        loan.endTime += extensionDays * 1 days;
        emit LoanExtended(loanId, loan.endTime);
    }

    function calculateInterest(uint256 loanId) public view returns (uint256) {
        Loan memory loan = loans[loanId];
        require(loan.status == LoanStatus.Active, "Loan is not active");

        uint256 totalDuration = loan.durationDays * 1 days;
        uint256 elapsed = block.timestamp > loan.endTime ? totalDuration : block.timestamp - loan.startTime;
        return (((loan.amount * loan.interestRate) / 100) * elapsed) / totalDuration;
    }

    function assessLoanRisk(uint256 loanId) public view returns (uint256) {
        Loan memory loan = loans[loanId];
        require(loan.status == LoanStatus.Active, "Loan is not active");

        uint256 collateralValue = loan.collateral * 2; // demo assumption
        uint256 ltv = (loan.amount * 100) / collateralValue;

        if (ltv < 50) return 1;      // low risk
        if (ltv < 75) return 2;      // medium risk
        return 3;                    // high risk
    }
}
