package com.spmods.sinkey.ui.screens

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// ── Screenshot-matched palette (indigo/purple → pink) ──────────────────────
private val IndigoDeep = Color(0xFF6C4CE0)
private val IndigoMid = Color(0xFF7C5CF0)
private val PinkAccent = Color(0xFFE0498A)
private val TitleIndigo = Color(0xFF3B2F8C)
private val BodyGrey = Color(0xFF6B7280)
private val StepCardBg = Color(0xFFFFFFFF)
private val FeatureStripBg = Color(0xFFF3F1FB)
private val OutlineIndigo = Color(0xFF6C4CE0)
private val OutlinePink = Color(0xFFE0498A)

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 24.dp)
    ) {
        // ── Top bar: hamburger · title · crown badge ────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 18.dp, 20.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text(
                        "SinKey ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TitleIndigo
                    )
                    Text(
                        "Board",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PinkAccent
                    )
                }
                Text(
                    "Type Smart. Type Easy. Type SinKey.",
                    fontSize = 12.sp,
                    color = BodyGrey,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0EBFB))
                    .border(1.dp, Color(0xFFE1D8F7), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = IndigoDeep,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Hero welcome card ───────────────────────────────────────────
        val heroGradient = Brush.linearGradient(
            colors = listOf(Color(0xFFE9E4FB), Color(0xFFF3E7F2), Color(0xFFFBE7EE)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )

        Card(
            modifier = Modifier
                .padding(20.dp, 14.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroGradient)
            ) {
                // Two-column layout: text + CTA on the left, illustration
                // on the right — matches the original wide, short hero
                // card. Row height is driven by content (max of the two
                // columns), so the card stays compact.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                    Text(
                        "WELCOME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = IndigoDeep
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Type your world",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E1B33)
                    )
                    Row {
                        Text(
                            "in ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1B33)
                        )
                        Text(
                            "Sinhala ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IndigoDeep
                        )
                        Text(
                            "or ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1B33)
                        )
                        Text(
                            "English",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PinkAccent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Switch anytime. Type naturally.",
                        fontSize = 13.sp,
                        color = BodyGrey
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(IndigoDeep, IndigoMid)
                                )
                            )
                            .clickable {
                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            }
                            .padding(horizontal = 22.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Start Typing",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    }

                    // Illustration — right column, shrunk and pulled up
                    // to sit level with the heading/subtitle, matching
                    // the original reference image.
                    Box(
                        modifier = Modifier
                            .padding(top = 30.dp)
                            .width(120.dp)
                            .height(96.dp)
                    ) {
                    // sparkles above the keyboard
                    Text(
                        "✦",
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = (-30).dp, y = 2.dp)
                    )
                    Text(
                        "✦",
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 11.dp, y = (-2).dp)
                    )

                    // keyboard body with keys
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(118.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFB08CF0), Color(0xFF7C4FE0))
                                )
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(6) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFC6B3F5).copy(alpha = 0.85f))
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFC6B3F5).copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(3f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFC6B3F5).copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFC6B3F5).copy(alpha = 0.85f))
                            )
                        }
                    }

                    // white "සිංහල" bubble — overlaps the keyboard's top-left corner,
                    // spilling out past the illustration box to the left (as in
                    // the reference, where it sits beside the headline text)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-38).dp, y = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "සිංහල",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF241C3D)
                            )
                        }
                        // speech-bubble tail — sits left-of-center under
                        // the bubble, pointing down at the keyboard
                        // (matches the reference exactly)
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(9.dp)
                                .offset(y = (-4).dp)
                                .rotate(45f)
                                .clip(RoundedCornerShape(bottomStart = 3.dp))
                                .background(Color.White)
                        )
                    }

                    // pink "A" bubble — overlaps the keyboard's top-right corner
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = 26.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF6B8D6))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "A",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF241C3D)
                            )
                        }
                        // speech-bubble tail — bottom-left corner,
                        // pointing down toward the keyboard
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .offset(y = (-4).dp)
                                .rotate(45f)
                                .clip(RoundedCornerShape(bottomStart = 3.dp))
                                .background(Color(0xFFF6B8D6))
                        )
                    }

                    // small decorative dots, bottom-left of the keyboard
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-10).dp, y = (-2).dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC6B3F5))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-3).dp, y = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC6B3F5))
                    )
                    }
                }
            }
        }

        // ── "Let's get you started" ─────────────────────────────────────
        Text(
            "Let's get you started",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(20.dp, 22.dp, 20.dp, 10.dp)
        )

        Card(
            modifier = Modifier
                .padding(20.dp, 0.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StepCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                SetupStepRow(
                    icon = Icons.Filled.Shield,
                    iconBg = Color(0xFFEDE8FC),
                    iconTint = IndigoDeep,
                    title = "1. Enable SinKey",
                    subtitle = "Turn on SinKey in system keyboard settings.",
                    actionLabel = "Enable Now",
                    actionOutline = OutlineIndigo,
                    done = enabled
                ) {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }

                HorizontalDivider(
                    color = Color(0xFFF0EEF6),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                SetupStepRow(
                    icon = Icons.Filled.Star,
                    iconBg = Color(0xFFFCE8F0),
                    iconTint = PinkAccent,
                    title = "2. Set as Default Keyboard",
                    subtitle = "Choose SinKey as your default keyboard.",
                    actionLabel = "Set as Default",
                    actionOutline = OutlinePink,
                    done = isDefault
                ) {
                    val imm = context.getSystemService(InputMethodManager::class.java)
                    imm.showInputMethodPicker()
                }
            }
        }

        // ── Feature strip ────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 0.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FeatureStripBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureItem(
                    glyph = "සිං",
                    glyphColor = IndigoDeep,
                    bgColor = Color(0xFFEDE8FC),
                    title = "Sinhala Typing",
                    subtitle = "Easy & Natural"
                )
                FeatureItem(
                    glyph = "A",
                    glyphColor = PinkAccent,
                    bgColor = Color(0xFFFCE8F0),
                    title = "English Typing",
                    subtitle = "Fast & Smart"
                )
                FeatureItem(
                    icon = Icons.Filled.Palette,
                    glyphColor = IndigoDeep,
                    bgColor = Color(0xFFEDE8FC),
                    title = "Themes",
                    subtitle = "Style your keyboard"
                )
                FeatureItem(
                    icon = Icons.Filled.VerifiedUser,
                    glyphColor = PinkAccent,
                    bgColor = Color(0xFFFCE8F0),
                    title = "Privacy First",
                    subtitle = "100% Secure"
                )
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionOutline: Color,
    done: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 14.dp, 18.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (done) Color(0xFFE3F5EA) else iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF1E8A4C), modifier = Modifier.size(20.dp))
            } else {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B33))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = BodyGrey,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        if (done) {
            // Completed state: filled green pill with a checkmark, no
            // longer clickable — visually distinct from the pending
            // outline-only pill so the user can see the step registered.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE3F5EA))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF1E8A4C),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Done",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E8A4C)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, actionOutline.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable { onAction() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = actionOutline
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = actionOutline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    glyph: String? = null,
    icon: ImageVector? = null,
    glyphColor: Color,
    bgColor: Color,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = glyphColor, modifier = Modifier.size(22.dp))
            } else if (glyph != null) {
                Text(glyph, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = glyphColor)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1B33),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            subtitle,
            fontSize = 9.sp,
            color = BodyGrey,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
