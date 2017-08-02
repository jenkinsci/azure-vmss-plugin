/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetStorageProfile;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVMProfile;
import com.microsoft.azure.management.compute.VirtualMachineScaleSets;
import com.microsoft.azure.management.compute.implementation.ImageReferenceInner;
import com.microsoft.azure.management.compute.implementation.VirtualMachineScaleSetInner;
import com.microsoft.azure.management.compute.implementation.VirtualMachineScaleSetsInner;
import edu.emory.mathcs.backport.java.util.Arrays;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateInstancesBuilderTest {

    @Rule
    public TemporaryFolder workspace = new TemporaryFolder();

    private Azure mockAzure() {
        final VirtualMachineScaleSetsInner vmssMgrInner = mock(VirtualMachineScaleSetsInner.class);
        final VirtualMachineScaleSets vmssMgr = mock(VirtualMachineScaleSets.class);
        when(vmssMgr.inner()).thenReturn(vmssMgrInner);

        final Azure azure = mock(Azure.class);
        when(azure.virtualMachineScaleSets()).thenReturn(vmssMgr);

        return azure;
    }

    @Test
    public void perform() throws IOException, InterruptedException {
        final UpdateInstancesBuilder builder = new UpdateInstancesBuilder("cid", "rg", "name", "1,2,3");
        final Azure azure = mockAzure();
        builder.setAzureClientFactory(new BaseBuilder.AzureClientFactory() {
            @Override
            public Azure createAzureClient(String azureCredentialsId) {
                return azure;
            }
        });

        final Run run = mock(Run.class);
        final FilePath workspace = new FilePath(this.workspace.getRoot());
        final Launcher launcher = mock(Launcher.class);
        final TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(System.out);

        builder.perform(run, workspace, launcher, listener);

        final ArgumentCaptor<List<String>> instanceIdsArg = ArgumentCaptor.forClass(List.class);
        verify(azure.virtualMachineScaleSets().inner()).updateInstances(
                eq("rg"), eq("name"), instanceIdsArg.capture());
        Assert.assertEquals(Arrays.asList(new String[]{"1", "2", "3"}), instanceIdsArg.getValue());
    }
}
