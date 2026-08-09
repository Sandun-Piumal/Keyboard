package com.spmods.sinkey

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.spmods.sinkey.data.KeyColorPalette
import com.spmods.sinkey.data.KeyEffect
import com.spmods.sinkey.data.PreferencesManager
import com.spmods.sinkey.data.ThemeMode
import com.spmods.sinkey.keyboard.KeyboardView
import com.spmods.sinkey.ui.screens.HomeScreen
import com.spmods.sinkey.ui.screens.KeyboardHeightScreen
import com.spmods.sinkey.ui.screens.SettingsScreen
import com.spmods.sinkey.ui.screens.ThemesScreen
import com.spmods.sinkey.ui.theme.SinKeyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DeshGreen = Color(0xFF1B5E37)

private enum class Tab { HOME, THEMES, SETTINGS }

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
    val isDark = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val defaultLanguage by prefs.defaultLanguage.collectAsState(initial = "si")
    val keySoundEnabled by prefs.keySoundEnabled.collectAsState(initial = true)
    val keyVibrateEnabled by prefs.keyVibrateEnabled.collectAsState(initial = false)
    val keyboardHeight by prefs.keyboardHeight.collectAsState(initial = 2f)
    val bottomSpaceEnabled by prefs.bottomSpaceEnabled.collectAsState(initial = true)
    val bottomSpaceSize by prefs.bottomSpaceSize.collectAsState(initial = 0f)
    val showKeyBorders by prefs.showKeyBorders.collectAsState(initial = true)
    val mixAutoSinhala by prefs.mixAutoSinhala.collectAsState(initial = false)
    val swipeTypingEnabled by prefs.swipeTypingEnabled.collectAsState(initial = false)
    val smoothImeTransition by prefs.smoothImeTransition.collectAsState(initial = true)
    val keyColorPalette by prefs.keyColorPalette.collectAsState(initial = KeyColorPalette.DEFAULT)
    val keyEffect by prefs.keyEffect.collectAsState(initial = KeyEffect.NONE)
    val customBackgroundUri by prefs.customBackgroundUri.collectAsState(initial = null)
    val backgroundStyle by prefs.backgroundStyle.collectAsState(initial = com.spmods.sinkey.data.BackgroundStyle.NONE)
    val materialYouEnabled by prefs.materialYouEnabled.collectAsState(initial = false)
    val typingAnimation by prefs.typingAnimation.collectAsState(initial = com.spmods.sinkey.data.TypingAnimation.NONE)
    val typingAnimationEmoji by prefs.typingAnimationEmoji.collectAsState(initial = "✨")
    val typingAnimationImageUri by prefs.typingAnimationImageUri.collectAsState(initial = null)
    val ledPattern by prefs.ledPattern.collectAsState(initial = com.spmods.sinkey.data.LedPattern.NONE)
    val ledIdleDimming by prefs.ledIdleDimming.collectAsState(initial = true)

    // ── "My themes" custom background: photo picker + preview decode ──────
    val context = LocalContext.current
    // Small in-memory preview bitmap for the Themes screen's "My themes"
    // tile — decoded off the main thread whenever customBackgroundUri
    // changes (including on first load, so the previously-picked photo
    // still shows after reopening the app). Separate from the keyboard's
    // own KeyboardCustomBackground decode/cache (KeyboardView is a
    // different composable tree, often not even composed while the app UI
    // is showing this screen), so this doesn't share or depend on that
    // cache.
    var customBackgroundPreview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(customBackgroundUri) {
        val uriString = customBackgroundUri
        customBackgroundPreview = if (uriString != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }

    // PickVisualMedia is the modern Android Photo Picker — needs no
    // READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE runtime permission on any API
    // level (it runs out-of-process and only hands back a scoped Uri for
    // whatever the user picks), which is why AndroidManifest.xml needed no
    // changes for this feature. ImageOnly's filter is image/*, which
    // already includes image/gif, so "Image / GIF Backgrounds" needs no
    // separate picker mode — KeyboardCustomBackground below sniffs the
    // picked file's bytes and animates it if it turns out to be a GIF.
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Persist read access across app/device restarts — without this,
        // the Uri stored in DataStore would throw SecurityException the
        // next time anything (this screen's preview decode, or the
        // keyboard's KeyboardCustomBackground) tries to open it after the
        // process that received it from the picker has died.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        scope.launch { prefs.setCustomBackgroundUri(uri.toString()) }
    }

    // ── "Typing Animation" DIY custom image: same picker/preview pattern
    // as the "My themes" background above, kept as a fully separate
    // Uri/preview pair since the two features are independent (a user can
    // have a custom background photo AND a custom typing-animation image
    // picked from two different photos).
    var typingAnimationImagePreview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(typingAnimationImageUri) {
        val uriString = typingAnimationImageUri
        typingAnimationImagePreview = if (uriString != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }
    val pickTypingAnimationImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        scope.launch { prefs.setTypingAnimationImageUri(uri.toString()) }
    }

    // ── Back press priority (highest → lowest) ───────────────────────────────
    // 1. Keyboard preview open  → close preview
    // 2. In a sub-screen        → go back to Settings main
    // 3. On Themes / Settings tab → go to Home tab
    // 4. On Home tab            → default (app minimize / close)

    if (showKeyboardPreview) {
        BackHandler { showKeyboardPreview = false }
    }

    if (settingsSubScreen == SettingsSubScreen.KEYBOARD_HEIGHT) {
        BackHandler { settingsSubScreen = SettingsSubScreen.MAIN }
    }

    if (tab != Tab.HOME && !showKeyboardPreview) {
        BackHandler { tab = Tab.HOME }
    }

    // ─────────────────────────────────────────────────────────────────────────

    Scaffold(
        floatingActionButton = {
            // FAB only visible when keyboard preview is NOT shown
            // (inside the preview itself there is no need for it)
            if (!showKeyboardPreview) {
                FloatingActionButton(
                    onClick = { showKeyboardPreview = true },
                    containerColor = DeshGreen,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Text("⌨", fontSize = 22.sp)
                }
            }
        },
        bottomBar = {
            if (settingsSubScreen == SettingsSubScreen.MAIN && !showKeyboardPreview) {
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
                    tab == Tab.HOME -> HomeScreen()
                    tab == Tab.THEMES -> ThemesScreen(
                        currentMode = themeMode,
                        onSelect = { mode -> scope.launch { prefs.setThemeMode(mode) } },
                        keyColorPalette = keyColorPalette,
                        onKeyColorPaletteChange = { palette -> scope.launch { prefs.setKeyColorPalette(palette) } },
                        keyEffect = keyEffect,
                        onKeyEffectChange = { effect -> scope.launch { prefs.setKeyEffect(effect) } },
                        customBackgroundPreview = customBackgroundPreview,
                        onPickCustomBackground = {
                            pickPhotoLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onClearCustomBackground = { scope.launch { prefs.setCustomBackgroundUri(null) } },
                        backgroundStyle = backgroundStyle,
                        onBackgroundStyleChange = { style -> scope.launch { prefs.setBackgroundStyle(style) } },
                        materialYouEnabled = materialYouEnabled,
                        onMaterialYouEnabledChange = { enabled -> scope.launch { prefs.setMaterialYouEnabled(enabled) } },
                        materialYouAvailable = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                        typingAnimation = typingAnimation,
                        onTypingAnimationChange = { anim -> scope.launch { prefs.setTypingAnimation(anim) } },
                        typingAnimationEmoji = typingAnimationEmoji,
                        onTypingAnimationEmojiChange = { emoji -> scope.launch { prefs.setTypingAnimationEmoji(emoji) } },
                        typingAnimationImagePreview = typingAnimationImagePreview,
                        onPickTypingAnimationImage = {
                            pickTypingAnimationImageLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onClearTypingAnimationImage = { scope.launch { prefs.setTypingAnimationImageUri(null) } },
                        ledPattern = ledPattern,
                        onLedPatternChange = { pattern -> scope.launch { prefs.setLedPattern(pattern) } },
                        ledIdleDimming = ledIdleDimming,
                        onLedIdleDimmingChange = { enabled -> scope.launch { prefs.setLedIdleDimming(enabled) } }
                    )
                    tab == Tab.SETTINGS -> SettingsScreen(
                        defaultLanguage = defaultLanguage,
                        keySoundEnabled = keySoundEnabled,
                        keyVibrateEnabled = keyVibrateEnabled,
                        themeMode = themeMode,
                        mixAutoSinhala = mixAutoSinhala,
                        swipeTypingEnabled = swipeTypingEnabled,
                        smoothImeTransition = smoothImeTransition,
                        onLanguageChange = { lang -> scope.launch { prefs.setDefaultLanguage(lang) } },
                        onKeySoundChange = { enabled -> scope.launch { prefs.setKeySoundEnabled(enabled) } },
                        onKeyVibrateChange = { enabled -> scope.launch { prefs.setKeyVibrateEnabled(enabled) } },
                        onThemeModeChange = { mode -> scope.launch { prefs.setThemeMode(mode) } },
                        onMixAutoSinhalaChange = { enabled -> scope.launch { prefs.setMixAutoSinhala(enabled) } },
                        onSwipeTypingChange = { enabled -> scope.launch { prefs.setSwipeTypingEnabled(enabled) } },
                        onSmoothImeTransitionChange = { enabled -> scope.launch { prefs.setSmoothImeTransition(enabled) } },
                        onOpenKeyboardHeight = { settingsSubScreen = SettingsSubScreen.KEYBOARD_HEIGHT }
                    )
                }
            }

            // ── Keyboard preview overlay ──────────────────────────────────
            AnimatedVisibility(
                visible = showKeyboardPreview,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Bug O6 Fix: Preview keyboard is display-only — no real
                // InputConnection exists here. Previously onKey was a no-op
                // lambda but the emoji picker inside KeyboardView could still
                // open, and selecting an emoji called prefs.addRecentEmoji()
                // via a PreferencesManager created with LocalContext (the
                // Activity context, not the IME context) which is fine, but
                // onSuggestionSelected was missing entirely, risking a NPE on
                // some Compose versions. Both callbacks are now explicit no-ops.
                // onDismiss is wired to close the preview — previously it was
                // passed but KeyboardView never called it (Bug N7); the FAB
                // BackHandler still handles dismiss for now.
                KeyboardView(
                    currentLanguage = defaultLanguage,
                    keyboardHeight = keyboardHeight,
                    bottomSpaceEnabled = bottomSpaceEnabled,
                    bottomSpaceSize = bottomSpaceSize,
                    showKeyBorders = showKeyBorders,
                    isDark = isDark,
                    suggestions = emptyList(),
                    onSuggestionSelected = { /* preview — no input dispatch */ },
                    onKey = { /* preview — no input dispatch */ },
                    onDismiss = { showKeyboardPreview = false }
                )
            }
        }
    }
}
