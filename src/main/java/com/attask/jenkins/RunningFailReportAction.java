package com.attask.jenkins;

import hudson.model.*;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: brianmondido
 * Date: 8/31/12
 * Time: 1:23 PM
 */
public class RunningFailReportAction implements Action {
	private static final int DEFAULT_AGE = 1;
	private String buildId;
	private final String startPattern;
	private final int startGroupNumber;
	private final String endPattern;
	private final int endGroupNumber;

	private transient int lastLine = -1;
	private transient Reference<Map<String, String>> cachedMap;

	public RunningFailReportAction(String buildId, String startPattern, String startGroupNumber, String endPattern, String endGroupNumber) {
		this.buildId = buildId;
		this.startPattern = startPattern;
		this.startGroupNumber = Integer.parseInt(startGroupNumber);
		this.endPattern = endPattern;
		this.endGroupNumber = Integer.parseInt(endGroupNumber);
	}

	public Map<String, String> generateFailureReport() throws IOException {
		Run<?, ?> run = AbstractBuild.fromExternalizableId(buildId);

		StringBuilder stackTraceBuilder = new StringBuilder();

		String testName = null;
		Pattern startPattern = Pattern.compile(this.startPattern);
		Pattern endPattern = Pattern.compile(this.endPattern);

		int count = 0;
		Map<String, String> testOutput = null;
		if (cachedMap != null) {
			testOutput = cachedMap.get();
		}
		if (testOutput == null) {
			lastLine = -1;
			testOutput = new ConcurrentHashMap<String, String>();
			cachedMap = new SoftReference<Map<String, String>>(testOutput);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(run.getLogFile()), run.getCharset()));
		try {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				count++;
				if (lastLine >= 0 && count <= lastLine) {
					System.out.println("skipping: " + line);
					continue;
				}

				Matcher startMatcher = startPattern.matcher(line);
				if (testName == null && startMatcher.find()) {
					testName = startMatcher.group(startGroupNumber);
					continue;
				}

				Matcher endMatcher = endPattern.matcher(line);
				if (testName != null && endMatcher.find()) {
					String endOfTestName = endMatcher.group(endGroupNumber);
					if (!endOfTestName.equals(testName)) {
						System.err.println("This is a weird state we're in! We started in one test and finished in another");
						continue;
					}
					testOutput.put(testName, stackTraceBuilder.toString());
					stackTraceBuilder = new StringBuilder();
					testName = null;
					continue;
				}

				if (testName != null) {
					stackTraceBuilder.append(line).append("\n");
				}
			}
		} finally {
			lastLine = count;
			reader.close();
		}

		return testOutput;
	}

	public int findAge(String testName) {
		int age = DEFAULT_AGE;
		AbstractBuild build = (AbstractBuild) AbstractBuild.fromExternalizableId(buildId);
		AbstractProject project = build.getProject();
		Run lastStableBuild = project.getLastStableBuild();
		Run lastUnstableBuild = project.getLastUnstableBuild();
		if (lastUnstableBuild != null && (lastStableBuild == null || lastStableBuild.getNumber() < lastUnstableBuild.getNumber())) {
			TestResultAction action = lastUnstableBuild.getAction(TestResultAction.class);
			List<CaseResult> failedTests = action.getFailedTests();
			for (int i = 0; i < failedTests.size(); i++) {
				CaseResult caseResult = failedTests.get(i);
				String caseResultFullName = caseResult.getFullName();
				if (caseResultFullName.equals(testName.replace("#", "."))) {
					age = caseResult.getAge() + DEFAULT_AGE;
					i = failedTests.size();
				}
			}
		}
		return age;
	}

	public void tearDown() {
		try {
			Run build = AbstractBuild.fromExternalizableId(buildId);
			build.getActions().remove(this);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}

	}

	public String getBuildId() {
		return buildId;
	}

	public String getStartPattern() {
		return startPattern;
	}

	public int getStartGroupNumber() {
		return startGroupNumber;
	}

	public String getEndPattern() {
		return endPattern;
	}

	public int getEndGroupNumber() {
		return endGroupNumber;
	}

	public Run findBuild() {
		return Build.fromExternalizableId(getBuildId());
	}

	public String getIconFileName() {
		return "clipboard.png";
	}

	public String getDisplayName() {
		return "Test Result";
	}

	public String getUrlName() {
		return "testReport";
	}
}
