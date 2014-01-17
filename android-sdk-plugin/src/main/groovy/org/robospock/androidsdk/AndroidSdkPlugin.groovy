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
                println "build-tools-${revision} found"
            else {
                println "build-tools-${revision} not found. Downloading."
                downloadBuildTools(androidBin, revision)
            }

            if (checkForSdk(androidBin, compileSdkVersion)) {
                println "${compileSdkVersion} found."
            } else {
                println "${compileSdkVersion} not found. Downloading ..."
                downloadSdk(androidBin, compileSdkVersion)
            }
        }
    }

    boolean checkForSdk(String androidBin, String compileSdkVersion) {
        def command = "${androidBin} list target -c"

        def process = command.execute()
        process.waitFor()

        return process.text.contains(compileSdkVersion)
    }

    void downloadSdk(String androidBin, String compileSdkVersion) {
        def command = "${androidBin} update sdk --filter ${compileSdkVersion} --no-ui"

        def process = command.execute()

        Thread.start {
            acceptLicence(process)
        }

        process.waitFor()
    }

    boolean checkForBuildTools(String androidSdk, String revision) {
        def availableBuildTools = new File(androidSdk + "/build-tools").list() as List
        availableBuildTools.contains(revision)
    }

    void downloadBuildTools(String androidBin, String revision) {
        def command = "${androidBin} update sdk -u -a -t build-tools-${revision}"

        def process = command.execute()

        Thread.start {
            acceptLicence(process)
        }

        process.waitFor()
    }

    private void acceptLicence(Process process) {
        def reader = new BufferedReader(new InputStreamReader(process.in))
        def writer = new PrintWriter(new BufferedOutputStream(process.out))
        writer.println('y\n')
        writer.close()

        def next
        while ((next = reader.readLine()) != null) {
            println next
        }
    }


}
