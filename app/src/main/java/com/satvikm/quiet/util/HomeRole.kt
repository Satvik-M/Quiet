package com.satvikm.quiet.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Never trust that a role-request or Settings round-trip succeeded; always
 * re-resolve who currently holds the HOME role.
 */
fun isDefaultLauncher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
    }
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

fun createChangeDefaultLauncherIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}
