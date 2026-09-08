package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
private val GithubBg     = Color(0xFFEDE9F7)
private val GithubFg     = Color(0xFF1F2430)
private val TelegramBg   = Color(0xFFDCEEFC)
private val TelegramFg   = Color(0xFF2AA9E0)
private val WhatsAppBg   = Color(0xFFDFF5E3)
private val WhatsAppFg   = Color(0xFF33B24A)
private val MailBg       = Color(0xFFE3DEFA)
private val MailFg       = Color(0xFF5B4BDB)

/** One quick social/contact icon shown under the developer's name. */
private data class DeveloperSocial(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bg: Color,
    val fg: Color,
    val contentDescription: String,
    val url: String
)

private val DEVELOPER_SOCIALS = listOf(
    DeveloperSocial(Icons.Filled.Code, GithubBg, GithubFg, "GitHub", "https://github.com/"),
    DeveloperSocial(Icons.Filled.Send, TelegramBg, TelegramFg, "Telegram", "https://t.me/SPModsSandun"),
    DeveloperSocial(Icons.Filled.Person, WhatsAppBg, WhatsAppFg, "WhatsApp", "https://wa.me/"),
    DeveloperSocial(Icons.Filled.Email, MailBg, MailFg, "Email", "mailto:sandunpiumal123@gmail.com")
)

/** One skill/tool chip shown in the "My Skills" section. */
private data class DeveloperSkill(
    val emoji: String,
    val bg: Color,
    val label: String
)

private val DEVELOPER_SKILLS = listOf(
    DeveloperSkill("🩵", Color(0xFFDCEEFC), "Flutter"),
    DeveloperSkill("🔷", Color(0xFFE3DEFA), "Dart"),
    DeveloperSkill("🟨", Color(0xFFFCEFC2), "JavaScript"),
    DeveloperSkill("🔥", Color(0xFFFBE3D6), "Firebase"),
    DeveloperSkill("🎨", Color(0xFFF6DCEE), "UI/UX Design"),
    DeveloperSkill("🐙", Color(0xFFE9E9EE), "Git & GitHub"),
    DeveloperSkill("🔵", Color(0xFFDCEEFC), "VS Code"),
    DeveloperSkill("🅿️", Color(0xFFDDE3F7), "Photoshop")
)

/**
 * "About developer" screen: avatar/name/tagline header, quick social icons,
 * an About Me blurb, a skills grid, and contact details (email + location).
 * [onOpenLink] receives the raw URL/mailto — the caller (MainActivity) turns
 * that into an ACTION_VIEW intent, same pattern as every other external-link
 * launch in this app.
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

            // ── Header: avatar, name, role, quote, socials ──────────────
            Row(verticalAlignment = Alignment.Top) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_developer),
                    contentDescription = "Sandun Piumal",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Sandun Piumal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Developer & UI/UX Designer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = DevIndigo
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.badge_code),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\u201C Turning ideas into real apps \u201D",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = DevGrey
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DEVELOPER_SOCIALS.forEach { social ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(social.bg)
                                    .clickable { onOpenLink(social.url) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    social.icon,
                                    contentDescription = social.contentDescription,
                                    tint = social.fg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── About Me ─────────────────────────────────────────────────
            SectionCard {
                SectionHeader(
                    icon = Icons.Filled.Person,
                    iconBg = DevIndigo,
                    iconTint = Color.White,
                    title = "About Me"
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hi! I\u2019m Sandun Piumal, a passionate developer who loves building mobile apps and clean, modern UI designs. I enjoy turning creative ideas into real-world solutions and always strive to learn new technologies and improve my skills.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── My Skills ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DevIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("My Skills", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Technologies & Tools I work with",
                            fontSize = 12.sp,
                            color = DevGrey
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SkillFlowRows(skills = DEVELOPER_SKILLS)
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
                    value = "sandunpiumal123@gmail.com",
                    onClick = { onOpenLink("mailto:sandunpiumal123@gmail.com") }
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
                "Thanks for using my app! \uD83D\uDE0A",
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

/** Lays skill chips out in wrapping rows of three, matching the reference layout. */
@Composable
private fun SkillFlowRows(skills: List<DeveloperSkill>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        skills.chunked(3).forEach { rowSkills ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowSkills.forEach { skill ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(skill.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(skill.emoji, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(skill.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
