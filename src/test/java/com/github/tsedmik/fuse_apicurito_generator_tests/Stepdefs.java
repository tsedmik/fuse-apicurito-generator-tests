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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class Stepdefs {

	private String projectLocation;
	private String settingsFile;
	private Process process;

	@Given("^project generated via fuse-apicurito-generator - \"([^\"]*)\"$")
	public void project_generated_via_fuse_apicurito_generator(String arg1) throws Exception {
	    projectLocation = arg1;
	}

	@Given("^\"([^\"]*)\" file for setting Maven to use non-public repositories$")
	public void file_for_setting_Maven_to_use_non_public_repositories(String arg1) throws Exception {
	    settingsFile = arg1;
	}

	@When("^I execute Maven with goals \"([^\"]*)\"$")
	public void i_execute_Maven_with_goals(String arg1) throws Exception {
		process = syncExecuteMaven(projectLocation, settingsFile, arg1);
	}

	@Then("^The project is successfully built$")
	public void the_project_is_successfully_built() throws Exception {
		String log = getAndLogProcessOutPut(process);
		assertTrue("Something went wrong during the build of the project", log.contains("BUILD SUCCESS"));
	}

	@Then("^The project is running$")
	public void the_project_is_running() throws Exception {
		assertTrue("The application was not started properly\n", checkAndlogProcessOutput(process, "io.example.openapi.Application           : Started Application"));
	}

	@Then("^The file on 'http://localhost:(\\d+)/openapi\\.json' is accessible$")
	public void the_file_on_http_localhost_openapi_json_is_accessible(int arg1) throws Exception {
		InputStream in = new URL("http://localhost:8080/openapi.json").openStream();
		String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
		assertTrue(result.contains("\"swagger\": \"2.0\""));
		process.destroy();
	}

	private Process syncExecuteMaven(String projectLocation, String settingsFile, String goals) throws IOException, InterruptedException {
		File projectDir = new File(projectLocation);
		File mavenSettings = new File(settingsFile);
		return Runtime.getRuntime().exec("mvn -f " + projectDir.getAbsolutePath() + " -s " + mavenSettings.getAbsolutePath() + " " + goals);
	}
	private String getAndLogProcessOutPut(Process process) throws IOException {
		StringBuilder builder = new StringBuilder();
		try (InputStreamReader is = new InputStreamReader(process.getInputStream());
				BufferedReader reader = new BufferedReader(is)) {
			String line = reader.readLine();
			while (line != null) {
				builder.append(line);
				System.out.println(line);
				line = reader.readLine();
			}
		}
		return builder.toString();
	}

	private boolean checkAndlogProcessOutput(Process process, String checkText) throws IOException {
		boolean isConditionMet = false;
		try (InputStreamReader is = new InputStreamReader(process.getInputStream());
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line = reader.readLine();
			while (line != null && !isConditionMet) {
				System.out.println(line);
				if (line.contains(checkText)) {
					isConditionMet = true;
					break;
				}
				line = reader.readLine();
			}
		}
		return isConditionMet;
	}
}
