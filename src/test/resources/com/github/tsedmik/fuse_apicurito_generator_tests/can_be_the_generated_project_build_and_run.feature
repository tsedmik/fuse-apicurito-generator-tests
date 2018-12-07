@SmokeTests
Feature: Can be the generated project build and run?
  Users want a project (as an output of fuse-apicurito-generator) which can be built and run out-of-the-box

  Scenario: Build the project
    Given OpenAPI file - "src/test/resources/openapi-spec.json"
    When I generate a project from the OpenAPI file
    And I execute Maven with goals "clean package"
    Then The project is successfully built

  Scenario: Run the project
    Given OpenAPI file - "src/test/resources/openapi-spec.json"
    When I generate a project from the OpenAPI file
    And I execute Maven with goals "spring-boot:run"
    Then The project is running
    Then The file on 'http://localhost:8080/openapi.json' is accessible