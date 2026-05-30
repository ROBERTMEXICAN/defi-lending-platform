const API = "http://localhost:8080/api";
const output = document.getElementById("output");

function show(data) {
  output.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

async function request(path, options = {}) {
  const response = await fetch(`${API}${path}`, options);
  const text = await response.text();
  let body;
  try { body = text ? JSON.parse(text) : {}; } catch { body = text; }
  if (!response.ok) {
    throw new Error(typeof body === "string" ? body : JSON.stringify(body, null, 2));
  }
  return body;
}

document.getElementById("getStatus").addEventListener("click", async () => {
  try { show(await request("/blockchain/status")); }
  catch (e) { show({ error: e.message }); }
});

document.getElementById("createLoan").addEventListener("click", async () => {
  try {
    const body = {
      amount: document.getElementById("amount").value,
      collateral: document.getElementById("collateral").value,
      interestRate: document.getElementById("interestRate").value,
      durationDays: document.getElementById("durationDays").value
    };
    show(await request("/loans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    }));
  } catch (e) { show({ error: e.message }); }
});

document.getElementById("getLoan").addEventListener("click", async () => {
  try { show(await request(`/loans/${document.getElementById("loanId").value}`)); }
  catch (e) { show({ error: e.message }); }
});

document.getElementById("repayLoan").addEventListener("click", async () => {
  try { show(await request(`/loans/${document.getElementById("loanId").value}/repay`, { method: "POST" })); }
  catch (e) { show({ error: e.message }); }
});

document.getElementById("liquidateLoan").addEventListener("click", async () => {
  try { show(await request(`/loans/${document.getElementById("loanId").value}/liquidate`, { method: "POST" })); }
  catch (e) { show({ error: e.message }); }
});

document.getElementById("getInterest").addEventListener("click", async () => {
  try { show(await request(`/loans/${document.getElementById("loanId").value}/interest`)); }
  catch (e) { show({ error: e.message }); }
});

document.getElementById("getRisk").addEventListener("click", async () => {
  try { show(await request(`/loans/${document.getElementById("loanId").value}/risk`)); }
  catch (e) { show({ error: e.message }); }
});
