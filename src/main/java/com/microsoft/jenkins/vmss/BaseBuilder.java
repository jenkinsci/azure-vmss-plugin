/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.jenkins.vmss.util.AzureUtils;
import com.microsoft.jenkins.vmss.util.Constants;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;

public abstract class BaseBuilder extends Builder implements SimpleBuildStep {

    private transient AzureClientFactory azureClientFactory;
    private final String azureCredentialsId;
    private final String resourceGroup;
    private final String name;

    protected BaseBuilder(
            final String azureCredentialsId,
            final String resourceGroup,
            final String name) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.name = name;
    }

    public String getAzureCredentialsId() {
        return azureCredentialsId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getName() {
        return name;
    }

    @VisibleForTesting
    void setAzureClientFactory(final AzureClientFactory factory) {
        azureClientFactory = factory;
    }

    interface AzureClientFactory {
        Azure createAzureClient(Item owner, String credentialsId);

        AzureClientFactory DEFAULT = new AzureClientFactory() {
            @Override
            public Azure createAzureClient(Item owner, String credentialsId) {
                return AzureUtils.buildClient(owner, credentialsId);
            }
        };
    }

    protected Azure getAzureClient(Item owner) {
        if (azureClientFactory != null) {
            return azureClientFactory.createAzureClient(owner, getAzureCredentialsId());
        } else {
            return AzureClientFactory.DEFAULT.createAzureClient(owner, getAzureCredentialsId());
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    protected static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        protected ListBoxModel listAzureCredentialsIdItems(Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.includeEmptyValue();
            model.includeAs(ACL.SYSTEM, owner, AzureBaseCredentials.class);
            return model;
        }

        protected ListBoxModel listResourceGroupItems(Item owner,
                                                      String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                try {
                    for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                        model.add(rg.name());
                    }
                } catch (Exception ex) {
                    // If the credential selected is an MSI, and the MSI is not granted access to any of the resource
                    // groups, we will get exception on resource group listing with the message
                    //     "Parameter this.client.subscriptionId() is required and cannot be null"
                    // However, this message is a bit misleading. So we wrap the error message a bit and this will
                    // also cover other exceptions raised during the resource group listing.
                    model.add(Messages.BaseBuilder_FailedToLoadResourceGroups(ex.getMessage()), "");
                }
            }

            return model;
        }

        protected ListBoxModel listVMSSItems(Item owner, String azureCredentialsId, String resourceGroup) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                try {
                    final PagedList<VirtualMachineScaleSet> vmssList =
                            azureClient.virtualMachineScaleSets().listByResourceGroup(resourceGroup);
                    for (final VirtualMachineScaleSet vmss : vmssList) {
                        model.add(vmss.name());
                    }
                } catch (Exception ex) {
                    // If the credential previously configured is an MSI, and we revoked all the resource group access
                    // after we saved the configuration, we will get an exception when we open the configure page again,
                    // with the message:
                    //     "Parameter this.client.subscriptionId() is required and cannot be null"
                    // This is similar to resource group listing in #listResourceGroupItems and we wrap similarly.
                    model.add(Messages.BaseBuilder_FailedToLoadVMSSItems(ex.getMessage()), "");
                }
            }

            return model;
        }
    }

}
