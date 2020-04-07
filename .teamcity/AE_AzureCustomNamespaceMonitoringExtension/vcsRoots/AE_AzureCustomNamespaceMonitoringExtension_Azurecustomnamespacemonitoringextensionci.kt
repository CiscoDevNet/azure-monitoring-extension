package AE_AzureCustomNamespaceMonitoringExtension.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object AE_AzureCustomNamespaceMonitoringExtension_Azurecustomnamespacemonitoringextensionci : GitVcsRoot({
    uuid = "e1bd1329-ee6c-4645-b763-225f0fde47f1"
    name = "azurecustomnamespacemonitoringextensionci"
    url = "ssh://git@bitbucket.corp.appdynamics.com:7999/ae/azure-custom-namespace-monitoring-extension.git"
    pushUrl = "ssh://git@bitbucket.corp.appdynamics.com:7999/ae/azure-custom-namespace-monitoring-extension.git"
    branch = "refs/heads/1.0.0"
    authMethod = uploadedKey {
        uploadedKey = "Teamcity BB Key"
        passphrase = "credentialsJSON:937bf972-53b0-48bb-98bf-8eee5e4622e6"
    }
})
