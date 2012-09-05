package com.attask.jenkins;

import hudson.model.*;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: brianmondido
 * Date: 8/31/12
 * Time: 1:23 PM
 * To change this template use File | Settings | File Templates.
 */

public class RunningFailReportAction implements Action {

    private static final int DEFAULT_AGE=1;
    private String buildId;
    private final String startPattern;
    private final int startGroupNumber;
    private final String endPattern;
    private final int endGroupNumber;

    public RunningFailReportAction(String buildId, String startPattern, String startGroupNumber, String endPattern, String endGroupNumber){
        this.buildId=buildId;
        this.startPattern = startPattern;
        this.startGroupNumber = Integer.parseInt(startGroupNumber);
        this.endPattern = endPattern;
        this.endGroupNumber = Integer.parseInt(endGroupNumber);
    }

    public Map<String, String> generateFailureReport() throws IOException {
        Map<String, String> testOutput=new HashMap<String, String>();
        Run<?,?> run = AbstractBuild.fromExternalizableId(buildId);

        List<String> log = run.getLog(Integer.MAX_VALUE);
        StringBuilder stackTraceBuilder=new StringBuilder();

        String testName = null;
        Pattern startPattern = Pattern.compile(this.startPattern);
        Pattern endPattern = Pattern.compile(this.endPattern);
        for (String line : log) {

            Matcher startMatcher = startPattern.matcher(line);
            if(testName == null && startMatcher.find()) {
                testName = startMatcher.group(startGroupNumber);
                continue;
            }

            Matcher endMatcher = endPattern.matcher(line);
            if(testName != null && endMatcher.find()){
                String endOfTestName = endMatcher.group(endGroupNumber);
                if(!endOfTestName.equals(testName)) {
                    System.err.println("This is a weird state we're in! We started in one test and finished in another");
                    continue;
                }
                testOutput.put(testName, stackTraceBuilder.toString());
                stackTraceBuilder=new StringBuilder();
                testName = null;
                continue;
            }

            if(testName != null){
                stackTraceBuilder.append(line).append("\n");
            }
        }
        return testOutput;
    }

    public int findAge(String testName){
        int age=DEFAULT_AGE;
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
                if (caseResultFullName.equals(testName)) {
                    age = caseResult.getAge() + DEFAULT_AGE;
                    i = failedTests.size();
                }
            }
        }
        return age;
    }

    public void tearDown(){
        try{
            Run build = AbstractBuild.fromExternalizableId(buildId);
            build.getActions().remove(this);
        } catch (IllegalArgumentException e){
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

    public Run findBuild(){
        return Build.fromExternalizableId(getBuildId());
    }

    public String getIconFileName() {
        return "/plugin/runningtestfailurereport/epic.jpg";
    }

    public String getDisplayName() {
        return "Running Failure Report";
    }

    public String getUrlName() {
        return "testReport";
    }
}
