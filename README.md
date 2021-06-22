# Azure Monitoring Extension

## Use Case
The extension collects metrics supported by Azure Monitor using the Azure management APIs and displays them in the AppDynamics Metric Browser. Please refer the link below for details on supported metrics.
https://docs.microsoft.com/en-us/azure/azure-monitor/platform/metrics-supported

**NOTE:** The extension is built using Azure Java SDK and may not monitor all the Azure services.

## Prerequisites
The extension collects metrics, which are supported with Azure Monitor using the Azure management APIs. In order to query the APIs, you should have the following Ids from App registrations in Azure Active Directory.
```   
         client: 
         tenant: 
         secret: 
         subscriptionId:   
```
Please refer the link below for detailed steps [here](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal#create-a-new-application-secret)

In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Standalone+Machine+Agents) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).
The extension should be able to connect to the Azure management Apis in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

**Note : This extension is compatible with Machine Agent version 4.5.13 or later.

## Installation
1. Run 'mvn clean install' from azure-monitoring-extension
2. Copy and unzip AzureMonitor-\<version\>.zip from 'target' directory into \<machine_agent_dir\>/monitors/
3. Edit config.yml file in AzureMonitor/conf and provide the required configuration (see Configuration section)
4. Restart the Machine Agent.

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.
In the AppDynamics Metric Browser, look for **Application Infrastructure Performance|\<Tier\>|Custom Metrics|Azure|** and you should be able to see all the metrics.

## Configuration
In order to use the extension, you need to update the config.yml file that is present in the extension folder. Please find a detailed explanation for the configurable fields in the config.yml file.

1. If SIM is enabled, then use the following metricPrefix `metricPrefix: "Custom Metrics|Azure"` else configure the "COMPONENT_ID" under which the metrics need to be reported.
This can be done by changing the value of <COMPONENT_ID> in `metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|Azure|"`.
   For example,
     ```
     metricPrefix: "Server|Component:100|Custom Metrics|Azure|"
     ```
2. Provide the Azure client, tenant, secret and SubscriptionId under the credentials.

   ```
     credentials:
         client: "********-****-****-****-************"
         tenant: "********-****-****-****-************"
         secret: "*******"
         subscriptionId: "********-****-****-****-************"
   ```
   ```
       services:
       - resourceGroups: ["my.resource-group"]  # please put the required resourceGroups only
         serviceName: "Virtual machine"
         regions: [""East US""] # supports regex .*
         serviceInstances: ["my-vm"] #supports regex .*
   ```
 
**resourceGroups:** Azure Resource groups (this accepts an array of Resource groups with regex matching supported)
```
        resourceGroups: ["myResourceGroups", "myResourceGroup1", "app.*"]
```
**service:** Name of the service or the resource that you want to monitor for e.g. Virtual machine or Storage account or SQL server. Clearly, this accepts a single entry for service. You can find the serviceName from Azure portal which is **type** of the resource.
```
        serviceName: "Virtual machine"
```
**regions:** A list of entries for the regions of the service to be monitored.
```
        regions: ["East US 2", "East US"]
```
**serviceInstances:** service instance or resource name which we want to monitor.
```
        serviceInstances: ["myvm1", "myvm2"]
```
**credentials:** Azure Active Directory registered application credentials. You need to provide the client, tenant, secret and subscriptionId under it. Please refer the [link](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal#create-a-new-application-secret) for more details.

**Example:**
Configuration for Cosmos DB, where its instance belongs to a resourceGroup `test-ext-appd` and have 2 instances running (say `cosmos1` and `cosmos2`) in 2 different regions, `East US 2` and `East US`. You should configure  as below in the config.yml. 
``` 
 - displayName: "Azure Cosmos DB"
    resourceGroups: ["test-ext-appd"]  # Please put the concerned resourceGroups only
    serviceName: "Cosmos DB account"
    regions: ["East US", "East US 2"]
    serviceInstances: ["cosmos1", "cosmos2"]
```
Configuration for Storage Account.
```
  - displayName: "AzureAccount Storage"
    resourceGroups: ["test-ext.*"]
    service: "Storage account"
    regions: ["East US"]
    serviceInstances: [".*"]
```
**NOTE:** Since the resourceGroups, regions and serviceInstances support regex matching/filtering which means putting ".*" includes and queries for all the available resources from the account. Depending on the number of instances/resources that is being monitored, the extension may hit into rate limit issues(429 Too Many Requests) from Azure or even metrics explosion to the Appd Controller. Refer [here](https://docs.microsoft.com/en-us/azure/api-management/api-management-access-restriction-policies#LimitCallRate) for more details.

5. Configure the metrics in the metrics.xml.

     For configuring the metrics in the metrics.xml, the following properties can be used:

     |     Property      |   Default value |         Possible values         |                                              Description                                                                                                |
     | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
     | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
     | statType          | "ave"           | "AVERAGE", "SUM", "MIN", "MAX"  | Azure configured values as returned by API                                                                       |
     | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
     | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
     | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
     | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
     | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1  |
     | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |

    **All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.**
   
    Metrics will be collected for the services and stats configured here. By default we have added the stats for Virtual machine.
    Please make sure to add the service and stats details for monitoring.
    
### config.yml

Please avoid using tab (\t) when editing yaml files. Please copy all the contents of the config.yml file and go to [Yaml Validator](http://www.yamllint.com/) . On reaching the website, paste the contents and press the “Go” button on the bottom left.
If you get a valid output, that means your formatting is correct and you may move on to the next step.

**Below is an example config for monitoring VMs for an account and regions:**

~~~
#prefix used to show metrics in AppDynamics
metricPrefix: "Server|Component:<TIER_ID>|Custom Metrics|Azure|"
#metricPrefix: "Custom Metrics|Azure|"

accounts:
  - displayName: "Az VM Instances"
    credentials:
         client: ""
         tenant: ""
         secret: ""
         subscriptionId: ""
    services:
       - resourceGroups: ["my.*"]  # Please put the required resourceGroups only
         serviceName: "Virtual machine"
         regions: [".*"]
         serviceInstances: [".*"]

~~~
# List of Supported services
Below is the list of services which is supported by the extension:

1.Virtual machine
2.Storage account
3.Container instances
4.SQL server
5.App Service
6.App Service plan
7.ApplicationGateway
8.Cosmos DB Account

## Metrics
Typical metric path: **Application Infrastructure Performance|\<Tier\>|Custom Metrics|Azure|\<Account DisplayName\>|\<serviceName\>|serviceInstanceName|\<metric\>** followed by the metrics defined in the link below:
- [Azure Metrics](https://docs.microsoft.com/en-us/azure/azure-monitor/platform/metrics-supported)


## Credentials Encryption
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.

## Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

1. Stop the running machine agent.
2. Delete all existing logs under <MachineAgent>/logs.
3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
   <logger name="com.singularity">
   <logger name="com.appdynamics">
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
5. Attach the zipped <MachineAgent>/conf/* directory here.
6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.
   For any support related questions, you can also contact help@appdynamics.com.

## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/azure-monitoring-extension).

## Version
   |          Name            |  Version   |
   |--------------------------|------------|
   |Extension Version         |1.0.5      |
   |Controller Compatibility  |4.5 or Later|
   |Agent Compatibility        | 4.5.13 or later|
   |Last Update               |11 May, 2021 |
List of changes to this extension can be found [here](https://github.com/Appdynamics/azure-monitoring-extension/blob/master/CHANGELOG.md)
