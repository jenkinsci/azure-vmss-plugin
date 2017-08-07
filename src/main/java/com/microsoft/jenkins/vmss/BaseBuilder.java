/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.vmss.util.Constants;
import com.microsoft.jenkins.vmss.util.TokenCache;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;

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
        Azure createAzureClient(final String credentialsId);

        AzureClientFactory DEFAULT = new AzureClientFactory() {
            @Override
            public Azure createAzureClient(final String credentialsId) {
                return TokenCache.getInstance(AzureCredentials.getServicePrincipal(credentialsId))
                        .getAzureClient();
            }
        };
    }

    protected Azure getAzureClient() {
        if (azureClientFactory != null) {
            return azureClientFactory.createAzureClient(getAzureCredentialsId());
        } else {
            return AzureClientFactory.DEFAULT.createAzureClient(getAzureCredentialsId());
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

        protected ListBoxModel listAzureCredentialsIdItems(final Item owner) {
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(CredentialsProvider.lookupCredentials(
                            AzureCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()
                    ));
        }

        protected ListBoxModel listResourceGroupItems(final String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = TokenCache.getInstance(
                        AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                    model.add(rg.name());
                }
            }

            return model;
        }

        protected ListBoxModel listVMSSItems(final String azureCredentialsId, final String resourceGroup) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                final Azure azureClient = TokenCache.getInstance(
                        AzureCredentials.getServicePrincipal(azureCredentialsId)).getAzureClient();
                final PagedList<VirtualMachineScaleSet> vmssList =
                        azureClient.virtualMachineScaleSets().listByResourceGroup(resourceGroup);
                for (final VirtualMachineScaleSet vmss : vmssList) {
                    model.add(vmss.name());
                }
            }

            return model;
        }
    }

}
