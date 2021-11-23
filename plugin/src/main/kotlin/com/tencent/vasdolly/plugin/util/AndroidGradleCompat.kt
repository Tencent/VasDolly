/*
 * Copyright (C) 2021 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.vasdolly.plugin.util

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.AndroidTest
import com.android.build.api.variant.UnitTest
import com.android.build.api.variant.Variant
import org.gradle.api.Project

/**
 * Compatibility version of [com.android.build.api.variant.AndroidComponentsExtension]
 * - In AGP 4.2 its package is 'com.android.build.api.extension'
 * - In AGP 7.0 its packages is 'com.android.build.api.variant'
 */
sealed class AndroidComponentsExtensionCompat {

    /**
     * A combined compatibility function of
     * [com.android.build.api.variant.AndroidComponentsExtension.onVariants] that includes also
     * [AndroidTest] and [UnitTest] variants.
     */
    abstract fun onAllVariants(block: (Variant) -> Unit)

    // AGP7.0
    class Api70Impl(
            private val actual: AndroidComponentsExtension<*, *, *>
    ) : AndroidComponentsExtensionCompat() {
        override fun onAllVariants(block: (Variant) -> Unit) {
            actual.onVariants { variant ->
                block.invoke(variant)
            }
        }
    }

    // AGP4.2
    class Api42Impl(private val actual: Any) : AndroidComponentsExtensionCompat() {

        private val extensionClazz = Class.forName("com.android.build.api.extension.AndroidComponentsExtension")

        private val variantSelectorClazz = Class.forName("com.android.build.api.extension.VariantSelector")

        override fun onAllVariants(block: (Variant) -> Unit) {
            val selector = extensionClazz.getDeclaredMethod("selector").invoke(actual)
            val allSelector = variantSelectorClazz.getDeclaredMethod("all").invoke(selector)
            val wrapFunction: (Variant) -> Unit = {
                block.invoke(it)
            }
            extensionClazz.getDeclaredMethod(
                    "onVariants", variantSelectorClazz, Function1::class.java
            ).invoke(actual, allSelector, wrapFunction)

        }
    }

    companion object {
        fun getAndroidComponentsExtension(project: Project): AndroidComponentsExtensionCompat {
            return if (
                    findClass("com.android.build.api.variant.AndroidComponentsExtension") != null
            ) {
                //AGP7.0
                val actualExtension = project.extensions.getByType(AndroidComponentsExtension::class.java)
                Api70Impl(actualExtension)
            } else {
                //AGP4.2
                val clsName = "com.android.build.api.extension.AndroidComponentsExtension"
                val actualExtension = project.extensions.getByType(Class.forName(clsName))
                Api42Impl(actualExtension)
            }
        }
    }
}

fun findClass(fqName: String) = try {
    Class.forName(fqName)
} catch (ex: ClassNotFoundException) {
    null
}