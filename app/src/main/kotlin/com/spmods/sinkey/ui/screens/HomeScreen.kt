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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    val cardGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1A2744), Color(0xFF2C3E6B), Color(0xFF3D2060)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

        // Header
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

        // Welcome Card — full width with proper horizontal padding so shadow shows on both sides
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardGradient)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(58.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFE8D5C4), Color(0xFFD4B896))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SiK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A2744)
                    )
                }
            }
        }

        // Setup Steps
        Column(modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 0.dp)) {
            SetupStep(
                number = "1",
                icon = Icons.Filled.Settings,
                title = "Enable SinKey",
                subtitle = "Turn on in system keyboard settings",
                done = enabled,
                actionLabel = if (!enabled) "Enable" else null
            ) {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            SetupStep(
                number = "2",
                icon = Icons.Filled.List,
                title = "Set as Default Keyboard",
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
                .padding(16.dp, 20.dp, 16.dp, 0.dp)
                .fillMaxWidth()
        ) {
            Text(if (!enabled) "Enable SinKey keyboard" else "Set as default keyboard")
        }
    }
}

@Composable
private fun SetupStep(
    number: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    done: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon circle — dark navy with icon, like original screenshot
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (done) MaterialTheme.colorScheme.primary
                    else Color(0xFF1A2744)
                )
                .border(
                    1.5.dp,
                    if (done) Color.Transparent
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Step number badge
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
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
