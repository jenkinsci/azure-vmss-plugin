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
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.lang.reflect.Field;

public class AzureUtilsTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void getUserAgent() {
        final String userAgent = AzureUtils.getUserAgent();
        Assert.assertEquals(Constants.PLUGIN_NAME + "/local/" + j.jenkins.getLegacyInstanceId(), userAgent);
    }

    @Test
    public void fromServicePrincipal() throws NoSuchFieldException, IllegalAccessException {
        final AzureCredentials.ServicePrincipal sp = new AzureCredentials.ServicePrincipal(
                "sub",
                "client",
                "secret",
                "http://example.com/tenant/oauth2",
                "management",
                "auth",
                "rm",
                "graph"
        );
        final ApplicationTokenCredentials token = AzureUtils.fromServicePrincipal(sp);
        final AzureEnvironment env = token.environment();
        final Field secretField = token.getClass().getDeclaredField("secret");
        secretField.setAccessible(true);
        Assert.assertEquals("secret", (String)secretField.get(token));
        Assert.assertEquals("client", token.clientId());
        Assert.assertEquals("tenant", token.domain());
        Assert.assertEquals("management", env.managementEndpoint());
        Assert.assertEquals("auth", env.activeDirectoryEndpoint());
        Assert.assertEquals("rm", env.resourceManagerEndpoint());
        Assert.assertEquals("graph", env.graphEndpoint());
    }

    @Test
    public void buildAzureClient() {
        final AzureCredentials.ServicePrincipal sp = new AzureCredentials.ServicePrincipal(
                "sub",
                "client",
                "secret",
                "http://example.com/tenant/oauth2",
                "",
                "",
                "",
                ""
        );
        final Azure azure = AzureUtils.buildAzureClient(sp);
        Assert.assertEquals("sub", azure.subscriptionId());
    }
}
