package AE_AzureCustomNamespaceMonitoringExtension.buildTypes

import AE_AzureCustomNamespaceMonitoringExtension.publishCommitStatus
import AE_AzureCustomNamespaceMonitoringExtension.vcsRoots.AE_AzureCustomNamespaceMonitoringExtension
import AE_AzureCustomNamespaceMonitoringExtension.withDefaults
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.exec
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs


/**
 * @author Prashant Mehta
 */
object AE_AzureCustomNamespaceMonitoringExtension_IntegrationTests : BuildType({
    uuid = "2591b76d-858b-4b71-82c0-a9df8f8b2d68"
    name = "Run Integration Tests"

    withDefaults()

    steps {
        exec {
            path = "make"
            arguments = "dockerRun"
        }

        exec {
            path = "make"
            arguments = "terraformApply"
        }

        //Waits for 5 minutes to send metrics to the controller
        exec {
            path = "make"
            arguments = "sleep"
        }

        maven {
            goals = "clean verify -DskipITs=false"
            mavenVersion = defaultProvidedVersion()
            jdkHome = "%env.JDK_18%"
            userSettingsSelection = "teamcity-settings"
        }

        exec {
            executionMode = BuildStep.ExecutionMode.ALWAYS
            path = "make"
            arguments = "terraformDestroy"
        }
        exec {
            executionMode = BuildStep.ExecutionMode.ALWAYS
            path = "make"
            arguments = "dockerStop"
        }

    }

    triggers {
        vcs {
        }
    }

    artifactRules = """
        /opt/buildAgent/work/machine-agent-logs => target/
""".trimIndent()

    dependencies {
        dependency(AE_AzureCustomNamespaceMonitoringExtension_Build) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts {
                artifactRules = """
                +:target/AzureCustomNamespaceMonitor-*.zip => target/
            """.trimIndent()
            }
        }
    }

    publishCommitStatus()
})