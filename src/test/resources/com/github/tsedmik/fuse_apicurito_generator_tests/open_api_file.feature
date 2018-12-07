@SmokeTests
Feature: OpenAPI file
  This feature covers scenarios related to OpenAPI file and its transformations

  Scenario: Remove host/scheme keys of the generated OpenAPI specification stored in the project

    As a user, I want to remove host/schemes keys from the openapi.json file stored in my project,
    although they are in the original OpenAPI specification file.
    For more details see: https://github.com/jboss-fuse/fuse-apicurito-generator/issues/19

    Given OpenAPI file - "src/test/resources/host-scheme.json"
    When I generate a project from the OpenAPI file
    Then openapi.json file stashed in the generated project does not contain key - "host"
    And openapi.json file stashed in the generated project does not contain key - "schemes"
