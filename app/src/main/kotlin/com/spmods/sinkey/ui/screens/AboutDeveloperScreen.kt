package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One social/contact link row's static data — see [DEVELOPER_LINKS] below. */
private data class DeveloperLink(
    val icon: String,
    val label: String,
    val url: String
)

/**
 * The developer's contact/social links, in the order they appear. A plain
 * emoji is used per platform rather than brand-specific vector icons
 * (Telegram/YouTube aren't in Material Icons, and pulling in their actual
 * logos would mean bundling third-party brand assets) — consistent with
 * how every other icon in Settings/About is a plain emoji already.
 */
private val DEVELOPER_LINKS = listOf(
    DeveloperLink("▶️", "YouTube", "https://youtube.com/@datahackerz?si=Xbnnev0jTDV5sRWY"),
    DeveloperLink("✈️", "Telegram — SPMods Sandun", "https://t.me/SPModsSandun"),
    DeveloperLink("✈️", "Telegram — DH WhatsApp Ultra", "https://t.me/dhwhatsappultra"),
    DeveloperLink("🌐", "Website", "https://www.spmods.download")
)

/**
 * "About developer" screen: avatar, name, and tappable links to the
 * developer's other channels/socials. [onOpenLink] receives the raw URL —
 * the caller (MainActivity) is responsible for turning that into an
 * ACTION_VIEW intent, same pattern as every other external-link launch in
 * this app (see KeyboardView.kt's link handling for the existing
 * precedent) — this screen has no Context/Intent dependency of its own.
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Male avatar — plain emoji rather than a bundled image
                // asset, same reasoning as DEVELOPER_LINKS' icons above:
                // no photo was provided, and an emoji avoids needing to
                // ship/maintain a placeholder image file.
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨", fontSize = 48.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Sandun Piumal", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Developer of SinKey",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DeveloperCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                DEVELOPER_LINKS.forEachIndexed { index, link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenLink(link.url) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(link.icon, fontSize = 15.sp)
                        }
                        Text(
                            link.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index != DEVELOPER_LINKS.lastIndex) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 64.dp)
                        )
                    }
                }
            }

            Text(
                "Thanks for using SinKey! 💚",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun DeveloperCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}
