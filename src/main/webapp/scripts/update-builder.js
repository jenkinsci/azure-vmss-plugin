/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
(function() {
    Behaviour.specify('SELECT.select-azure-vmss-name', 'vmssImageType', 10000, function(nameEl) {
        var credentialsEl = findPreviousFormItem(nameEl, 'azureCredentialsId');
        var resourceGroupEl = findPreviousFormItem(nameEl, 'resourceGroup');
        var spinnerEl = findNext(nameEl, function(e) { return e.className === 'azure-vmss-spinner'; });

        // Reset resource group and name field if credentials changed
        if (credentialsEl) {
            credentialsEl.addEventListener('change', function() {
                resourceGroupEl.value = '';
                nameEl.value = '';
            });
        }

        // Reset name field if resource group changed
        if (resourceGroupEl) {
            resourceGroupEl.addEventListener('change', function() {
                nameEl.value = '';
            });
        }

        // Select appropriate image type according to VMSS profile
        function selectImageType() {
            var checkUrl = nameEl.getAttribute("data-check-url");
            var builderTBody = findAncestor(nameEl, 'TBODY');
            if (!builderTBody)
                return;

            var name = nameEl.value;
            var azureCredentialsId = credentialsEl ? credentialsEl.value : null;
            var resourceGroup = resourceGroupEl ? resourceGroupEl.value : null;

            var imageTypeOfficialEl = findNext(nameEl, function(e) { return e.tagName === 'INPUT' && e.value === 'official'; });
            var imageTypeCustomEl = findNext(nameEl, function(e) { return e.tagName === 'INPUT' && e.value === 'custom'; });

            if (name && resourceGroup && azureCredentialsId && checkUrl) {
                spinnerEl.style.display = '';

                var config = {
                    parameters: {
                        azureCredentialsId: azureCredentialsId,
                        resourceGroup: resourceGroup,
                        name: name
                    },
                    onSuccess: function(rsp) {
                        console.log(rsp);
                        spinnerEl.style.display = 'none';

                        var isCustomImage = rsp.responseText === "true";
                        var enabledEl = isCustomImage ? imageTypeCustomEl : imageTypeOfficialEl;
                        var disabledEl = !isCustomImage ? imageTypeCustomEl : imageTypeOfficialEl;

                        disabledEl.disabled = true;
                        enabledEl.disabled = false;
                        enabledEl.checked = true;

                        enabledEl.form.radios['imageType'].updateButtons();
                    },
                    onFailure: function(rsp) {
                        console.log(rsp);
                    }
                };
                new Ajax.Request(checkUrl, config);
            }
        }

        nameEl.addEventListener('change', function() {
            selectImageType();
        });

        // On load
        selectImageType();
    });
})();

