package org.robospock.androidsdk

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidSdkPlugin implements Plugin<Project> {


    void apply(Project project) {

        project.afterEvaluate {

            def compileSdkVersion = project.android.getCompileSdkVersion() as String
            def revision = project.android.getBuildToolsRevision() as String

            // nasty fallback since IntelliJ doesn't read .bashrc
            def androidSdk = System.getenv("ANDROID_HOME") ?: System.getenv("LD_LIBRARY_PATH")[0..-5] + "sdk"
            def androidBin = androidSdk + "/tools/android"

            if (checkForBuildTools(androidSdk, revision))
                project.logger.lifecycle "Found Android build tools version '${revision}'"
            else {
                project.logger.lifecycle "Not found Android build tools version '${revision}' -> downloading..."
                downloadBuildTools(project,androidBin, revision)
            }

            if (checkForSdk(androidBin, compileSdkVersion)) {
                project.logger.lifecycle "Found Android SDK version '${compileSdkVersion}'"
            } else {
                project.logger.lifecycle "Not found Android SDK version '${compileSdkVersion}' -> downloading..."
                downloadSdk(project, androidBin, compileSdkVersion)
            }
        }
    }

    boolean checkForSdk(String androidBin, String compileSdkVersion) {
        def command = "${androidBin} list target -c"

        def process = command.execute()
        process.waitFor()

        return process.text.contains(compileSdkVersion)
    }

    void downloadSdk(Project project, String androidBin, String compileSdkVersion) {
        def command = "${androidBin} update sdk --filter ${compileSdkVersion} --no-ui"

        def process = command.execute()

        Thread.start {
            acceptLicence(project, process)
        }

        process.waitFor()
    }

    boolean checkForBuildTools(String androidSdk, String revision) {
        def availableBuildTools = new File(androidSdk + "/build-tools").list() as List
        availableBuildTools.contains(revision)
    }

    void downloadBuildTools(Project project, String androidBin, String revision) {
        def command = "${androidBin} update sdk -u -a -t build-tools-${revision}"

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
