/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.vmss.util;

import com.microsoft.rest.LogLevel;

public final class Constants {

    private Constants() {
        // Hide
    }

    public static final String PLUGIN_NAME = "AzureJenkinsVMSS";

    public static final LogLevel DEFAULT_AZURE_SDK_LOGGING_LEVEL = LogLevel.NONE;

    // the first option for select element. Keep the same value as jenkins pre-defined default empty value.
    public static final String EMPTY_SELECTION = "- none -";

    public static final String AI_VMSS = "VMSS";
    public static final String AI_UPDATE_START = "UpdateStart";
    public static final String AI_UPDATE_SUCCESS = "UpdateSuccess";
    public static final String AI_UPDATE_FAILED = "UpdateFailed";
    public static final String AI_UPDATE_INSTANCES_START = "UpdateInstancesStart";
    public static final String AI_UPDATE_INSTANCES_SUCCESS = "UpdateInstancesSuccess";
    public static final String AI_UPDATE_INSTANCES_FAILED = "UpdateInstancesFailed";
}
