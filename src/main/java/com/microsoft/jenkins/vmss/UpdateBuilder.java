/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.implementation.ImageReferenceInner;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.vmss.util.AzureUtils;
import com.microsoft.jenkins.vmss.util.Constants;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class UpdateBuilder extends BaseBuilder {

    private final ImageReference imageReference;

    @DataBoundConstructor
    public UpdateBuilder(
            final String azureCredentialsId,
            final String resourceGroup,
            final String name,
            final ImageReference imageReference) {
        super(azureCredentialsId, resourceGroup, name);
        this.imageReference = imageReference;
    }

    public ImageReference getImageReference() {
        return imageReference;
    }

    @Override
    public void perform(@Nonnull final Run<?, ?> run,
                        @Nonnull final FilePath workspace,
                        @Nonnull final Launcher launcher,
                        @Nonnull final TaskListener listener) throws InterruptedException, IOException {

        listener.getLogger().println(Messages.UpdateBuilder_PerformLogStart());

        final Azure azure = getAzureClient(run.getParent());

        AzureVMSSPlugin.sendEvent(Constants.AI_VMSS, Constants.AI_UPDATE_START,
                "Run", AppInsightsUtils.hash(run.getUrl()),
                "Subscription", AppInsightsUtils.hash(azure.subscriptionId()),
                "ResourceGroup", AppInsightsUtils.hash(getResourceGroup()),
                "Name", AppInsightsUtils.hash(getName()));

        final VirtualMachineScaleSet vmss = azure.virtualMachineScaleSets().getByResourceGroup(
                getResourceGroup(), getName());
        if (vmss == null) {
            listener.getLogger().println(Messages.UpdateBuilder_VMSSNotFound(getName()));
            run.setResult(Result.FAILURE);
            return;
        }

        final ImageReferenceInner azureImageRef = vmss.storageProfile().imageReference();
        listener.getLogger().println(
                Messages.UpdateBuilder_PerformLogCurrentImageReference(printImageReference(azureImageRef)));

        imageReference.apply(azureImageRef, run.getEnvironment(listener));

        listener.getLogger().println(
                Messages.UpdateBuilder_PerformLogNewImageReference(printImageReference(azureImageRef)));

        try {
            azure.virtualMachineScaleSets().inner().createOrUpdate(getResourceGroup(), getName(), vmss.inner());

            listener.getLogger().println(Messages.UpdateBuilder_PerformLogSuccess());

            AzureVMSSPlugin.sendEvent(Constants.AI_VMSS, Constants.AI_UPDATE_SUCCESS,
                    "Run", AppInsightsUtils.hash(run.getUrl()),
                    "Subscription", AppInsightsUtils.hash(azure.subscriptionId()),
                    "ResourceGroup", AppInsightsUtils.hash(getResourceGroup()),
                    "Name", AppInsightsUtils.hash(getName()));
        } catch (CloudException ex) {
            ex.printStackTrace(listener.getLogger());
            run.setResult(Result.FAILURE);

            AzureVMSSPlugin.sendEvent(Constants.AI_VMSS, Constants.AI_UPDATE_FAILED,
                    "Run", AppInsightsUtils.hash(run.getUrl()),
                    "Subscription", AppInsightsUtils.hash(azure.subscriptionId()),
                    "ResourceGroup", AppInsightsUtils.hash(getResourceGroup()),
                    "Name", AppInsightsUtils.hash(getName()),
                    "Message", ex.getMessage());
        }
    }

    private String printImageReference(final ImageReferenceInner image) {
        return Messages.UpdateBuilder_PrintImageReference(
                image.id(),
                image.publisher(),
                image.offer(),
                image.sku(),
                image.version());
    }

    @Extension
    @Symbol("azureVMSSUpdate")
    public static class DescriptorImpl extends BaseBuilder.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return Messages.UpdateBuilder_DisplayName();
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath Item owner) {
            return listAzureCredentialsIdItems(owner);
        }

        public ListBoxModel doFillResourceGroupItems(@AncestorInPath Item owner,
                                                     @QueryParameter String azureCredentialsId) {
            return listResourceGroupItems(owner, azureCredentialsId);
        }

        public ListBoxModel doFillNameItems(@AncestorInPath Item owner,
                                            @QueryParameter String azureCredentialsId,
                                            @QueryParameter String resourceGroup) {
            return listVMSSItems(owner, azureCredentialsId, resourceGroup);
        }

        public String doIsCustomImage(@AncestorInPath Item owner,
                                      @QueryParameter String azureCredentialsId,
                                      @QueryParameter String resourceGroup,
                                      @QueryParameter String name) {
            if (StringUtils.isNotBlank(azureCredentialsId)
                    && StringUtils.isNotBlank(resourceGroup)
                    && StringUtils.isNotBlank(name)) {
                final Azure azure = AzureUtils.buildClient(owner, azureCredentialsId);
                final VirtualMachineScaleSet vmss = azure.virtualMachineScaleSets()
                        .getByResourceGroup(resourceGroup, name);
                if (vmss == null) {
                    return "false";
                }
                return String.valueOf(StringUtils.isNotBlank(vmss.storageProfile().imageReference().id()));
            }
            return "false";
        }
    }

}
