import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.FileContentReplacer
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.replaceContent
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.1"

project {

    subProject(Build)
    subProject(Deploy)
}


object Build : Project({
    name = "Build"

    buildType(Build_ScanForVulnerabilities)
    buildType(Build_1)
    buildType(BuildFrontend)
    buildType(BuildBackendImage)
    buildType(BuildFrontendImage)
    buildTypesOrder = arrayListOf(Build_1, BuildBackendImage, BuildFrontend, BuildFrontendImage)
})

object Build_1 : BuildType({
    id("Build")
    name = "Build Backend"

    artifactRules = "backend/build/libs/*"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            tasks = "build"
            workingDir = "backend"
            dockerImage = "openjdk:8-jdk"
        }
    }

    failureConditions {
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.TEST_COUNT
            units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
            comparison = BuildFailureOnMetric.MetricComparison.LESS
            compareTo = build {
                buildRule = lastFinished()
            }
        }
    }

    features {
        pullRequests {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            provider = github {
                authType = token {
                    token = "credentialsJSON:6a41e8a0-1293-48e3-93c6-b961214f46e3"
                }
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
            }
        }

        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:acac33de-a843-4933-b273-64b5f9184815"
                }
            }
        }
    }
})

object BuildBackendImage : BuildType({
    name = "Build Backend Image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dockerCommand {
            name = "Build image"
            commandType = build {
                source = path {
                    path = "backend/Dockerfile"
                }
                contextDir = "backend"
                namesAndTags = "%dockerNamespace%/guestbook-backend:%build.number%"
                commandArgs = "--pull --build-arg JAR_FILE=build/*.jar"
            }
        }
        dockerCommand {
            name = "Push image"
            commandType = push {
                namesAndTags = "%dockerNamespace%/guestbook-backend:%build.number%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_4"
            }
        }
    }

    dependencies {
        dependency(Build_1) {
            snapshot {
            }

            artifacts {
                cleanDestination = true
                artifactRules = "*.jar => backend/build"
            }
        }
    }
})

object BuildFrontend : BuildType({
    name = "Build Frontend"

    artifactRules = "frontend/docker/dist/* => dist/"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            workingDir = "frontend"
            scriptContent = """
                npm install
                npm run build
            """.trimIndent()
            dockerImage = "node"
        }
    }
})

object BuildFrontendImage : BuildType({
    name = "Build Frontend Image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dockerCommand {
            name = "Build image"
            commandType = build {
                source = path {
                    path = "frontend/docker/Dockerfile"
                }
                contextDir = "frontend/docker"
                namesAndTags = "%dockerNamespace%/guestbook-frontend:%build.number%"
                commandArgs = "--pull"
            }
        }
        dockerCommand {
            name = "Push image"
            commandType = push {
                namesAndTags = "%dockerNamespace%/guestbook-frontend:%build.number%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_4"
            }
        }
    }

    dependencies {
        dependency(BuildFrontend) {
            snapshot {
            }

            artifacts {
                cleanDestination = true
                artifactRules = "dist/ => frontend/docker/dist"
            }
        }
    }
})

object Build_ScanForVulnerabilities : BuildType({
    name = "Scan for vulnerabilities"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        step {
            type = "snykSecurity"
            param("severityThreshold", "high")
            param("file", "backend/build.gradle")
            param("version", "1.149.0")
            param("secure:apiToken", "credentialsJSON:b7a47992-5f9c-46a9-8968-9055eabf2a05")
        }
    }

    dependencies {
        snapshot(Build_1) {
            runOnSameAgent = true
            reuseBuilds = ReuseBuilds.NO
        }
    }
})


object Deploy : Project({
    name = "Deploy"

    buildType(DeployGuestbook)
    buildType(DeployStaging)
    buildTypesOrder = arrayListOf(DeployStaging, DeployGuestbook)
})

object DeployGuestbook : BuildType({
    name = "Deploy to Production"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            workingDir = "deployment"
            scriptContent = """
                gcloud components install kubectl
                gcloud container clusters get-credentials guestbook-cluster --region=us-west2-a
                kubectl apply -f guestbook-deployment.yml
            """.trimIndent()
            dockerImage = "google/cloud-sdk:alpine"
        }
    }

    features {
        replaceContent {
            fileRules = "deployment/guestbook-deployment.yml"
            pattern = "us.gcr.io/teamcitytest-166414/guestbook-backend"
            regexMode = FileContentReplacer.RegexMode.FIXED_STRINGS
            replacement = "us.gcr.io/teamcitytest-166414/guestbook-backend:${BuildBackendImage.depParamRefs.buildNumber}"
        }
        replaceContent {
            fileRules = "deployment/guestbook-deployment.yml"
            pattern = "us.gcr.io/teamcitytest-166414/guestbook-frontend"
            regexMode = FileContentReplacer.RegexMode.FIXED_STRINGS
            replacement = "us.gcr.io/teamcitytest-166414/guestbook-frontend:${BuildFrontendImage.depParamRefs.buildNumber}"
        }
    }

    dependencies {
        snapshot(DeployStaging) {
        }
    }
})

object DeployStaging : BuildType({
    name = "Deploy to Staging"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            workingDir = "deployment"
            scriptContent = """
                gcloud components install kubectl
                gcloud container clusters get-credentials guestbook-cluster --region=us-west2-a
                kubectl apply -f guestbook-staging.yml
            """.trimIndent()
            dockerImage = "google/cloud-sdk:alpine"
        }
    }

    triggers {
        vcs {
            branchFilter = ""
        }
    }

    features {
        replaceContent {
            fileRules = "deployment/guestbook-staging.yml"
            pattern = "us.gcr.io/teamcitytest-166414/guestbook-backend"
            regexMode = FileContentReplacer.RegexMode.FIXED_STRINGS
            replacement = "us.gcr.io/teamcitytest-166414/guestbook-backend:${BuildBackendImage.depParamRefs.buildNumber}"
        }
        replaceContent {
            fileRules = "deployment/guestbook-staging.yml"
            pattern = "us.gcr.io/teamcitytest-166414/guestbook-frontend"
            regexMode = FileContentReplacer.RegexMode.FIXED_STRINGS
            replacement = "us.gcr.io/teamcitytest-166414/guestbook-frontend:${BuildFrontendImage.depParamRefs.buildNumber}"
        }
    }

    dependencies {
        snapshot(BuildBackendImage) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(BuildFrontendImage) {
        }
        snapshot(Build_ScanForVulnerabilities) {
            reuseBuilds = ReuseBuilds.NO
            onDependencyFailure = FailureAction.IGNORE
            onDependencyCancel = FailureAction.IGNORE
        }
    }
})
