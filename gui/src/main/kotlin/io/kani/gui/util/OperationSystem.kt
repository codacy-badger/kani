package io.kani.gui.util

import java.util.*

enum class OperationSystem {
    Windows, MacOS, Linux, Other;

    companion object {
        @JvmStatic
        private val systemOS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)

        @JvmStatic
        val currentOperationSystem = when {
            systemOS.indexOf("mac") >= 0 || systemOS.indexOf("darwin") >= 0 -> MacOS
            systemOS.indexOf("nux") >= 0 -> Linux
            systemOS.indexOf("win") >= 0 -> Windows
            else -> Other
        }
    }
}


