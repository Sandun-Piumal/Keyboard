package com.spmods.sinkey.ui.screens

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spmods.sinkey.ui.theme.AccentGradient
import kotlinx.coroutines.delay

private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(InputMethodManager::class.java)
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isImeDefault(context: Context): Boolean {
    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return defaultIme?.startsWith(context.packageName) == true
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var enabled by remember { mutableStateOf(isImeEnabled(context)) }
    var isDefault by remember { mutableStateOf(isImeDefault(context)) }

    DisposableEffect(lifecycleOwner) {
        fun refresh() {
            enabled = isImeEnabled(context)
            isDefault = isImeDefault(context)
        }

        // ON_RESUME alone isn't enough: the "set default keyboard" picker is a
        // system overlay dialog, not a separate Activity, so the host Activity
        // never pauses/resumes when a choice is made there. Watching the two
        // underlying Settings.Secure keys directly (ENABLED_INPUT_METHODS,
        // DEFAULT_INPUT_METHOD) catches both that case and the "disable in
        // system Settings" case the instant either value changes.
        val handler = Handler(Looper.getMainLooper())
        val contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD), false, contentObserver
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false, contentObserver
        )

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            context.contentResolver.unregisterContentObserver(contentObserver)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    var showSinhala by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            showSinhala = !showSinhala
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Column(modifier = Modifier.padding(22.dp, 18.dp, 22.dp, 4.dp)) {
            Text(
                "SinKey Board",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Fast Sinhala & English typing, built for everyday conversations.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(22.dp, 14.dp, 22.dp, 0.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AccentGradient)
                .padding(20.dp)
        ) {
            AnimatedContent(
                targetState = showSinhala,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 })
                        .togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                },
                label = "greeting"
            ) { isSinhala ->
                Text(
                    text = if (isSinhala) "ආයුබෝවන්" else "Welcome",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                "Type naturally in Sinhala or English — switch anytime.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(modifier = Modifier.padding(22.dp, 22.dp, 22.dp, 0.dp)) {
            SetupStep(
                number = "1",
                title = "Enable SinKey",
                subtitle = "Turn on in system keyboard settings",
                done = enabled,
                actionLabel = if (!enabled) "Enable" else null
            ) {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            SetupStep(
                number = "2",
                title = "Set as default keyboard",
                subtitle = "Choose SinKey when typing",
                done = isDefault,
                actionLabel = if (enabled && !isDefault) "Set up" else null
            ) {
                val imm = context.getSystemService(InputMethodManager::class.java)
                imm.showInputMethodPicker()
            }
        }

        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
            modifier = Modifier
                .padding(22.dp, 20.dp, 22.dp, 0.dp)
                .fillMaxWidth()
        ) {
            Text(if (!enabled) "Enable SinKey keyboard" else "Set as default keyboard")
        }
    }
}

@Composable
private fun SetupStep(
    number: String,
    title: String,
    subtitle: String,
    done: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .padding(10.dp)
        ) {
            Text(
                if (done) "✓" else number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (done) Color.White else MaterialTheme.colorScheme.secondary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null) {
            Text(
                actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction() }
                    .padding(4.dp)
            )
        }
    }
}
