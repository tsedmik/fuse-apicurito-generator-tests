/*
 * Copyright (C) 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tsedmik.fuse_apicurito_generator_tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.json.JSONObject;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.lingala.zip4j.core.ZipFile;

public class Stepdefs {

	private String openAPIFile;
	private String projectName = "example";
	private String settingsFile;
	private Process process;
	private String openshiftURL;

	@Given("^\"([^\"]*)\" file for setting Maven to use non-public repositories$")
	public void file_for_setting_Maven_to_use_non_public_repositories(String arg1) throws Exception {
		settingsFile = arg1;
	}

	@When("^I execute Maven with goals \"([^\"]*)\"$")
	public void i_execute_Maven_with_goals(String arg1) throws Exception {
		process = syncExecuteMaven("target/" + projectName, settingsFile, arg1);
	}

	@Then("^The project is successfully built$")
	public void the_project_is_successfully_built() throws Exception {
		String log = Utils.getProcessOutPut(process, true);
		assertTrue("Something went wrong during the build of the project", log.contains("BUILD SUCCESS"));
	}

	@Then("^The project is running$")
	public void the_project_is_running() throws Exception {
		assertTrue("The application was not started properly\n",
				Utils.checkAndlogProcessOutput(process, "io.example.openapi.Application           : Started Application"));
	}

	@Then("^The file on 'http://localhost:(\\d+)/openapi\\.json' is accessible$")
	public void the_file_on_http_localhost_openapi_json_is_accessible(int arg1) throws Exception {
		InputStream in = new URL("http://localhost:8080/openapi.json").openStream();
		String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
		assertTrue(result.contains("\"swagger\" : \"2.0\""));
		process.destroy();
	}

	@Given("^OpenAPI file - \"([^\"]*)\"$")
	public void openapi_file(String arg1) throws Exception {
		openAPIFile = arg1;
	}

	@When("^I generate a project from the OpenAPI file$")
	public void i_create_a_project_from_the_OpenAPI_file() throws Exception {

		// check whether the OpenAPI file is set
		if (openAPIFile == null) {
			fail("OpenAPI file is not set. Please use 'Given OpenAPI file' step before.");
		}
		File openAPI = new File(System.getProperty("user.dir") + "/" + openAPIFile);
		if (!openAPI.exists()) {
			fail("OpenAPI file does not exists --> cannot proceed");
		}
		File generatorJar = new File(System.getProperty("user.dir") + "/target/fuse-apicurito-generator/target/fuse-apicurito-generator-1.0-SNAPSHOT.jar");
		if (!generatorJar.exists()) {
			fail("fuse-apicurito-generator is not built --> cannot proceed");
		}
		Process generatorProcess = Runtime.getRuntime().exec("java -jar " + generatorJar.getAbsolutePath());
		Utils.checkAndlogProcessOutput(generatorProcess, "com.redhat.fuse.apicurio.Application     : Started Application");
		File headersFile = new File(System.getProperty("user.dir") + "/src/test/resources/headers.txt");
		File unzipSource = new File(System.getProperty("user.dir") + "/target/" + projectName + ".zip");
		File unzipDestination = new File(System.getProperty("user.dir") + "/target/" + projectName);
		Process generateProject = Runtime.getRuntime().exec("curl -s -X POST -H @" + headersFile.getAbsolutePath() + " -d @" + openAPI.getAbsolutePath() + " http://localhost:8080/api/v1/generate/camel-project.zip -o " + unzipSource.getAbsolutePath());
		generateProject.waitFor();
		generatorProcess.destroy();
		ZipFile zipFile = new ZipFile(unzipSource);
		zipFile.extractAll(unzipDestination.getAbsolutePath());
	}

	@Then("^openapi\\.json file stashed in the generated project does not contain key - \"([^\"]*)\"$")
	public void openapi_json_file_stashed_in_the_generated_project_does_not_contain_key(String arg1) throws Exception {
		File openapi = new File (System.getProperty("user.dir") + "/target/" + projectName + "/src/main/resources/openapi.json");
		if (!openapi.exists()) {
			fail("OpenAPI file does not exists --> cannot proceed");
		}
		String jsonData = new String(Files.readAllBytes(Paths.get(openapi.getAbsolutePath())));
		JSONObject object = new JSONObject(jsonData);
		if (object.has(arg1)) {
			fail("openapi.json contains '" + arg1 + "' key");
		}
	}

	@Given("^An OpenShift instance running on the current machine$")
    public void a_running_OpenShift_instance_The_URL_can_be_found_in_system_variable() throws Exception {
		process = Runtime.getRuntime().exec("minishift ip");
		String ip = Utils.getProcessOutPut(process, false);
        openshiftURL = "https://" + ip + ":8443";
    }

    @Given("^I login to the OpenShift instance with `oc` as a \"([^\"]*)\" with password \"([^\"]*)\"$")
    public void i_login_to_the_OpenShift_instance_with_oc_as_a_with_password(String arg1, String arg2)
            throws Exception {
        i_execute_shell_command("oc login -u " + arg1 + " -p " + arg2 + " " + openshiftURL);
    }

    @Given("^I execute shell command - \"([^\"]*)\"$")
    public void i_execute_shell_command(String arg1) throws Exception {
		process = Runtime.getRuntime().exec(arg1);
        assertTrue("The command did not terminate normally", process.waitFor() == 0);
    }

    @Given("^I execute shell command - \"([^\"]*)\" \\(this could fail\\)$")
    public void i_execute_shell_command_this_could_fail(String arg1) throws Exception {
		process = Runtime.getRuntime().exec(arg1);
		process.waitFor();
	}

	@Then("^the project is successfully deployed and running on OpenShift$")
    public void the_project_is_successfully_deployed_and_running_on_OpenShift() throws Exception {
		assertTrue("The build process failed", Utils.checkAndlogProcessOutput(process, "BUILD SUCCESS"));
		boolean isStarted = false;
		for (int i = 0; i < 30; i++) {
			process = Runtime.getRuntime().exec("oc status");
			String result = Utils.getProcessOutPut(process, false);
			if (result.contains("deployed")) {
				isStarted = true;
				break;
			}
			Thread.sleep(5000);
		}
		assertTrue("The application is not deployed on OpenShift", isStarted);
	}

	@Then("^the project exposes a service with 3scale annotations$")
    public void the_project_exposes_a_service_with_3scale_annotations() throws Exception {
		process = Runtime.getRuntime().exec("oc get services -o yaml");
		String service = Utils.getProcessOutPut(process, false);
		assertTrue("3scale annotations are not present", service.contains("discovery.3scale.net/description-path: /openapi.json"));
		assertTrue("3scale annotations are not present", service.contains("discovery.3scale.net/discovery-version: v1"));
		assertTrue("3scale annotations are not present", service.contains("discovery.3scale.net/port: \"8080\""));
		assertTrue("3scale annotations are not present", service.contains("discovery.3scale.net/scheme: http"));
	}

	@Then("^Wait for \"([^\"]*)\" seconds$")
    public void wait_for_seconds(String arg1) throws Exception {
		Thread.sleep(Integer.parseInt(arg1) * 1000);
	}

	private Process syncExecuteMaven(String projectLocation, String settingsFile, String goals)
			throws IOException, InterruptedException {
		File projectDir = new File(System.getProperty("user.dir") + "/" + projectLocation);
		if (settingsFile != null) {
			File mavenSettings = new File(System.getProperty("user.dir") + "/" + settingsFile);
			return Runtime.getRuntime().exec(
					"mvn -f " + projectDir.getAbsolutePath() + " -s " + mavenSettings.getAbsolutePath() + " " + goals);
		} else {
			return Runtime.getRuntime().exec("mvn -f " + projectDir.getAbsolutePath() + " " + goals);
		}
	}
}
