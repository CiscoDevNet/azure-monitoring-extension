package AE_AzureCustomNamespaceMonitoringExtension.buildTypes

import AE_AzureCustomNamespaceMonitoringExtension.publishCommitStatus
import AE_AzureCustomNamespaceMonitoringExtension.vcsRoots.AE_AzureCustomNamespaceMonitoringExtension
import AE_AzureCustomNamespaceMonitoringExtension.withDefaults
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object AE_AzureCustomNamespaceMonitoringExtension_Build : BuildType({
    uuid = "9d9925fc-7815-11ea-bc55-0242ac130003"
    name = "Azure CustomNamespace Monitoring Extension Build"

    withDefaults()

    steps {
        maven {
            goals = "clean install"
            mavenVersion = defaultProvidedVersion()
            jdkHome = "%env.JDK_18%"
            userSettingsSelection = "teamcity-settings"
        }
    }

    triggers {
        vcs {
        }
    }

    artifactRules = """
    +:target/AzureCustomNamespaceMonitor-*.zip => target/
""".trimIndent()

    publishCommitStatus()

})