package com.banking.cucumber.steps;

import com.banking.dto.auth.LoginRequest;
import com.banking.dto.auth.LoginResponse;
import com.banking.dto.auth.RegisterRequest;
import com.banking.dto.user.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> lastResponse;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Given("a new customer wants to register with username {string}")
    public void aNewCustomerWantsToRegisterWithUsername(String username) {
        // stored in shared context via When step
    }

    @When("the customer registers with username {string} and password {string}")
    public void theCustomerRegistersWithUsernameAndPassword(String username, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setFirstName("Test");
        req.setLastName("User");
        req.setEmail(username + "@test.nl");
        req.setPhoneNumber("+31612345678");
        req.setBsn("123456780");
        req.setDateOfBirth(LocalDate.of(1990, 1, 1));

        lastResponse = restTemplate.postForEntity(
            baseUrl() + "/api/auth/register",
            req,
            String.class
        );
    }

    @Then("the registration response status should be {int}")
    public void theRegistrationResponseStatusShouldBe(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the registered user should not be approved")
    public void theRegisteredUserShouldNotBeApproved() throws Exception {
        UserDTO user = objectMapper.readValue(lastResponse.getBody(), UserDTO.class);
        assertThat(user.isApproved()).isFalse();
    }

    @When("the customer logs in with username {string} and password {string}")
    public void theCustomerLogsInWithUsernameAndPassword(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        lastResponse = restTemplate.postForEntity(
            baseUrl() + "/api/auth/login",
            req,
            String.class
        );
    }

    @Then("the login response status should be {int}")
    public void theLoginResponseStatusShouldBe(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the login response should contain a token")
    public void theLoginResponseShouldContainAToken() throws Exception {
        LoginResponse response = objectMapper.readValue(lastResponse.getBody(), LoginResponse.class);
        assertThat(response.getToken()).isNotBlank();
    }

    @When("someone tries to login with username {string} and wrong password {string}")
    public void someoneTryToLoginWithUsernameAndWrongPassword(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        lastResponse = restTemplate.postForEntity(
            baseUrl() + "/api/auth/login",
            req,
            String.class
        );
    }
}
