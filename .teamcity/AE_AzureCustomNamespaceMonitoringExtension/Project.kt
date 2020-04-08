package AE_AzureCustomNamespaceMonitoringExtension

import AE_AzureCustomNamespaceMonitoringExtension.buildTypes.*
import AE_AzureCustomNamespaceMonitoringExtension.vcsRoots.AE_AzureCustomNamespaceMonitoringExtension
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.versionedSettings

object Project : Project({
    uuid = "8a2085ee-30c8-40c8-b68d-0c86abe73148"
    id("AE_AzureCustomNamespaceMonitoringExtension")
    parentId("AE")
    name = "AE_Azure CustomNamespace Monitoring Extension"

    vcsRoot(AE_AzureCustomNamespaceMonitoringExtension)

    buildType(AE_AzureCustomNamespaceMonitoringExtension_Build)
    buildType(AE_AzureCustomNamespaceMonitoringExtension_IntegrationTests)

    features {
        versionedSettings {
            id = "PROJECT_EXT_4"
            mode = VersionedSettings.Mode.ENABLED
            buildSettingsMode = VersionedSettings.BuildSettingsMode.PREFER_SETTINGS_FROM_VCS
            rootExtId = "${AE_AzureCustomNamespaceMonitoringExtension.id}"
            showChanges = false
            settingsFormat = VersionedSettings.Format.KOTLIN
            storeSecureParamsOutsideOfVcs = true
        }
    }

    params {
        password("env.CLIENT_ID", "zxx47244f4679365380172109a6f589a6c9a25e5baffe6fd6b8b7bcd0a59b7fa9b603dfba7f81e32b20")
        password("env.CLIENT_SECRET", "zxx5e1e72372ce8ae8b045878057cb6e2021783647d9f878141600a3b41febff4be67bf471d161bb5a4")
        password("env.SUBSCRIPTION_ID", "zxxadde6fc530cbb116801f2c087b9d7e135888f72eddeecd428d25153eef7bbc3e7bc2fb8e4c4a118f")
        password("env.TENANT_ID", "zxxc2cd637a6b2a6cd9dcd231ce995e8ce8cbb3d383b776cca3d85464ebeb52a0ff22416ab9961de16a")
    }

    buildTypesOrder = arrayListOf(
            AE_AzureCustomNamespaceMonitoringExtension_Build,
            AE_AzureCustomNamespaceMonitoringExtension_IntegrationTests
    )
})
