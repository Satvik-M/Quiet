package com.satvikm.quiet.domain.model

import android.content.ComponentName
import android.os.UserHandle

/**
 * An activity a user can launch, identified by [componentName] *and*
 * [userHandle] together — the same component name can exist once per work
 * profile, and launching requires the matching handle.
 */
data class LaunchableApp(
    val componentName: ComponentName,
    val userHandle: UserHandle,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
) {
    val packageName: String get() = componentName.packageName

    /** Stable identity for persistence (favorites, hidden apps, etc.). */
    val id: String get() = "${componentName.flattenToString()}#$userHandle"

    /** What the UI should show: the user's rename if set, otherwise the system label. */
    val displayLabel: String get() = customLabel ?: label
}
