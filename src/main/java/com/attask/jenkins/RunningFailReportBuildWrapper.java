package com.attask.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: brianmondido
 * Date: 8/31/12
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class RunningFailReportBuildWrapper extends BuildWrapper {

    private String startPattern;
    private String startGroupNumber;
    private String endPattern;
    private String endGroupNumber;

    @DataBoundConstructor
    public RunningFailReportBuildWrapper(String startPattern, String startGroupNumber, String endPattern, String endGroupNumber) {
        this.startPattern = startPattern;
        this.startGroupNumber = startGroupNumber;
        this.endPattern = endPattern;
        this.endGroupNumber = endGroupNumber;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        String externalizableId = build.getExternalizableId();

        RunningFailReportAction action = new RunningFailReportAction(externalizableId, startPattern, startGroupNumber, endPattern, endGroupNumber);
        build.addAction(action);
        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                build.getAction(RunningFailReportAction.class).tearDown();
                return true;
            }
        };
    }

    public String getStartPattern() {
        return startPattern;
    }

    public String getStartGroupNumber() {
        return startGroupNumber;
    }

    public String getEndPattern() {
        return endPattern;
    }

    public String getEndGroupNumber() {
        return endGroupNumber;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Running Failures Reporting";
        }
    }
}
