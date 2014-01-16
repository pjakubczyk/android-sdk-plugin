package org.robospock.androidsdk

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidSdkPlugin implements Plugin<Project> {


    void apply(Project project) {
        Project androidProject

        def appPlugin

        project.getRootProject().getAllprojects().each {
            if (it.plugins.hasPlugin("android")) {
                androidProject = it
                appPlugin = it.plugins["android"]
            }
        }


        def compileSdkVersion = androidProject.android.getCompileSdkVersion()
        def androidBin = System.getenv("ANDROID_HOME") + "/tools/android"



        if (checkForSdk(androidBin, compileSdkVersion)) {
            println "compileSdkVersion " + compileSdkVersion + " is available"
        } else {
            println "compileSdkVersion " + compileSdkVersion + " is not available"
            downloadSdk(androidBin, compileSdkVersion)
        }


    }

    boolean checkForSdk(String androidBin, String compileSdkVersion) {
        println "checking for: " + compileSdkVersion
        def command = """${androidBin} list target -c"""// Create the String
        println(command)
        def proc = command.execute()                 // Call *execute* on the string
        proc.waitFor()
        def stdout = proc.text
        println stdout
        return stdout.contains(compileSdkVersion)
    }

    void downloadSdk(String androidBin, String compileSdkVersion) {
        def command = """${androidBin} update sdk --filter ${compileSdkVersion} --no-ui"""


        def proc = command.execute()                 // Call *execute* on the string

        Thread.start {
            def reader = new BufferedReader(new InputStreamReader(proc.in))
            def writer = new PrintWriter(new BufferedOutputStream(proc.out))
            writer.println('y\n')
            writer.close()

            def next
            while ((next = reader.readLine()) != null) {
                println next
            }

        }

        proc.waitFor()
    }


}
