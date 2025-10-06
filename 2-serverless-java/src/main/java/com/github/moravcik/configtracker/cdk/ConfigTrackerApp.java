package com.github.moravcik.configtracker.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class ConfigTrackerApp {
    public static String resourcePrefix = "config-tracker-2-serverless-java";

    public static void main(final String[] args) {
        App app = new App();
        new ConfigTrackerStack(app, resourcePrefix + "-stack", StackProps.builder().build());
        app.synth();
    }
}

