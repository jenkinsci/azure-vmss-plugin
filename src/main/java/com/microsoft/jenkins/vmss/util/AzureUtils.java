/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.vmss.util;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.azure.util.AzureCredentialUtil;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.azurecommons.core.credentials.TokenCredentialData;
import com.microsoft.jenkins.vmss.AzureVMSSPlugin;
import hudson.model.Item;

public final class AzureUtils {

    private AzureUtils() {
        // Hide
    }

    static TokenCredentialData getToken(Item owner, String credentialsId) {
        AzureBaseCredentials credential = AzureCredentialUtil.getCredential(owner, credentialsId);
        if (credential == null) {
            throw new IllegalStateException(
                    String.format("Can't find credential in scope %s with id: %s", owner, credentialsId));
        }
        return TokenCredentialData.deserialize(credential.serializeToTokenData());
    }

    public static Azure buildClient(Item owner, String credentialsId) {
        TokenCredentialData token = getToken(owner, credentialsId);
        return buildClient(token);
    }

    public static Azure buildClient(TokenCredentialData token) {
        return AzureClientFactory.getClient(token, new AzureClientFactory.Configurer() {
            @Override
            public Azure.Configurable configure(Azure.Configurable configurable) {
                return configurable
                        .withLogLevel(Constants.DEFAULT_AZURE_SDK_LOGGING_LEVEL)
                        .withInterceptor(new AzureVMSSPlugin.AzureTelemetryInterceptor())
                        .withUserAgent(AzureClientFactory.getUserAgent(
                                Constants.PLUGIN_NAME, AzureUtils.class.getPackage().getImplementationVersion()));
            }
        });
    }
}
