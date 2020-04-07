package AE_AzureCustomNamespaceMonitoringExtension

import AE_AzureCustomNamespaceMonitoringExtension.vcsRoots.*
import AE_AzureCustomNamespaceMonitoringExtension.vcsRoots.AE_AzureCustomNamespaceMonitoringExtension_Azurecustomnamespacemonitoringextensionci
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.versionedSettings

object Project : Project({
    uuid = "48a46fc4-076f-43f1-a606-4047dd46ceb6"
    id("AE_AzureCustomNamespaceMonitoringExtension")
    parentId("AE")
    name = "Azure CustomNamespace Monitoring Extension"

    vcsRoot(AE_AzureCustomNamespaceMonitoringExtension_Azurecustomnamespacemonitoringextensionci)

    features {
        versionedSettings {
            id = "PROJECT_EXT_2"
            mode = VersionedSettings.Mode.ENABLED
            buildSettingsMode = VersionedSettings.BuildSettingsMode.PREFER_SETTINGS_FROM_VCS
            rootExtId = "${AE_AzureCustomNamespaceMonitoringExtension_Azurecustomnamespacemonitoringextensionci.id}"
            showChanges = false
            settingsFormat = VersionedSettings.Format.KOTLIN
            storeSecureParamsOutsideOfVcs = true
        }
    }
})
