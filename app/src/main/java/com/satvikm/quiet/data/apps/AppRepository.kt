package com.satvikm.quiet.data.apps

import android.content.ComponentName
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.satvikm.quiet.domain.model.LaunchableApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val appDao: AppDao,
    private val overridesRepository: AppOverridesRepository,
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val apps: Flow<List<LaunchableApp>> = combine(
        appDao.observeAll(),
        overridesRepository.overrides,
    ) { entities, overrides ->
        val overrideById = overrides.associateBy { it.appId }
        entities.mapNotNull { entity ->
            val app = entity.toDomainOrNull() ?: return@mapNotNull null
            val override = overrideById[app.id] ?: return@mapNotNull app
            app.copy(customLabel = override.customLabel, isHidden = override.isHidden)
        }
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) =
            refreshPackageAsync(packageName, user)

        override fun onPackageChanged(packageName: String, user: UserHandle) =
            refreshPackageAsync(packageName, user)

        override fun onPackageRemoved(packageName: String, user: UserHandle) =
            refreshPackageAsync(packageName, user)

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            packageNames.forEach { refreshPackageAsync(it, user) }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            packageNames.forEach { refreshPackageAsync(it, user) }
        }
    }

    /** Call once, at app startup. */
    fun start() {
        launcherApps.registerCallback(callback)
        repositoryScope.launch { refreshAll() }
    }

    suspend fun refreshAll() {
        userManager.userProfiles.forEach { refreshForUser(it) }
    }

    private suspend fun refreshForUser(userHandle: UserHandle) {
        val serial = userManager.getSerialNumberForUser(userHandle)
        if (serial == -1L) return
        val entities = try {
            launcherApps.getActivityList(null, userHandle)
        } catch (e: SecurityException) {
            // The profile can go inaccessible (e.g. work profile paused)
            // between listing it and querying its activities.
            Log.w(TAG, "Could not query activities for user $userHandle", e)
            emptyList()
        }.map { info ->
            AppEntity(
                id = AppEntity.id(info.componentName.packageName, info.componentName.className, serial),
                packageName = info.componentName.packageName,
                className = info.componentName.className,
                userSerial = serial,
                label = info.label.toString(),
            )
        }
        appDao.replaceForUser(serial, entities)
    }

    private suspend fun refreshPackage(packageName: String, userHandle: UserHandle) {
        val serial = userManager.getSerialNumberForUser(userHandle)
        if (serial == -1L) return
        val entities = try {
            launcherApps.getActivityList(packageName, userHandle)
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not query activities for $packageName / $userHandle", e)
            emptyList()
        }.map { info ->
            AppEntity(
                id = AppEntity.id(packageName, info.componentName.className, serial),
                packageName = packageName,
                className = info.componentName.className,
                userSerial = serial,
                label = info.label.toString(),
            )
        }
        appDao.replacePackageForUser(packageName, serial, entities)
    }

    private fun refreshPackageAsync(packageName: String, userHandle: UserHandle) {
        repositoryScope.launch { refreshPackage(packageName, userHandle) }
    }

    fun launch(app: LaunchableApp, sourceBounds: Rect? = null) {
        launcherApps.startMainActivity(app.componentName, app.userHandle, sourceBounds, null)
    }

    private fun AppEntity.toDomainOrNull(): LaunchableApp? {
        val userHandle = userManager.getUserForSerialNumber(userSerial) ?: return null
        return LaunchableApp(
            componentName = ComponentName(packageName, className),
            userHandle = userHandle,
            label = label,
        )
    }

    private companion object {
        const val TAG = "AppRepository"
    }
}
