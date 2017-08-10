/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.implementation.ImageReferenceInner;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.vmss.util.AzureUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

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

        final Azure azure = getAzureClient();

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

        azure.virtualMachineScaleSets().inner().createOrUpdate(getResourceGroup(), getName(), vmss.inner());

        listener.getLogger().println(Messages.UpdateBuilder_PerformLogSuccess());
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
    public static class DescriptorImpl extends BaseBuilder.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return Messages.UpdateBuilder_DisplayName();
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

        @JavaScriptMethod
        public boolean isCustomImage(final String azureCredentialsId,
                                     final String resourceGroup,
                                     final String name) {
            if (StringUtils.isNotBlank(azureCredentialsId)
                    && StringUtils.isNotBlank(resourceGroup)
                    && StringUtils.isNotBlank(name)) {
                final Azure azure = AzureUtils.buildAzureClient(
                        AzureCredentials.getServicePrincipal(azureCredentialsId));
                final VirtualMachineScaleSet vmss = azure.virtualMachineScaleSets()
                        .getByResourceGroup(resourceGroup, name);
                if (vmss == null) {
                    return false;
                }
                return StringUtils.isNotBlank(vmss.storageProfile().imageReference().id());
            }
            return false;
        }
    }

}
