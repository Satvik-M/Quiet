package com.satvikm.quiet.data.workprofile

import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, stateless wrapper over the real OS-level Work Profile (the one provisioned via
 * MDM/Android Enterprise), using the same [UserManager]/[LauncherApps] APIs Pixel Launcher
 * itself uses. Deliberately holds no persisted state of any kind — every query below reflects
 * live OS state at call time.
 */
@Singleton
class WorkProfileManager @Inject constructor(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
) {

    fun hasWorkProfile(): Boolean = userManager.userProfiles.size > 1

    fun personalUserHandle(): UserHandle = Process.myUserHandle()

    /**
     * The non-personal profile among [UserManager.userProfiles], confirmed to actually be a
     * managed (work) profile on API 33+ via [LauncherApps.getLauncherUserInfo] — this guards
     * against Private Space / cloned-app profiles on modern devices being misclassified as
     * "work". Below API 33 there's no such API to confirm with, so the plain "not personal"
     * heuristic is used instead.
     */
    fun workUserHandle(): UserHandle? {
        val personal = personalUserHandle()
        val candidate = userManager.userProfiles.firstOrNull { it != personal } ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val info = try {
                launcherApps.getLauncherUserInfo(candidate)
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not query LauncherUserInfo for $candidate", e)
                null
            }
            return if (info?.userType == UserManager.USER_TYPE_PROFILE_MANAGED) candidate else null
        }
        return candidate
    }

    fun isQuietModeEnabled(handle: UserHandle): Boolean = userManager.isQuietModeEnabled(handle)

    /** Pauses (freezes) the work profile's apps. No-op (returns false) below API 28. */
    fun pauseWorkApps(handle: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return userManager.requestQuietModeEnabled(true, handle)
    }

    /**
     * Resumes (unfreezes) the work profile's apps.
     *
     * Deviation from the original plan: the plan called for the `IntentSender`-based 3-arg
     * `UserManager.requestQuietModeEnabled(boolean, UserHandle, IntentSender)` overload on API
     * 30+, so the OS could drive a credential-confirmation flow and land back in this app.
     * That overload is not present in this project's actual compileSdk 37 android.jar (verified
     * via `javap` against both android-37.1 and android-34 platform jars — only the 2-arg
     * overload and a 3-arg `(boolean, UserHandle, int flags)` overload exist; the 3-arg one
     * takes [UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED], not an
     * `IntentSender`, and was added in API 31). Since a regular default-launcher app (not a
     * device/profile owner) cannot use the `IntentSender` overload's underlying system
     * permission anyway, this uses the real public overload available on API 31+, and falls
     * back to the plain 2-arg overload on API 28-30.
     *
     * On API 28-30, and on 31+ when a credential actually is required, this call returns false
     * without prompting the user — a real, documented platform limitation for non-profile-owner
     * launchers, not something this code can retry or work around. Callers should surface this
     * as a known limitation (e.g. "couldn't resume automatically, check system Settings") rather
     * than retrying or crashing. Below API 28, returns false immediately.
     */
    fun resumeWorkApps(handle: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return userManager.requestQuietModeEnabled(
                false,
                handle,
                UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED,
            )
        }
        return userManager.requestQuietModeEnabled(false, handle)
    }

    private companion object {
        const val TAG = "WorkProfileManager"
    }
}
