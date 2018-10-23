Feature: Can be the generated project build and run?
  Users want a project (as an output of fuse-apicurito-generator) which can be built and run out-of-the-box

  Scenario: Build the project
    Given project generated via fuse-apicurito-generator - "target/example"
    And "src/test/resources/settings.xml" file for setting Maven to use non-public repositories
    When I execute Maven with goals "clean package"
    Then The project is successfully built

  Scenario: Run the project
    Given project generated via fuse-apicurito-generator - "target/example"
    And "src/test/resources/settings.xml" file for setting Maven to use non-public repositories
    When I execute Maven with goals "spring-boot:run"
    Then The project is running
    Then The file on 'http://localhost:8080/openapi.json' is accessible
    