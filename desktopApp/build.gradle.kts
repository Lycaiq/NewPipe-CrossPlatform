/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.about.libraries)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.jetbrains.coroutines.swing)
    implementation(libs.jetbrains.compose.preview)

    // VLCj: el único reproductor JVM con soporte real de HLS/DASH sin browser engine.
    // vlcj-natives incluye los bindings nativos precompilados para Windows/macOS/Linux.
    implementation(libs.vlcj.core)
    implementation(libs.vlcj.natives)

    // Extractor de NewPipe + OkHttp como downloader
    implementation(libs.newpipe.extractor)
    implementation(libs.squareup.okhttp)
}

compose.desktop {
    application {
        mainClass = "net.newpipe.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = NEWPIPE_APPLICATION_ID_NEW
            packageVersion = NEWPIPE_VERSION_NAME
        }

        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}

aboutLibraries {
    export {
        outputFile = file("../shared/src/jvmMain/resources/aboutlibraries.json")
        prettyPrint = true
        excludeFields.addAll("organization", "scm", "funding")
    }
}
