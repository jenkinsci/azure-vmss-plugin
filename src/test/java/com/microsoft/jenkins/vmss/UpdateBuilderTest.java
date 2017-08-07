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
import hudson.EnvVars;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateBuilderTest {

    @Rule
    public TemporaryFolder workspace = new TemporaryFolder();

    private Azure mockAzure(final ImageReferenceInner currentImageReference) {
        final VirtualMachineScaleSetStorageProfile currentStorageProfile = mock(
                VirtualMachineScaleSetStorageProfile.class);
        when(currentStorageProfile.imageReference()).thenReturn(currentImageReference);

        final VirtualMachineScaleSetVMProfile vmProfile = mock(VirtualMachineScaleSetVMProfile.class);
        when(vmProfile.storageProfile()).thenReturn(currentStorageProfile);

        final VirtualMachineScaleSetInner vmssInner = mock(VirtualMachineScaleSetInner.class);
        when(vmssInner.virtualMachineProfile()).thenReturn(vmProfile);
        when(vmssInner.id()).thenReturn("test-vmss-inner-id");

        final VirtualMachineScaleSet vmss = mock(VirtualMachineScaleSet.class);
        when(vmss.storageProfile()).thenReturn(currentStorageProfile);
        when(vmss.inner()).thenReturn(vmssInner);

        final VirtualMachineScaleSetsInner vmssMgrInner = mock(VirtualMachineScaleSetsInner.class);
        final VirtualMachineScaleSets vmssMgr = mock(VirtualMachineScaleSets.class);
        when(vmssMgr.inner()).thenReturn(vmssMgrInner);
        when(vmssMgr.getByResourceGroup(anyString(), anyString())).thenReturn(vmss);

        final Azure azure = mock(Azure.class);
        when(azure.virtualMachineScaleSets()).thenReturn(vmssMgr);

        return azure;
    }

    @Test
    public void perform() throws IOException, InterruptedException {
        final ImageReference imageReference = spy(new ImageReference());
        imageReference.setId("id-new");

        final UpdateBuilder builder = new UpdateBuilder("cid", "rg", "name", imageReference);
        final ImageReferenceInner azureImageReference = mock(ImageReferenceInner.class);
        when(azureImageReference.id()).thenReturn("id-old");
        final Azure azure = mockAzure(azureImageReference);
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
        final EnvVars env = new EnvVars();
        when(run.getEnvironment(listener)).thenReturn(env);
        when(listener.getLogger()).thenReturn(System.out);

        builder.perform(run, workspace, launcher, listener);

        verify(azure.virtualMachineScaleSets()).getByResourceGroup("rg", "name");
        verify(imageReference).apply(azureImageReference, env);
        verify(azureImageReference).withId("id-new");

        final ArgumentCaptor<VirtualMachineScaleSetInner> updatedVMSSArg =
                ArgumentCaptor.forClass(VirtualMachineScaleSetInner.class);
        verify(azure.virtualMachineScaleSets().inner()).createOrUpdate(
                eq("rg"), eq("name"), updatedVMSSArg.capture());
        Assert.assertEquals("test-vmss-inner-id", updatedVMSSArg.getValue().id());
    }
}
