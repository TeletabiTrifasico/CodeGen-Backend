package com.banking.cucumber.steps;

import com.banking.dto.atm.AtmRequest;
import com.banking.dto.atm.AtmResponse;
import com.banking.dto.auth.LoginRequest;
import com.banking.dto.auth.LoginResponse;
import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private ResponseEntity<String> lastResponse;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Given("I am logged in as {string} with password {string}")
    public void iAmLoggedInAs(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/auth/login", req, String.class
        );
        LoginResponse loginResponse = objectMapper.readValue(response.getBody(), LoginResponse.class);
        authToken = loginResponse.getToken();
    }

    @When("I deposit {double} EUR into account {string}")
    public void iDepositIntoAccount(Double amount, String iban) {
        AtmRequest req = new AtmRequest();
        req.setIban(iban);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setDescription("Test deposit");

        lastResponse = restTemplate.exchange(
            baseUrl() + "/api/atm/deposit",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            String.class
        );
    }

    @When("I withdraw {double} EUR from account {string}")
    public void iWithdrawFromAccount(Double amount, String iban) {
        AtmRequest req = new AtmRequest();
        req.setIban(iban);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setDescription("Test withdrawal");

        lastResponse = restTemplate.exchange(
            baseUrl() + "/api/atm/withdraw",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            String.class
        );
    }

    @When("I transfer {double} EUR from {string} to {string}")
    public void iTransferFromTo(Double amount, String fromIban, String toIban) {
        TransferRequest req = new TransferRequest();
        req.setFromIban(fromIban);
        req.setToIban(toIban);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setDescription("Test transfer");

        lastResponse = restTemplate.exchange(
            baseUrl() + "/api/transactions/transfer",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            String.class
        );
    }

    @Then("the transaction response status should be {int}")
    public void theTransactionResponseStatusShouldBe(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the ATM response should have a reference number")
    public void theAtmResponseShouldHaveAReferenceNumber() throws Exception {
        AtmResponse response = objectMapper.readValue(lastResponse.getBody(), AtmResponse.class);
        assertThat(response.getReference()).isNotBlank();
    }

    @Then("the transfer response should have a reference number")
    public void theTransferResponseShouldHaveAReferenceNumber() throws Exception {
        TransactionDTO dto = objectMapper.readValue(lastResponse.getBody(), TransactionDTO.class);
        assertThat(dto.getReference()).isNotBlank();
    }

    @When("I view my transactions")
    public void iViewMyTransactions() {
        lastResponse = restTemplate.exchange(
            baseUrl() + "/api/transactions",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class
        );
    }

    @Then("the transactions list should not be empty")
    public void theTransactionsListShouldNotBeEmpty() throws Exception {
        TransactionDTO[] txs = objectMapper.readValue(lastResponse.getBody(), TransactionDTO[].class);
        assertThat(txs).isNotEmpty();
    }
}
