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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Test;

public class BuildTest {

	private Logger log = Logger.getLogger(BuildTest.class.getName());

	@Test
	public void testMavenRun() throws IOException, InterruptedException {
		log.info("Building the project...");
		File projectDir = new File("target/example");
		File mavenSettings = new File("resources/settings.xml");
		Process process = Runtime.getRuntime().exec("mvn -f " + projectDir.getAbsolutePath() + " -s " + mavenSettings.getAbsolutePath() + " clean package");
		logProcessOutput(process);
		assertTrue("Something went wrong during the build of the project", process.waitFor() == 0);
		log.info("The project was built");

		log.info("Starting the application...");
		Process process2 = Runtime.getRuntime().exec("mvn -f " + projectDir.getAbsolutePath() + " -s " + mavenSettings.getAbsolutePath() + " spring-boot:run");
		assertTrue("The application was not started properly\n",
				checkAndlogProcessOutput(process2, "io.example.openapi.Application           : Started Application"));
		log.info("The application is started");

		log.info("Checking the application");
		InputStream in = new URL("http://localhost:8080/openapi.json").openStream();
		String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
		assertTrue(result.contains("\"swagger\": \"2.0\""));
		process2.destroy();
	}

	private void logProcessOutput(Process process) throws IOException {
		try (InputStreamReader is = new InputStreamReader(process.getInputStream());
				BufferedReader reader = new BufferedReader(is)) {
			String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = reader.readLine();
			}
		}
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
