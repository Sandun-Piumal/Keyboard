package com.spmods.sinkey

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.spmods.sinkey.data.PreferencesManager
import com.spmods.sinkey.data.ThemeMode
import com.spmods.sinkey.ui.screens.HomeScreen
import com.spmods.sinkey.ui.screens.SettingsScreen
import com.spmods.sinkey.ui.screens.ThemesScreen
import com.spmods.sinkey.ui.theme.SinKeyTheme
import kotlinx.coroutines.launch

private enum class Tab(val label: String) { HOME("Home"), THEMES("Themes"), SETTINGS("Settings") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(applicationContext)

        setContent {
            val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SinKeyTheme(themeMode = themeMode) {
                // The status bar otherwise stays the launcher's default grey since
                // nothing tells it to follow our Material color scheme. Push the
                // background color + matching icon tint into the window each time
                // the theme changes.
                val statusBarColor = MaterialTheme.colorScheme.background.toArgb()
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = statusBarColor
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    SinKeyApp(prefs)
                }
            }
        }
    }
}

@Composable
private fun SinKeyApp(prefs: PreferencesManager) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    val scope = rememberCoroutineScope()

    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val defaultLanguage by prefs.defaultLanguage.collectAsState(initial = "si")
    val keySoundEnabled by prefs.keySoundEnabled.collectAsState(initial = true)
    val keyVibrateEnabled by prefs.keyVibrateEnabled.collectAsState(initial = false)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.HOME,
                    onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == Tab.THEMES,
                    onClick = { tab = Tab.THEMES },
                    icon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    label = { Text("Themes") }
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS,
                    onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding), color = MaterialTheme.colorScheme.background) {
            when (tab) {
                Tab.HOME -> HomeScreen()
                Tab.THEMES -> ThemesScreen(
                    currentMode = themeMode,
                    onSelect = { mode -> scope.launch { prefs.setThemeMode(mode) } }
                )
                Tab.SETTINGS -> SettingsScreen(
                    defaultLanguage = defaultLanguage,
                    keySoundEnabled = keySoundEnabled,
                    keyVibrateEnabled = keyVibrateEnabled,
                    themeMode = themeMode,
                    onLanguageChange = { lang -> scope.launch { prefs.setDefaultLanguage(lang) } },
                    onKeySoundChange = { enabled -> scope.launch { prefs.setKeySoundEnabled(enabled) } },
                    onKeyVibrateChange = { enabled -> scope.launch { prefs.setKeyVibrateEnabled(enabled) } },
                    onThemeModeChange = { mode -> scope.launch { prefs.setThemeMode(mode) } }
                )
            }
        }
    }
}
