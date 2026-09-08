package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.R

// ── Palette (matches the purple/indigo accent used across Profile/Settings) ──
private val DevIndigo    = Color(0xFF6C4CE0)
private val DevGrey      = Color(0xFF6B7280)

/** One row in the "Follow Us" section — a direct link or a bottom-sheet trigger. */
private data class FollowItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val label: String,
    val url: String?
)

private const val TELEGRAM_SANDUN_URL = "https://t.me/SPModsSandun"
private const val TELEGRAM_DHWA_URL = "https://t.me/dhwhatsappultra"

private val FOLLOW_ITEMS = listOf(
    FollowItem(Icons.Filled.Language, Color(0xFFE3DEFA), Color(0xFF5B4BDB), "Website", "https://www.spmods.download"),
    FollowItem(Icons.Filled.Send, Color(0xFFDCEEFC), Color(0xFF2AA9E0), "Telegram", null),
    FollowItem(Icons.Filled.PlayArrow, Color(0xFFFBE0E0), Color(0xFFE0362E), "YouTube", "https://youtube.com/@datahackerz?si=3ORYJfLTK2LeInBj")
)

/**
 * "About developer" screen: centered avatar/name/tagline header, a Follow Us
 * section (website, Telegram, YouTube), a skills grid, and contact details
 * (email + location). [onOpenLink] receives the raw URL/mailto — the caller
 * (MainActivity) turns that into an ACTION_VIEW intent, same pattern as every
 * other external-link launch in this app. Tapping "Telegram" opens a bottom
 * sheet listing both Telegram channels rather than linking directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDeveloperScreen(
    onOpenLink: (url: String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About developer", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Header: centered avatar, name, role, tagline ────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF29B6F6), Color(0xFF29B6F6).copy(alpha = 0f))
                            )
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_developer),
                        contentDescription = "Sandun Piumal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(148.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF29B6F6), CircleShape)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("Sandun Piumal", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Developer of SinKey",
                    fontSize = 15.sp,
                    color = DevGrey
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "\"Code  \u2022  Create  \u2022  Improve\"",
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = DevIndigo
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Follow Us ────────────────────────────────────────────────
            var showTelegramSheet by remember { mutableStateOf(false) }

            SectionCard {
                SectionHeader(
                    icon = Icons.Filled.Share,
                    iconBg = DevIndigo,
                    iconTint = Color.White,
                    title = "Follow Us"
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Stay updated with the latest apps, mods, and tutorials.",
                    fontSize = 13.sp,
                    color = DevGrey
                )
                Spacer(Modifier.height(14.dp))
                FOLLOW_ITEMS.forEachIndexed { index, item ->
                    FollowRow(
                        item = item,
                        onClick = {
                            if (item.url != null) onOpenLink(item.url) else showTelegramSheet = true
                        }
                    )
                    if (index != FOLLOW_ITEMS.lastIndex) {
                        Spacer(Modifier.height(14.dp))
                        androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }

            if (showTelegramSheet) {
                TelegramChannelsSheet(
                    onDismiss = { showTelegramSheet = false },
                    onOpenLink = onOpenLink
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Contact ──────────────────────────────────────────────────
            SectionCard {
                SectionHeader(
                    icon = Icons.Filled.Email,
                    iconBg = DevIndigo,
                    iconTint = Color.White,
                    title = "Contact"
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Feel free to reach out for collaboration or just to say hi!",
                    fontSize = 13.sp,
                    color = DevGrey
                )
                Spacer(Modifier.height(14.dp))
                ContactRow(
                    icon = Icons.Filled.Email,
                    label = "Email",
                    value = "spmodsofficial@gmail.com",
                    onClick = { onOpenLink("mailto:spmodsofficial@gmail.com") }
                )
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(14.dp))
                ContactRow(
                    icon = Icons.Filled.LocationOn,
                    label = "Location",
                    value = "Sri Lanka",
                    onClick = null
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                DividerLine()
                Text("\uD83D\uDC9C", fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
                DividerLine()
            }
            Text(
                "Thanks for using Sinkey! \uD83D\uDE0A",
                fontSize = 13.sp,
                color = DevGrey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FollowRow(
    item: FollowItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = item.iconTint, modifier = Modifier.size(18.dp))
        }
        Text(
            item.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = DevGrey,
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelegramChannelsSheet(
    onDismiss: () -> Unit,
    onOpenLink: (url: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            Text("Telegram channels", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose a channel to open",
                fontSize = 13.sp,
                color = DevGrey
            )
            Spacer(Modifier.height(16.dp))
            TelegramChannelRow(
                title = "SPMods Sandun",
                subtitle = "Apps, mods, and updates",
                onClick = {
                    onOpenLink(TELEGRAM_SANDUN_URL)
                    onDismiss()
                }
            )
            Spacer(Modifier.height(10.dp))
            TelegramChannelRow(
                title = "DH WhatsApp Ultra",
                subtitle = "WhatsApp mod releases",
                onClick = {
                    onOpenLink(TELEGRAM_DHWA_URL)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun TelegramChannelRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFDCEEFC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Send, contentDescription = null, tint = Color(0xFF2AA9E0), modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = DevGrey)
        }
        Icon(
            Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = DevGrey,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(1.dp)
            .background(DevIndigo.copy(alpha = 0.4f))
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DevIndigo.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DevIndigo, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, fontSize = 12.sp, color = DevGrey)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
