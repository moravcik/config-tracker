package com.github.moravcik.configtracker.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnElement;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.NestedStackProps;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigTrackerStack extends Stack {
    public ConfigTrackerStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ConfigTrackerStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        StorageNestedStack storageStack = new StorageNestedStack(this, "Storage");
        new ApiNestedStack(this, "Api", storageStack.getConfigTable());
        new NotificationsNestedStack(this, "Notifications", storageStack.getConfigChangesTopic());
    }

    @Override
    public @NotNull String getLogicalId(@NotNull CfnElement element) {
        if (element.getNode().getId().contains("NestedStackResource")) {
            Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)\\.NestedStackResource");
            Matcher matcher = pattern.matcher(element.getNode().getId());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return super.getLogicalId(element);
    }
}
