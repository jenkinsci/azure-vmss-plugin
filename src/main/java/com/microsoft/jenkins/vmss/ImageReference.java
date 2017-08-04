package com.microsoft.jenkins.vmss;

import com.microsoft.azure.management.compute.implementation.ImageReferenceInner;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ImageReference implements Describable<ImageReference> {
    private String id;
    private String offer;
    private String publisher;
    private String sku;
    private String version;

    @DataBoundConstructor
    public ImageReference() {
    }

    public String getId() {
        return id;
    }

    @DataBoundSetter
    public void setId(final String id) {
        this.id = id;
    }

    public String getOffer() {
        return offer;
    }

    @DataBoundSetter
    public void setOffer(final String offer) {
        this.offer = offer;
    }

    public String getPublisher() {
        return publisher;
    }

    @DataBoundSetter
    public void setPublisher(final String publisher) {
        this.publisher = publisher;
    }

    public String getSku() {
        return sku;
    }

    @DataBoundSetter
    public void setSku(final String sku) {
        this.sku = sku;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(final String version) {
        this.version = version;
    }

    public ImageReferenceInner apply(final ImageReferenceInner azureImageRef, final EnvVars env) {
        if (StringUtils.isNotBlank(azureImageRef.id())) {
            azureImageRef.withId(env.expand(id));
        } else {
            azureImageRef
                    .withPublisher(env.expand(Util.fixNull(publisher)))
                    .withOffer(env.expand(Util.fixNull(offer)))
                    .withSku(env.expand(Util.fixNull(sku)))
                    .withVersion(env.expand(Util.fixNull(version)));
        }

        return azureImageRef;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ImageReference> {

    }
}
