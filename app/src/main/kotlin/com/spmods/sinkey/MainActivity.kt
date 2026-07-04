package com.spmods.sinkey

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.spmods.sinkey.data.PreferencesManager
import com.spmods.sinkey.data.ThemeMode
import com.spmods.sinkey.keyboard.KeyboardView
import com.spmods.sinkey.ui.screens.HomeScreen
import com.spmods.sinkey.ui.screens.KeyboardHeightScreen
import com.spmods.sinkey.ui.screens.SettingsScreen
import com.spmods.sinkey.ui.screens.ThemesScreen
import com.spmods.sinkey.ui.theme.SinKeyTheme
import kotlinx.coroutines.launch

private val DeshGreen = Color(0xFF1B5E37)

private enum class Tab(val label: String) { HOME("Home"), THEMES("Themes"), SETTINGS("Settings") }

// Separate navigation stack for sub-screens inside Settings
private enum class SettingsSubScreen { MAIN, KEYBOARD_HEIGHT }

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
    var settingsSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    var showKeyboardPreview by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val defaultLanguage by prefs.defaultLanguage.collectAsState(initial = "si")
    val keySoundEnabled by prefs.keySoundEnabled.collectAsState(initial = true)
    val keyVibrateEnabled by prefs.keyVibrateEnabled.collectAsState(initial = false)
    val keyboardHeight by prefs.keyboardHeight.collectAsState(initial = 1f)
    val bottomSpaceEnabled by prefs.bottomSpaceEnabled.collectAsState(initial = true)
    val bottomSpaceSize by prefs.bottomSpaceSize.collectAsState(initial = 0f)
    val showKeyBorders by prefs.showKeyBorders.collectAsState(initial = true)

    Scaffold(
        floatingActionButton = {
            // FAB is shown on all screens — tapping shows keyboard preview overlay
            FloatingActionButton(
                onClick = { showKeyboardPreview = !showKeyboardPreview },
                containerColor = DeshGreen,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Text("⌨", fontSize = 22.sp)
            }
        },
        bottomBar = {
            // Hide nav bar when inside a sub-screen so the back arrow is the only nav
            if (settingsSubScreen == SettingsSubScreen.MAIN) {
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when {
                    // Sub-screen: Keyboard height (inside Settings)
                    settingsSubScreen == SettingsSubScreen.KEYBOARD_HEIGHT -> {
                        KeyboardHeightScreen(
                            keyboardHeight = keyboardHeight,
                            bottomSpaceEnabled = bottomSpaceEnabled,
                            bottomSpaceSize = bottomSpaceSize,
                            showKeyBorders = showKeyBorders,
                            onKeyboardHeightChange = { v -> scope.launch { prefs.setKeyboardHeight(v) } },
                            onBottomSpaceEnabledChange = { v -> scope.launch { prefs.setBottomSpaceEnabled(v) } },
                            onBottomSpaceSizeChange = { v -> scope.launch { prefs.setBottomSpaceSize(v) } },
                            onShowKeyBordersChange = { v -> scope.launch { prefs.setShowKeyBorders(v) } },
                            onBack = { settingsSubScreen = SettingsSubScreen.MAIN }
                        )
                    }

                    // Main tabs
                    tab == Tab.HOME -> HomeScreen()
                    tab == Tab.THEMES -> ThemesScreen(
                        currentMode = themeMode,
                        onSelect = { mode -> scope.launch { prefs.setThemeMode(mode) } }
                    )
                    tab == Tab.SETTINGS -> SettingsScreen(
                        defaultLanguage = defaultLanguage,
                        keySoundEnabled = keySoundEnabled,
                        keyVibrateEnabled = keyVibrateEnabled,
                        themeMode = themeMode,
                        onLanguageChange = { lang -> scope.launch { prefs.setDefaultLanguage(lang) } },
                        onKeySoundChange = { enabled -> scope.launch { prefs.setKeySoundEnabled(enabled) } },
                        onKeyVibrateChange = { enabled -> scope.launch { prefs.setKeyVibrateEnabled(enabled) } },
                        onThemeModeChange = { mode -> scope.launch { prefs.setThemeMode(mode) } },
                        onOpenKeyboardHeight = { settingsSubScreen = SettingsSubScreen.KEYBOARD_HEIGHT }
                    )
                }
            }

            // ── Keyboard preview overlay (slides up from bottom) ──────────
            AnimatedVisibility(
                visible = showKeyboardPreview,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                KeyboardView(
                    currentLanguage = defaultLanguage,
                    keyboardHeight = keyboardHeight,
                    bottomSpaceEnabled = bottomSpaceEnabled,
                    bottomSpaceSize = bottomSpaceSize,
                    showKeyBorders = showKeyBorders,
                    onKey = { /* preview — no real input dispatch */ },
                    onDismiss = { showKeyboardPreview = false }
                )
            }
        }
    }
}
