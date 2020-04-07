package AE_AzureCustomNamespaceMonitoringExtension.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object AE_AzureCustomNamespaceMonitoringExtension_Azurecustomnamespacemonitoringextensiongitci : GitVcsRoot({
    uuid = "8c18572e-9fff-4708-a004-743a909076ea"
    name = "azurecustomnamespacemonitoringextensiongitci"
    url = "git@github.com:prashmeh/azure-custom-namespace-monitoring-extension.git"
    pushUrl = "git@github.com:prashmeh/azure-custom-namespace-monitoring-extension.git"
    branch = "refs/heads/1.0.0"
    authMethod = uploadedKey {
        uploadedKey = "Teamcity BB Key"
        passphrase = "credentialsJSON:937bf972-53b0-48bb-98bf-8eee5e4622e6"
    }
})
