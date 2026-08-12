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
) {
    val packageName: String get() = componentName.packageName
}
