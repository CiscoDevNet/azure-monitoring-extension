# AppDynamics Azure Custom Namespace Monitoring Extension

The extension collects metrics, which are supported with Azure Monitor using the Azure management APIs. Please refer the link below for details on supported metrics.
https://docs.microsoft.com/en-us/azure/azure-monitor/platform/metrics-supported

The AzureCustomNamespaceMonitor extension is designed to collect metrics for an Azure  service using/filtering on the resourceGroups/regions/serviceInstances per account. They all can be configured in the config.yml, available in the zip attached.
```
accounts:
 - displayName: "AzureAccount VM"
   resourceGroups: ["test-ext.*"]  # Please put the concerned resourceGroups only
   service: "Virtual machine"
   regions: [".*"]
   serviceInstances: [".*"]
   credentials:
       client: ""
       tenant: ""
       secret: ""
       subscriptionId: ""
```       
Please find the details/explanation for the parameters below.

resourceGroups: Azure Resource groups (this accepts an array of Resource groups with regex matching supported)
resourceGroups: ["myResourceGroups", "myResourceGroup1", "app.*"]


service: Name of the service or the resource that you want to monitor for e.g. Virtual machine or Storage account or SQL server. Clearly, this accepts a single entry for service.
 service: "Virtual machine"


regions: A list of entries for the regions of the service to be monitored.
regions: ["East US 2", "East US"]


serviceInstances: service instance or resource name which we want to monitor.
serviceInstances: ["myvm1", "myvm2"]


credentials: Azure Active Directory registered application credentials. You need to provide the client, tenant, secret and subscriptionId under it. You may refer the link if needed.

   resourceGroups, regions and serviceInstances support regex matching/filtering which means putting ".*" includes and queries for all the available resources from the account, which will or most likely cause issues like rate limit issues(429 Too Many Requests)from Azure and metrics explosion to the Appd Controller.

So, you need to choose service first, let's say Cosmos DB, and it belongs to a resourceGroup test-ext-appd and have 2 instances running (say cosmos1 and cosmos2) in 2 different regions, East US 2, East US. You should configure  as below in the config.yml. 
```
  - displayName: "Azure Cosmos DB"
    resourceGroups: ["test-ext-appd"]  # Please put the concerned resourceGroups only
    service: "Cosmos DB account"
    regions: ["East US", "East US 2"]
    serviceInstances: ["cosmos1", "cosmos2"]
```    

A sample configuration covering multiple services from same or multiple accounts can be configured as below.
accounts:
```
  - displayName: "AzureAccount VM"

    resourceGroups: ["test-ext.*"]  # Please put the concerned resourceGroups only

    service: "Virtual machine"

    regions: [".*"]

    serviceInstances: [".*"]

    credentials:

        client: ""

        tenant: ""

        secret: ""

        subscriptionId: ""



  - displayName: "AzureAccount Storage"

    resourceGroups: ["test-ext.*"]

    service: "Storage account"

    regions: ["East US"]

    serviceInstances: [".*"]

    credentials:

        client: ""

        tenant: ""

        secret: ""

        subscriptionId: ""



  - displayName: "AzureAccount Container"

    resourceGroups: [".*ext.*"]

    service: "Container instances"

    regions: [".*"]

    serviceInstances: [".*"]

    credentials:

        client: ""

        tenant: ""

        secret: ""

        subscriptionId: ""



  - displayName: "AzureAccount Sql server"

    resourceGroups: [".*"]

    service: "SQL server"

    regions: ["australia.*"]

    serviceInstances: [".*"]

    credentials:

        client: ""

        tenant: ""

        secret: ""

        subscriptionId: ""

```
Azure API rate limit policy: 
Azure has stringent rate-limit policies in different scopes, please refer their documentation for more details. So, We recommend you to avoid putting (.*) on the regex supported configurations and monitor only the needed resources/instances/resource groups.


