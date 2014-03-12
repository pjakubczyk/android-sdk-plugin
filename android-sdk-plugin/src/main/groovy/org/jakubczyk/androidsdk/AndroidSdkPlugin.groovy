package org.jakubczyk.androidsdk

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidSdkPlugin implements Plugin<Project> {


    void apply(Project project) {

        project.afterEvaluate {

            def compileSdkVersion = project.android.getCompileSdkVersion() as String
            def revision = project.android.getBuildToolsRevision() as String

            // nasty fallback since IntelliJ doesn't read .bashrc
            def androidSdk = (System.getenv("ANDROID_HOME") ?: System.getenv("LD_LIBRARY_PATH")[0..-5] + "sdk")

            def androidBin = (androidSdk + "/tools/android")

            if (checkForBuildTools(androidSdk, revision))
                project.logger.lifecycle "Found Android build tools version '${revision}'"
            else {
                project.logger.lifecycle "Not found Android build tools version '${revision}' -> downloading..."
                downloadBuildTools(project, androidBin, revision)
            }

            if (checkForSdk(androidBin, compileSdkVersion)) {
                project.logger.lifecycle "Found Android SDK version '${compileSdkVersion}'"
            } else {
                project.logger.lifecycle "Not found Android SDK version '${compileSdkVersion}' -> downloading..."
                downloadSdk(project, androidBin, compileSdkVersion)
            }


            try {
                def androidProjectDependenciesList = project.configurations.compile.getAllDependencies()

                def androidExtras = new File(androidSdk + "extras/android/m2repository/").exists()
                def googleExtras = new File(androidSdk + "extras/google/m2repository/").exists()

                // extra-android-m2repository
                // extra-google-m2repository

                androidProjectDependenciesList.each {
                    println it
                    if (!androidExtras && it.group == "com.android.support") {
                        project.logger.lifecycle "Downloading Android Support Repository"
                        downloadExtra(project, androidBin, "extra-android-m2repository")
                    }

                    if (!googleExtras && it.group == "com.google.android.gms") {
                        project.logger.lifecycle "Downloading Google Repository"
                        downloadExtra(project, androidBin, "extra-google-m2repository")
                    }
                }
            } catch (Exception e) {
                println e
            }
        }


    }

    def downloadExtra(Project project, String androidBin, String bundle) {
        def command = [androidBin, "update", "sdk", "-u", "-a", "-t", bundle]

        def process = command.execute()

        Thread.start {
            acceptLicence(project, process)
        }

        process.waitFor()
    }

    boolean checkForSdk(String androidBin, String compileSdkVersion) {
        def command = [androidBin, "list", "target", "-c"]

        def process = command.execute()
        process.waitFor()

        return process.text.contains(compileSdkVersion)
    }

    void downloadSdk(Project project, String androidBin, String compileSdkVersion) {
        def command = [androidBin, "update", "sdk", "--filter", compileSdkVersion, "--no-ui"]

        def process = command.execute()

        Thread.start {
            acceptLicence(project, process)
        }

        process.waitFor()
    }

    boolean checkForBuildTools(String androidSdk, String revision) {
        def availableBuildTools = new File(androidSdk + "/build-tools").list() as List

        availableBuildTools == null ? false : availableBuildTools.contains(revision)
    }

    void downloadBuildTools(Project project, String androidBin, String revision) {
        def command = [androidBin, "update", "sdk", "-u", "-a", "-t", "build-tools-${revision}"]

        def process = command.execute()

        Thread.start {
            acceptLicence(project, process)
        }

        process.waitFor()
    }

    private void acceptLicence(Project project, Process process) {
        def reader = new BufferedReader(new InputStreamReader(process.in))
        def writer = new PrintWriter(new BufferedOutputStream(process.out))
        writer.println('y\n')
        writer.close()

        def next
        while ((next = reader.readLine()) != null) {
            project.logger.info next
        }
    }


}
