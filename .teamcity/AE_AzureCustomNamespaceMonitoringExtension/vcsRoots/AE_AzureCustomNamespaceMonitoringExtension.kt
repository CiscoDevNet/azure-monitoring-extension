package AE_AzureCustomNamespaceMonitoringExtension.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.vcs.GitVcsRoot

object AE_AzureCustomNamespaceMonitoringExtension : GitVcsRoot({
    uuid = "2de54c27-e130-4cea-8fef-57045953bbdf"
    id("AE_AzureCustomNamespaceMonitoringExtension")
    name = "AE_AzureCustomNamespaceMonitoringExtension"
    url = "ssh://git@bitbucket.corp.appdynamics.com:7999/ae/azure-custom-namespace-monitoring-extension.git"
    pushUrl = "ssh://git@bitbucket.corp.appdynamics.com:7999/ae/azure-custom-namespace-monitoring-extension.git"
    authMethod = uploadedKey {
        uploadedKey = "TeamCity BitBucket Key"
    }
    agentCleanPolicy = AgentCleanPolicy.ALWAYS
    branchSpec = """
        +:refs/heads/(master)
        +:refs/(pull-requests/*)/from
        """.trimIndent()
})
