/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.vmss;

import com.microsoft.azure.management.compute.implementation.ImageReferenceInner;
import hudson.EnvVars;
import org.junit.Assert;
import org.junit.Test;

public class ImageReferenceTest {

    @Test
    public void customImage() {
        final ImageReferenceInner azureImageReference = new ImageReferenceInner();
        azureImageReference.withId("image-1");

        final ImageReference imageReference = new ImageReference();
        imageReference.setId("image-${id}");

        Assert.assertEquals("image-${id}", imageReference.getId());
        Assert.assertNull(imageReference.getPublisher());
        Assert.assertNull(imageReference.getOffer());
        Assert.assertNull(imageReference.getSku());
        Assert.assertNull(imageReference.getVersion());
        Assert.assertTrue(imageReference.isCustomImage());

        final EnvVars env = new EnvVars("id", "2");
        imageReference.apply(azureImageReference, env);

        Assert.assertEquals("image-2", azureImageReference.id());
        Assert.assertNull(azureImageReference.publisher());
        Assert.assertNull(azureImageReference.offer());
        Assert.assertNull(azureImageReference.sku());
        Assert.assertNull(azureImageReference.version());
    }

    @Test
    public void officialImage() {
        final ImageReferenceInner azureImageReference = new ImageReferenceInner();
        azureImageReference
                .withPublisher("Canonical")
                .withOffer("UbuntuServer")
                .withOffer("16.04-LTS")
                .withVersion("20170801");

        final ImageReference imageReference = new ImageReference();
        imageReference.setPublisher("MicrosoftWindowsServer");
        imageReference.setOffer("WindowsServer");
        imageReference.setSku("2012-R2-Datacenter");
        imageReference.setVersion("${version}");

        Assert.assertNull(imageReference.getId());
        Assert.assertEquals("MicrosoftWindowsServer", imageReference.getPublisher());
        Assert.assertEquals("WindowsServer", imageReference.getOffer());
        Assert.assertEquals("2012-R2-Datacenter", imageReference.getSku());
        Assert.assertEquals("${version}", imageReference.getVersion());
        Assert.assertFalse(imageReference.isCustomImage());

        final EnvVars env = new EnvVars("version", "latest");
        imageReference.apply(azureImageReference, env);

        Assert.assertNull(azureImageReference.id());
        Assert.assertEquals("MicrosoftWindowsServer", azureImageReference.publisher());
        Assert.assertEquals("WindowsServer", azureImageReference.offer());
        Assert.assertEquals("2012-R2-Datacenter", azureImageReference.sku());
        Assert.assertEquals("latest", azureImageReference.version());
    }
}
