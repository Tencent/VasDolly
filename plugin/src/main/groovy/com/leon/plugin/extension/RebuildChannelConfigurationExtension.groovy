package com.leon.plugin.extension

import org.gradle.api.GradleException;
import org.gradle.api.Project;

/**
 * Created by leontli on 17/2/18.
 */

public class RebuildChannelConfigurationExtension extends ConfigurationExtension{
    /**
     * debug base APK path
     */
    File baseDebugApk;
    /**
     * release base APK path
     */
    File baseReleaseApk;
    /**
     * debug channel apk output dir
     */
    File debugOutputDir;
    /**
     * release channel apk output dir
     */
    File releaseOutputDir;

    RebuildChannelConfigurationExtension(Project project) {
        mProject = project;
        debugOutputDir = new File(project.buildDir, "rebuildChannel/debug");
        releaseOutputDir = new File(project.buildDir, "rebuildChannel/release");
    }


    public boolean isNeedRebuildDebugApk() {
        if (baseDebugApk == null || !baseDebugApk.exists() || !baseDebugApk.isFile()) {
            println("baseDebugApk : ${baseDebugApk} , it is not a valid file , so can not rebuild debug channel apk");
            return false
        } else {
            if (debugOutputDir == null) {
                throw new GradleException("baseDebugApk : ${baseDebugApk} is ok , so you should set a valid debugOutputDir , or you can set baseDebugApk = null")
            }

            if (!debugOutputDir.exists()) {
                debugOutputDir.mkdirs()
            } else {
                // delete old apks
                debugOutputDir.eachFile { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }

            return true
        }
    }

    public boolean isNeedRebuildReleaseApk() {
        if (baseReleaseApk == null || !baseReleaseApk.exists() || !baseReleaseApk.isFile()) {
            println("baseReleaseApk : ${baseReleaseApk} , it is not a valid file , so can not rebuild release channel apk");
            return false
        } else {
            if (releaseOutputDir == null) {
                throw new GradleException("baseReleaseApk : ${baseReleaseApk} is ok , so you should set a valid releaseOutputDir , or you can set baseReleaseApk = null")
            }

            if (!releaseOutputDir.exists()) {
                releaseOutputDir.mkdirs()
            } else {
                // delete old apks
                releaseOutputDir.eachFile { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }
            return true
        }
    }
}