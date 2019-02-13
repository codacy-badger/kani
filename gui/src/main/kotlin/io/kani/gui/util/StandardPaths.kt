package io.kani.gui.util

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import io.kani.gui.util.jna.win32.Userenv
import java.nio.file.Path
import java.nio.file.Paths


/**
 * The StandardPaths class provides methods for accessing standard paths.
 * This class contains functions to query standard locations on the local filesystem, for common tasks such as
 * user-specific directories or system-wide configuration directories.

 * This class is a ported version of Qt's QStandardPaths
 *
 * @see <a href="http://doc.qt.io/qt-5/qstandardpaths.html">Qt documentation</a>
 * @see <a href="https://github.com/qt/qtbase/tree/dev/src/corelib/io">Source</a>
 */
object StandardPaths {
    enum class Location {
        /**
         * Returns the user's desktop directory. This is a generic value. On systems with no concept of a desktop.
         */
        DesktopLocation,

        /**
         * Returns the directory containing user document files. This is a generic value.
         * The returned path is never empty.
         */
        DocumentsLocation,

        /**
         * Returns the directory containing user's fonts. This is a generic value.
         * Note that installing fonts may require additional, platform-specific operations.
         */
        FontsLocation,

        /**
         * Returns the directory containing the user applications (either executables, application bundles,
         * or shortcuts to them). This is a generic value.
         * Note that installing applications may require additional, platform-specific operations.
         * Files, folders or shortcuts in this directory are platform-specific.
         */
        ApplicationsLocation,

        /**
         * Returns the directory containing the user's music or other audio files. This is a generic value.
         * If no directory specific for music files exists, a sensible fallback for storing user documents is returned.
         */
        MusicLocation,

        /**
         * Returns the directory containing the user's movies and videos. This is a generic value.
         * If no directory specific for movie files exists, a sensible fallback for storing user documents is returned.
         */
        MoviesLocation,

        /**
         * Returns the directory containing the user's pictures or photos. This is a generic value.
         * If no directory specific for picture files exists, a sensible fallback for storing user documents is returned.
         */
        PicturesLocation,

        /**
         * Returns a directory where temporary files can be stored. The returned value might be application-specific,
         * shared among other applications for this user, or even system-wide.
         * The returned path is never empty.
         */
        TempLocation,

        /**
         * Returns the user's home directory. On Unix systems, this is equal to the
         * HOME environment variable. This value might be generic or application-specific,
         * but the returned path is never empty.
         */
        HomeLocation,

        /**
         * Returns the same value as AppLocalDataLocation. This enumeration value is deprecated.
         * Using AppDataLocation is preferable since on Windows, the roaming path is recommended.
         */
        DataLocation,

        /**
         * Returns a directory location where user-specific non-essential (cached) data should be written.
         * This is an application-specific directory. The returned path is never empty.
         */
        CacheLocation,

        /**
         * Returns a directory location where persistent data shared across applications can be stored.
         * This is a generic value. The returned path is never empty.
         */
        GenericDataLocation,

        /**
         * Returns a directory location where runtime communication files should be written, like Unix local sockets.
         * This is a generic value. The returned path may be empty on some systems.
         */
        RuntimeLocation,

        /**
         * Returns a directory location where user-specific configuration files should be written.
         * This may be either a generic value or application-specific, and the returned path is never empty.
         */
        ConfigLocation,

        /**
         * Returns a directory for user's downloaded files. This is a generic value.
         * If no directory specific for downloads exists, a sensible fallback for storing user documents is returned.
         */
        DownloadLocation,

        /**
         * Returns a directory location where user-specific non-essential (cached) data, shared across applications,
         * should be written. This is a generic value.
         * Note that the returned path may be empty if the system has no concept of shared cache.
         */
        GenericCacheLocation,

        /**
         * Returns a directory location where user-specific configuration files shared between multiple applications
         * should be written. This is a generic value and the returned path is never empty.
         */
        GenericConfigLocation,

        /**
         * Returns a directory location where persistent application data can be stored.
         * This is an application-specific directory.
         * To obtain a path to store data to be shared with other applications, use QStandardPaths::GenericDataLocation.
         * The returned path is never empty. On the Windows operating system, this returns the roaming path.
         */
        AppDataLocation,

        /**
         * Returns the local settings path on the Windows operating system.
         * On all other platforms, it returns the same value as AppDataLocation.
         */
        AppLocalDataLocation,

        /**
         * Returns a directory location where user-specific configuration files should be written.
         * This is an application-specific directory, and the returned path is never empty.
         */
        AppConfigLocation
    }

    @JvmStatic
    private val delegate: StandardPathsDelegate = when (OperationSystem.currentOperationSystem) {
        OperationSystem.Windows -> WindowsPathsImpl()
        OperationSystem.MacOS -> MacPathsImpl()
        OperationSystem.Linux -> LinuxPathsImpl()
        OperationSystem.Other -> throw IllegalArgumentException("StandardPaths is unsupported for unknown OS'es")
    }

    /**
     * Returns the directory where files of type should be written to, or an empty string if the location
     * cannot be determined.
     *
     * Note: The storage location returned can be a directory that does not exist; i.e.,
     * it may need to be created by the system or the user.
     * @param location location
     * @return path to the location
     */
    @JvmStatic
    fun writableLocation(location: Location): Path = delegate.writableLocation(location)

    /**
     * Returns all the directories where files of type belong.
     *
     * The list of directories is sorted from high to low priority, starting with writableLocation() if
     * it can be determined.
     *
     * This list is empty if no locations for type are defined.
     * @param location location
     * @return the list of possible paths, sorted from high to low priority
     */
    @JvmStatic
    fun standardLocations(location: Location): List<Path> = delegate.standardLocations(location)
}

private interface StandardPathsDelegate {
    fun writableLocation(location: StandardPaths.Location): Path
    fun standardLocations(location: StandardPaths.Location): List<Path>
}

private class WindowsPathsImpl : StandardPathsDelegate {
    companion object {
        /**
         * Maps location enum to system GUID
         */
        private val writableSpecialFolderId: Map<StandardPaths.Location, Guid.GUID> =
                mapOf(
                        StandardPaths.Location.DesktopLocation to KnownFolders.FOLDERID_Desktop,
                        StandardPaths.Location.DocumentsLocation to KnownFolders.FOLDERID_Documents,
                        StandardPaths.Location.FontsLocation to KnownFolders.FOLDERID_Fonts,
                        StandardPaths.Location.ApplicationsLocation to KnownFolders.FOLDERID_Programs,
                        StandardPaths.Location.MusicLocation to KnownFolders.FOLDERID_Music,
                        StandardPaths.Location.MoviesLocation to KnownFolders.FOLDERID_Videos,
                        StandardPaths.Location.PicturesLocation to KnownFolders.FOLDERID_Pictures,
                        StandardPaths.Location.TempLocation to Guid.GUID(),
                        StandardPaths.Location.HomeLocation to Guid.GUID(),
                        StandardPaths.Location.DataLocation to KnownFolders.FOLDERID_LocalAppData,
                        StandardPaths.Location.AppLocalDataLocation to KnownFolders.FOLDERID_LocalAppData,
                        StandardPaths.Location.CacheLocation to Guid.GUID(),
                        StandardPaths.Location.GenericDataLocation to KnownFolders.FOLDERID_LocalAppData,
                        StandardPaths.Location.RuntimeLocation to Guid.GUID(),
                        StandardPaths.Location.ConfigLocation to KnownFolders.FOLDERID_LocalAppData,
                        StandardPaths.Location.DownloadLocation to Guid.GUID(),
                        StandardPaths.Location.GenericCacheLocation to Guid.GUID(),
                        StandardPaths.Location.GenericConfigLocation to KnownFolders.FOLDERID_LocalAppData,
                        StandardPaths.Location.AppDataLocation to KnownFolders.FOLDERID_RoamingAppData,
                        StandardPaths.Location.AppConfigLocation to KnownFolders.FOLDERID_LocalAppData
                )

        private fun isGenericConfigLocation(type: StandardPaths.Location): Boolean =
                type == StandardPaths.Location.GenericConfigLocation || type == StandardPaths.Location.GenericDataLocation

        private fun isConfigLocation(type: StandardPaths.Location): Boolean = type == StandardPaths.Location.ConfigLocation ||
                type == StandardPaths.Location.AppConfigLocation ||
                type == StandardPaths.Location.AppDataLocation ||
                type == StandardPaths.Location.AppLocalDataLocation ||
                isGenericConfigLocation(type)

        /**
         * WinAPI call wrapper
         *
         * @param guid the special identified of the system forlder
         * @return path to the folder
         */
        private fun sHGetKnownFolderPath(guid: Guid.GUID): String {
            val ptr = PointerByReference()
            try {
                Shell32.INSTANCE.SHGetKnownFolderPath(guid, ShlObj.KNOWN_FOLDER_FLAG.DONT_VERIFY.flag, null, ptr)
                val sliced = ptr.value.getCharArray(0, WinDef.MAX_PATH).takeWhile { it != '\u0000' }.toCharArray()
                return String(sliced)
            } finally {
                Native.free(Pointer.nativeValue(ptr.value))
            }
        }

        /**
         * Special version which returns String object to ease porting
         *
         * @param location location of the folder
         * @return path to the folder
         */
        private fun writableLocationStr(location: StandardPaths.Location): String {
            var result: String
            when (location) {
                StandardPaths.Location.DownloadLocation -> {
                    result =
                        sHGetKnownFolderPath(KnownFolders.FOLDERID_Downloads)
                    if (result.isEmpty()) result =
                        writableLocationStr(StandardPaths.Location.DocumentsLocation)
                }
                StandardPaths.Location.CacheLocation -> {
                    // Although Microsoft has a Cache key it is a pointer to IE's cache, not a cache
                    // location for everyone.  Most applications seem to be using a
                    // cache directory located in their AppData directory
                    result =
                        sHGetKnownFolderPath(writableSpecialFolderId[StandardPaths.Location.AppLocalDataLocation]!!)
                    if (result.isNotEmpty()) {
                        result = appendOrganizationAndApp(result)
                        result += "/cache"
                    }
                }
                StandardPaths.Location.GenericCacheLocation -> {
                    result =
                        sHGetKnownFolderPath(writableSpecialFolderId[StandardPaths.Location.GenericDataLocation]!!)
                    if (result.isNotEmpty()) result += "/cache"
                }
                StandardPaths.Location.RuntimeLocation, StandardPaths.Location.HomeLocation -> result =
                    homePath()
                StandardPaths.Location.TempLocation -> result =
                    tempPath()
                else -> {
                    result =
                        sHGetKnownFolderPath(writableSpecialFolderId[location]!!)
                    if (result.isNotEmpty() && isConfigLocation(location) && !isGenericConfigLocation(
                            location
                        )
                    )
                        result = appendOrganizationAndApp(result)
                }
            }
            return result
        }

        /**
         * Append application name and organization
         */
        private fun appendOrganizationAndApp(path: String): String {
            var result = path
            // TODO what org should be used here?
            val appName: String? = System.getProperty("sun.java.command")
            appName?.isNotEmpty().let { result += "/$appName" }
            return result
        }

        /**
         * Home location
         */
        private fun homePath(): String {
            var ret = ""

            // First method
            val hnd: WinNT.HANDLE = Kernel32.INSTANCE.GetCurrentProcess()
            val token = WinNT.HANDLEByReference()
            val processOpened = Advapi32.INSTANCE.OpenProcessToken(hnd, WinNT.TOKEN_QUERY, token)

            if (processOpened) {
                val szPtr = IntByReference(0)
                // passing null -> function fails and sizePtr contains the required buffer size
                val sizeQueryOk = Userenv.INSTANCE.GetUserProfileDirectoryW(token.value, null, szPtr)
                if (!sizeQueryOk && szPtr.value != 0) {
                    val buf = CharArray(szPtr.value)
                    val tempDirQueryOk = Userenv.INSTANCE.GetUserProfileDirectoryW(token.value, buf, szPtr)
                    if (tempDirQueryOk) {
                        ret = String(buf)
                        // buf is returned with \0 escape symbol
                        if (ret.endsWith('\u0000')) {
                            ret = ret.dropLast(1)
                        }
                    }
                }
            }

            // backup fallback
            if (ret.isEmpty() || !Paths.get(ret).toFile().exists()) {
                ret = System.getenv("USERPROFILE")
                if (ret.isEmpty() || !Paths.get(ret).toFile().exists()) {
                    ret = System.getenv("HOMEDRIVE") + System.getenv("HOMEPATH")
                    if (ret.isEmpty() || !Paths.get(ret).toFile().exists()) {
                        ret = System.getenv("HOME")
                        if (ret.isEmpty() || !Paths.get(ret).toFile().exists()) {
                            ret = rootPath()
                        }
                    }
                }
            }

            return ret
        }

        /**
         * System Drive location
         */
        private fun rootPath(): String {
            var ret = System.getenv("SystemDrive")
            if (ret.isEmpty()) ret = "C:/"
            return ret
        }

        /**
         * Locate temp directory
         */
        private fun tempPath(): String {
            var ret = ""
            val maxPathLen = WinDef.MAX_PATH + 1
            val tempPath = CharArray(maxPathLen)
            val len = Kernel32.INSTANCE.GetTempPath(WinDef.DWORD(maxPathLen.toLong()), tempPath)

            if (len.toInt() > 0) {
                // TODO GetTempPath() can return short names (on Win7?), expand?
                ret = String(tempPath.take(len.toInt()).toCharArray())
            }

            return when {
                ret.isEmpty() -> "C:/tmp"
                // capital disk letter
                ret.length >= 2 && ret[1] == ':' -> ret[0].toUpperCase() + ret.substring(1)
                else -> ret
            }
        }
    }

    override fun standardLocations(location: StandardPaths.Location): List<Path> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun writableLocation(location: StandardPaths.Location): Path {
        val strPath = writableLocationStr(location)
        return Paths.get(strPath)
    }
}

private class LinuxPathsImpl : StandardPathsDelegate {
    override fun standardLocations(location: StandardPaths.Location): List<Path> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun writableLocation(location: StandardPaths.Location): Path {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

private class MacPathsImpl : StandardPathsDelegate {
    override fun standardLocations(location: StandardPaths.Location): List<Path> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun writableLocation(location: StandardPaths.Location): Path {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
