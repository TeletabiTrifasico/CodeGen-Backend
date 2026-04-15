Feature: Authentication

  Scenario: Customer registers successfully
    When the customer registers with username "newcustomer99" and password "Password1!"
    Then the registration response status should be 201
    And the registered user should not be approved

  Scenario: Customer logs in with valid credentials
    When the customer logs in with username "johndoe" and password "Password1!"
    Then the login response status should be 200
    And the login response should contain a token

  Scenario: Login fails with wrong password
    When someone tries to login with username "johndoe" and wrong password "WrongPassword"
    Then the login response status should be 401
