package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.sinkey.ui.theme.Gold
import com.spmods.sinkey.ui.theme.Maroon

/**
 * One tappable row destination the drawer can open. Each maps directly to
 * an existing state setter already used elsewhere in SinKeyApp
 * (settingsSubScreen / showProfile) — the drawer doesn't introduce any
 * new navigation state of its own, only shortcuts into the states that
 * already exist. See SinKeyApp's onDrawerItemClick wiring in
 * MainActivity.kt for what each of these actually does.
 */
enum class DrawerDestination { PROFILE, PERSONAL_DICTIONARY, QUICK_TEXT, ABOUT }

/**
 * Side drawer opened from AppHeader's hamburger icon (Icons.Filled.Menu —
 * previously present with no click handler at all). Home/Themes/Settings
 * stay on the bottom NavigationBar unchanged; this is deliberately for
 * DEEPER links that don't have their own bottom-tab slot: Profile,
 * Personal Dictionary, Quick Text, and About — see MainActivity's
 * SettingsSubScreen enum, which already treats these as standalone
 * destinations reachable independent of which bottom tab is selected.
 *
 * Styled with the app's own brand tokens (Gold/Maroon — see Theme.kt's
 * "warm cream / gold / maroon, lotus-and-brass" palette comment) rather
 * than default Material drawer colors, so it reads as part of this app
 * rather than generic scaffolding: a maroon header band under the brand
 * wordmark, gold-tinted row icons, and the existing theme's surface color
 * for the row list itself so light/dark mode both stay legible.
 */
@Composable
fun SinKeyDrawer(
    userDisplayName: String,
    onDestinationClick: (DrawerDestination) -> Unit,
    onDismiss: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        // ── Header band ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Maroon)
                .padding(20.dp, 28.dp, 20.dp, 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    // First letter of the user's name as a simple
                    // monogram avatar — matches the maroon/gold pairing
                    // rather than pulling in a photo picker just for the
                    // drawer header.
                    text = userDisplayName.trim().firstOrNull()?.uppercase() ?: "S",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Maroon
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (userDisplayName.isNotBlank()) userDisplayName else "Welcome",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Type Smart. Type Easy. Type SinKey.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // ── Destination rows ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DrawerRow(
                icon = Icons.Filled.Person,
                label = "Profile",
                onClick = { onDestinationClick(DrawerDestination.PROFILE); onDismiss() }
            )
            DrawerRow(
                icon = Icons.Filled.Book,
                label = "Personal Dictionary",
                onClick = { onDestinationClick(DrawerDestination.PERSONAL_DICTIONARY); onDismiss() }
            )
            DrawerRow(
                icon = Icons.Filled.TextSnippet,
                label = "Quick Text",
                onClick = { onDestinationClick(DrawerDestination.QUICK_TEXT); onDismiss() }
            )
            DrawerRow(
                icon = Icons.Filled.Info,
                label = "About",
                onClick = { onDestinationClick(DrawerDestination.ABOUT); onDismiss() }
            )
        }
    }
}

@Composable
private fun DrawerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Gold.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}
