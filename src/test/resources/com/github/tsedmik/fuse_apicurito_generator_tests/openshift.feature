@OpenShiftTests
Feature: fuse-apicurito-generator is working with an OpenShift
  This feature covers scenarios related to OpenShift.

  Prerequisites: The OpenShift instance is configured for deployment of Fuse projects.
  For more details see https://docs.engineering.redhat.com/display/fuseqe/How+to+deploy+a+project+generated+from+Apicurito+to+OpenShift


  Background: Prepare OpenShift instance
    Given An OpenShift instance running on the current machine
    And I login to the OpenShift instance with `oc` as a "developer" with password "developer"
    # delete old project for a fresh start
    And I execute shell command - "oc delete project test-camel --wait=true" (this could fail)
    # create a new OpenShift project
    And I execute shell command - "oc new-project test-camel"


  Scenario: Deploy the generated project to an OpenShift instance
    As a user, I want to deploy the generated project from fuse-apicurito-generator to an OpenShift
    instance simply with executing -- mvn clean fabric8:deploy (no additional settings is required) 

    Given OpenAPI file - "src/test/resources/address-book.json"
    And I generate a project from the OpenAPI file
    And I execute Maven with goals "fabric8:deploy"
    Then the project is successfully deployed and running on OpenShift
