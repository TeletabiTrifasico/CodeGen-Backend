Feature: Banking Transactions

  Background:
    Given I am logged in as "johndoe" with password "Password1!"

  Scenario: Customer deposits money via ATM
    When I deposit 500.0 EUR into account "NL02BANK1000000001"
    Then the transaction response status should be 201
    And the ATM response should have a reference number

  Scenario: Customer withdraws money via ATM
    When I withdraw 100.0 EUR from account "NL02BANK1000000001"
    Then the transaction response status should be 201
    And the ATM response should have a reference number

  Scenario: Customer transfers money to another customer
    When I transfer 50.0 EUR from "NL02BANK1000000001" to "NL02BANK2000000001"
    Then the transaction response status should be 201
    And the transfer response should have a reference number

  Scenario: Customer transfers money between their own accounts
    When I transfer 100.0 EUR from "NL02BANK1000000001" to "NL02BANK1000000002"
    Then the transaction response status should be 201
    And the transfer response should have a reference number

  Scenario: Customer views their transactions
    When I view my transactions
    Then the transaction response status should be 200
    And the transactions list should not be empty
