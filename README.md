# DeFi Lending Platform

Учебный MVP DeFi lending-протокола.

Состав проекта:

- `contracts/` — Solidity smart contracts: `LendingProtocol.sol`, `MockERC20.sol`
- `backend/` — Java Spring Boot API, связь с контрактом через Web3j
- `frontend/` — простой HTML/CSS/JS интерфейс
- `scripts/deploy.js` — деплой локальных контрактов в Hardhat

## 1. Быстрый запуск backend в demo-mode

Demo-mode не требует блокчейна. Данные хранятся в памяти Java-приложения.

```powershell
cd backend
mvn spring-boot:run
```

Проверка:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/blockchain/status
```

Создать займ:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/loans" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"amount":100,"collateral":200,"interestRate":10,"durationDays":30}'
```

Получить займ:

```powershell
Invoke-RestMethod http://localhost:8080/api/loans/1
```

## 2. Запуск с реальным smart contract через Hardhat

Нужны:

- Node.js 20+
- JDK 17 или 21
- Maven

### Шаг 1. Установить зависимости контрактов

В корне проекта:

```powershell
npm install
```

### Шаг 2. Запустить локальный blockchain

Открой первый терминал:

```powershell
npx hardhat node
```

Не закрывай этот терминал.

### Шаг 3. Задеплоить контракты

Открой второй терминал в корне проекта:

```powershell
npm run deploy:local
```

Скрипт выведет значения:

```text
RPC_URL=http://127.0.0.1:8545
CONTRACT_ADDRESS=...
LOAN_TOKEN_ADDRESS=...
COLLATERAL_TOKEN_ADDRESS=...
PRIVATE_KEY=...
```

Также они сохранятся в `backend/.env.local.example`.

### Шаг 4. Запустить Spring Boot в blockchain-mode

В PowerShell во втором терминале вставь значения из deploy output:

```powershell
cd backend
$env:BLOCKCHAIN_ENABLED="true"
$env:RPC_URL="http://127.0.0.1:8545"
$env:CONTRACT_ADDRESS="ВСТАВЬ_CONTRACT_ADDRESS"
$env:LOAN_TOKEN_ADDRESS="ВСТАВЬ_LOAN_TOKEN_ADDRESS"
$env:COLLATERAL_TOKEN_ADDRESS="ВСТАВЬ_COLLATERAL_TOKEN_ADDRESS"
$env:PRIVATE_KEY="ВСТАВЬ_PRIVATE_KEY"
mvn spring-boot:run
```

Проверка режима:

```powershell
Invoke-RestMethod http://localhost:8080/api/blockchain/status
```

Должно быть:

```json
{
  "blockchainEnabled": true
}
```

## 3. Проверка через API

Создать займ в smart contract:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/loans" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"amount":100,"collateral":200,"interestRate":10,"durationDays":30}'
```

Получить займ:

```powershell
Invoke-RestMethod http://localhost:8080/api/loans/1
```

Посчитать проценты:

```powershell
Invoke-RestMethod http://localhost:8080/api/loans/1/interest
```

Оценить риск:

```powershell
Invoke-RestMethod http://localhost:8080/api/loans/1/risk
```

Погасить займ:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/loans/1/repay" -Method Post
```

После этого:

```powershell
Invoke-RestMethod http://localhost:8080/api/loans/1
```

Статус должен стать `CLOSED`.

## 4. Frontend

Открой файл:

```text
frontend/index.html
```

Backend должен быть запущен на:

```text
http://localhost:8080
```

Frontend вызывает API backend, а backend вызывает smart contract.

## 5. Что показывать на защите

1. Запустить `npx hardhat node`.
2. Выполнить `npm run deploy:local`.
3. Запустить Spring Boot с env-переменными.
4. Открыть `frontend/index.html`.
5. Нажать `Проверить статус` — показать `blockchainEnabled: true`.
6. Создать займ.
7. Получить займ по ID.
8. Показать risk и interest.
9. Погасить займ и показать статус `CLOSED`.

## 6. Важное замечание

Для учебного MVP backend сам подписывает транзакции private key локального borrower-аккаунта Hardhat. В production так делать нельзя: пользователь должен подписывать транзакции через wallet, например MetaMask.
