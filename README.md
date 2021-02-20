# Azure Virtual Machine Scale Set Plugin

> ***Important***: This plugin is being retired and will be out of support as of February 29, 2024. Azure CLI is the currently recommended way to integrate Jenkins with Azure services.

## Using Credentials Binding and Az CLI

[Credentials Binding](https://plugins.jenkins.io/credentials-binding/) and Az CLI is the recommended way to integrate with Azure services.

1. Make sure you have [Az CLI installed](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli), version 2.0.67 or higher.
2. Create a service principal using Az CLI:

    ```bash
        az ad sp create-for-rbac
    ```

3. Make sure the [Credentials plugin](https://plugins.jenkins.io/credentials/) is installed and add a credential in Jenkins Credentials page.

   Ensure that the credential kind is ***Username with password*** and enter the following items:
    * Username - The ***appId*** of the service principal created.
    * Password - The ***password*** of the service principal created.
    * ID - Credential identifier such as AzureServicePrincipal

   Sample Jenkinsfile (declarative pipeline)

    ```groovy
    pipeline {
        agent any

        environment {
            AZURE_SUBSCRIPTION_ID='99999999-9999-9999-9999-999999999999'
            AZURE_TENANT_ID='99999999-9999-9999-9999-999999999999'
        }

        stages {
            stage('Example') {
                steps {
                       withCredentials([usernamePassword(credentialsId: 'myAzureCredential', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
                                sh 'az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $AZURE_TENANT_ID'
                                sh 'az account set -s $AZURE_SUBSCRIPTION_ID'
                                sh 'az vmss update --resource-group <myVMSSResourceGroup> --name <myVmScaleSet> --set virtualMachineProfile.storageProfile.imageReference.id=/subscriptions/$AZURE_SUBSCRIPTION_ID>/resourceGroups/<myVMSSResourceGroup>/providers/Microsoft.Compute/galleries/<myVMSSGallery>/images/MyImage/versions/0.0.<BuildId>'
                            }
                }
            }
        }
    }
    ```

---

A Jenkins plugin to deploy VM images to Azure Virtual Machine Scale Sets (VMSS).

## How to Install

You can install/update this plugin in Jenkins update center (Manage Jenkins -> Manage Plugins, search Azure Virtual Machine Scale Set Plugin).

You can also manually install the plugin if you want to try the latest feature before it's officially released.
To manually install the plugin:

1. Clone the repo and build:
   ```
   mvn package
   ```
2. Open your Jenkins dashboard, go to Manage Jenkins -> Manage Plugins.
3. Go to Advanced tab, under Upload Plugin section, click Choose File.
4. Select `azure-vmss.hpi` in `target` folder of your repo, click Upload.
5. Restart your Jenkins instance after install is completed.

## Deploy an VM image to Virtual Machine Scale Sets

### Prerequisites

To use this plugin to deploy an VM image, first you need to have an Azure Service Principal in your Jenkins instance.

1. Create an Azure Service Principal through [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?toc=%2fazure%2fazure-resource-manager%2ftoc.json) or [Azure portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal).
2. Open Jenkins dashboard, go to Credentials, add a new Microsoft Azure Service Principal with the credential information you just created.

If you haven't created a virtual machine scale set, you can create one in Azure Portal or through Azure CLI.

You should also provide an VM image for deploy. For custom images, you can use tool such as [Packer](https://www.packer.io) to simplify your work.

### Build Steps

Usually you can organize your deploy process into two steps:

* Update Scale Sets

  This step updates image setting for specific Virtual Machine Scale Sets. Once finished, newly created virtual machines will be provisioned with the new image. Currently running machines are not affected.

* Update Instances

  This step updates specific instances using the latest image setting of the scale sets. Instances will be stopped and re-created with the new image.

### Pipeline

You can also use this plugin in pipeline (Jenkinsfile). Here are some samples to use the plugin in pipeline script:

To update scale sets:

```groovy
// Update with official image
azureVMSSUpdate azureCredentialsId: '<credential_id>', resourceGroup: '<resource_group_name>', name: '<name>',
        imageReference: [offer: 'UbuntuServer', publisher: 'Canonical', sku: '16.04-LTS', version: 'latest']

// Update with custom image
azureVMSSUpdate azureCredentialsId: '<credential_id>', resourceGroup: '<resource_group_name>', name: '<name>',
        imageReference: [id: '/subscriptions/<subscription>/resourceGroups/<resource_group_name>/providers/Microsoft.Compute/images/<image_name>']
```

To update instances:

```groovy
azureVMSSUpdateInstances azureCredentialsId: '<credential_id>', resourceGroup: '<resource_group_name>', name: '<name>',
        instanceIds: '1,2,3'
```

For advanced options, you can use Jenkins Pipeline Syntax tool to generate a sample script.

## Data/Telemetry

Azure Virtual Machine Scale Set Plugin collects usage data and sends it to Microsoft to help improve our products and services. Read our [privacy statement](http://go.microsoft.com/fwlink/?LinkId=521839) to learn more.

You can turn off usage data collection in Manage Jenkins -> Configure System -> Azure -> Help make Azure Jenkins plugins better by sending anonymous usage statistics to Azure Application Insights.

## Reporting bugs and feature requests

We use [Jenkins JIRA](https://issues.jenkins-ci.org/) to record all bugs and feature requests. Please follow below steps to create your own issues.

1. Search in Jira to see if the issue was existed already.
2. Create a new issue with the component `azure-vmss-plugin` .

You can refer to [Jira doc](https://confluence.atlassian.com/jiracoreserver/creating-issues-and-sub-tasks-939937904.html#Creatingissuesandsub-tasks-Creatinganissue) for detailed instructions about creating an issue.