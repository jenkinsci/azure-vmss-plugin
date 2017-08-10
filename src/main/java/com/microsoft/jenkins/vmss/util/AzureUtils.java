/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.vmss.util;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.util.AzureCredentials;
import jenkins.model.Jenkins;

import java.util.HashMap;

public final class AzureUtils {

    private AzureUtils() {
        // Hide
    }

    static String getUserAgent() {
        String version = null;
        String instanceId = null;
        try {
            version = AzureUtils.class.getPackage().getImplementationVersion();
            Jenkins inst = Jenkins.getInstance();
            if (inst != null) {
                instanceId = inst.getLegacyInstanceId();
            }
        } catch (Exception e) {
        }

        if (version == null) {
            version = "local";
        }
        if (instanceId == null) {
            instanceId = "local";
        }

        return Constants.PLUGIN_NAME + "/" + version + "/" + instanceId;
    }

    static ApplicationTokenCredentials fromServicePrincipal(
            final AzureCredentials.ServicePrincipal servicePrincipal) {
        final AzureEnvironment env = new AzureEnvironment(new HashMap<String, String>() {
            {
                this.put(AzureEnvironment.Endpoint.MANAGEMENT.toString(),
                        servicePrincipal.getServiceManagementURL());
                this.put(AzureEnvironment.Endpoint.RESOURCE_MANAGER.toString(),
                        servicePrincipal.getResourceManagerEndpoint());
                this.put(AzureEnvironment.Endpoint.ACTIVE_DIRECTORY.toString(),
                        servicePrincipal.getAuthenticationEndpoint());
                this.put(AzureEnvironment.Endpoint.GRAPH.toString(),
                        servicePrincipal.getGraphEndpoint());
            }
        });
        return new ApplicationTokenCredentials(
                servicePrincipal.getClientId(),
                servicePrincipal.getTenant(),
                servicePrincipal.getClientSecret(),
                env
        );
    }

    public static Azure buildAzureClient(final AzureCredentials.ServicePrincipal servicePrincipal) {
        return Azure
                .configure()
                .withLogLevel(Constants.DEFAULT_AZURE_SDK_LOGGING_LEVEL)
                .withUserAgent(getUserAgent())
                .authenticate(fromServicePrincipal(servicePrincipal))
                .withSubscription(servicePrincipal.getSubscriptionId());
    }
}
