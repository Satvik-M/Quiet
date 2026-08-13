package com.satvikm.quiet.ui.friction

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.satvikm.quiet.data.block.BlocklistRepository
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.service.GestureAccessibilityService
import com.satvikm.quiet.ui.settings.SettingsViewModel
import com.satvikm.quiet.ui.theme.QuietTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Started by [GestureAccessibilityService] on top of a just-launched
 * blocklisted app. "Continue" explicitly relaunches the target app (rather
 * than relying on finish() to reveal whatever was underneath) so behavior
 * doesn't depend on how the app's task happens to be ordered; "Close"
 * explicitly navigates Home.
 */
@AndroidEntryPoint
class FrictionActivity : ComponentActivity() {

    @Inject lateinit var blocklistRepository: BlocklistRepository

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (targetPackageName == null) {
            finish()
            return
        }

        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetPackageName, 0)).toString()
        } catch (e: Exception) {
            targetPackageName
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val fontFamilySetting by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
            val fontSizeSetting by settingsViewModel.fontSize.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            val fontFamily = when (fontFamilySetting) {
                AppFontFamily.SANS -> FontFamily.SansSerif
                AppFontFamily.MONOSPACE -> FontFamily.Monospace
            }

            QuietTheme(darkTheme = darkTheme, fontFamily = fontFamily, fontScale = fontSizeSetting.scale) {
                FrictionScreen(
                    packageName = targetPackageName,
                    label = label,
                    blocklistRepository = blocklistRepository,
                    onClose = ::goHomeAndFinish,
                    onContinue = { continueToApp(targetPackageName) },
                )
            }
        }
    }

    private fun goHomeAndFinish() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    private fun continueToApp(packageName: String) {
        GestureAccessibilityService.instance?.grantGrace(packageName)
        lifecycleScope.launch { blocklistRepository.recordOpen(packageName) }
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}
