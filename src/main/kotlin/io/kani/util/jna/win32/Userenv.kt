package io.kani.util.jna.win32

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * Interface to select calls from the Userenv.dll library on Windows.
 */
interface Userenv : StdCallLibrary {

    /**
     * Retrieves the environment variables for the specified user. This block
     * can then be passed to the CreateProcessAsUser function.
     *
     * @param lpEnvironment
     * environment pointer to populate
     * @param phToken
     * the token
     * @param inherit
     * inherit variables from this process
     * @return ok
     */
    fun CreateEnvironmentBlock(lpEnvironment: PointerByReference, phToken: WinNT.HANDLE, inherit: Boolean): Boolean

    /**
     * Frees environment variables created by the CreateEnvironmentBlock
     * function.
     *
     * @param lpEnvironment
     * Pointer to the environment block created by
     * CreateEnvironmentBlock. The environment block is an array of
     * null-terminated Unicode strings. The list ends with two nulls
     * (\0\0).
     * @return TRUE if successful; otherwise, FALSE. To get extended error
     * information, call GetLastError.
     */
    fun DestroyEnvironmentBlock(lpEnvironment: Pointer): Boolean

    /**
     * Retrieves the path to the root directory of the specified user's profile.
     *
     * @param phToken
     * A token for the user, which is returned by the LogonUser,
     * CreateRestrictedToken, DuplicateToken, OpenProcessToken, or
     * OpenThreadToken function. The token must have TOKEN_QUERY
     * access. For more information, see Access Rights for
     * Access-Token Objects.
     * @param lpProfileDir
     * A pointer to a buffer that, when this function returns
     * successfully, receives the path to the specified user's
     * profile directory.
     * @param lpcchSize
     * Specifies the size of the lpProfileDir buffer, in TCHARs.
     *
     * If the buffer specified by lpProfileDir is not large enough or
     * lpProfileDir is NULL, the function fails and this parameter
     * receives the necessary buffer size, including the terminating
     * null character.
     * @return TRUE if successful; otherwise, FALSE. To get extended error
     * information, call GetLastError.
     */
    fun GetUserProfileDirectoryW(phToken: WinNT.HANDLE, lpProfileDir: CharArray?, lpcchSize: IntByReference): Boolean

    companion object {
        val INSTANCE = Native.load("userenv", Userenv::class.java, W32APIOptions.UNICODE_OPTIONS) as Userenv
    }
}
