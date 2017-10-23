/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.microsoft.azure.management.Azure;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.vmss.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class UpdateInstancesBuilder extends BaseBuilder {

    private final String instanceIds;

    @DataBoundConstructor
    public UpdateInstancesBuilder(
            final String azureCredentialsId,
            final String resourceGroup,
            final String name,
            final String instanceIds) {
        super(azureCredentialsId, resourceGroup, name);
        this.instanceIds = instanceIds;
    }

    public String getInstanceIds() {
        return instanceIds;
    }

    @Override
    public void perform(@Nonnull final Run<?, ?> run,
                        @Nonnull final FilePath workspace,
                        @Nonnull final Launcher launcher,
                        @Nonnull final TaskListener listener) throws InterruptedException, IOException {

        listener.getLogger().println(Messages.UpdateInstancesBuilder_PerformLogStart());

        final String resolvedInstanceIds = run.getEnvironment(listener).expand(instanceIds);

        listener.getLogger().println(Messages.UpdateInstancesBuilder_PerformLogInstanceIDs(
                resolvedInstanceIds.toString()));

        final Azure azure = getAzureClient();
        final List<String> instanceIdsList = parseInstanceIds(resolvedInstanceIds);

        AzureVMSSPlugin.sendEvent(Constants.AI_VMSS, Constants.AI_UPDATE_INSTANCES_START,
                "Run", AppInsightsUtils.hash(run.getUrl()),
                "Subscription", AppInsightsUtils.hash(azure.subscriptionId()),
                "ResourceGroup", AppInsightsUtils.hash(getResourceGroup()),
                "Name", AppInsightsUtils.hash(getName()),
                "InstanceCount", String.valueOf(instanceIdsList.size()));

        azure.virtualMachineScaleSets().inner().updateInstances(
                getResourceGroup(), getName(), instanceIdsList);

        listener.getLogger().println(Messages.UpdateInstancesBuilder_PerformLogSuccess());
    }

    static List<String> parseInstanceIds(final String instanceIdsText) {
        return Arrays.asList(instanceIdsText.split(","));
    }

    @Extension
    @Symbol("azureVMSSUpdateInstances")
    public static class DescriptorImpl extends BaseBuilder.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return Messages.UpdateInstancesBuilder_DisplayName();
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath final Item owner) {
            return listAzureCredentialsIdItems(owner);
        }

        public ListBoxModel doFillResourceGroupItems(@QueryParameter final String azureCredentialsId) {
            return listResourceGroupItems(azureCredentialsId);
        }

        public ListBoxModel doFillNameItems(@QueryParameter final String azureCredentialsId,
                                            @QueryParameter final String resourceGroup) {
            return listVMSSItems(azureCredentialsId, resourceGroup);
        }
    }
}
