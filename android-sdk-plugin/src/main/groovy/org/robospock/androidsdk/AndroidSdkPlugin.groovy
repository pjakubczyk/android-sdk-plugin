package org.robospock.androidsdk

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidSdkPlugin implements Plugin<Project> {


    void apply(Project project) {

        project.afterEvaluate {
            def compileSdkVersion = project.android.getCompileSdkVersion()

            // nasty fallback since IntelliJ doesn't read .bashrc
            def androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("LD_LIBRARY_PATH")[0..-5] + "sdk"
            def androidBin = androidHome + "/tools/android"

            if (checkForSdk(androidBin, compileSdkVersion)) {
                println compileSdkVersion + " found."
            } else {
                println compileSdkVersion + " not found. Downloading ..."
                downloadSdk(androidBin, compileSdkVersion)
            }
        }
    }

    boolean checkForSdk(String androidBin, String compileSdkVersion) {

        def command = """${androidBin} list target -c"""// Create the String

        def process = command.execute()                 // Call *execute* on the string
        process.waitFor()

        return process.text.contains(compileSdkVersion)
    }

    void downloadSdk(String androidBin, String compileSdkVersion) {
        def command = """${androidBin} update sdk --filter ${compileSdkVersion} --no-ui"""


        def process = command.execute()                 // Call *execute* on the string

        Thread.start {
            def reader = new BufferedReader(new InputStreamReader(process.in))
            def writer = new PrintWriter(new BufferedOutputStream(process.out))
            writer.println('y\n')
            writer.close()

            def next
            while ((next = reader.readLine()) != null) {
                println next
            }

        }

        process.waitFor()
    }


}
