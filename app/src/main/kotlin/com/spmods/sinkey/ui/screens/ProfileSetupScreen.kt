package com.spmods.sinkey.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.spmods.sinkey.R
import kotlinx.coroutines.launch
import java.util.Calendar

private val SetupIndigo = Color(0xFF5B2BEF)
private val SetupIndigoDeep = Color(0xFF34128F)
private val SetupPink = Color(0xFFE0498A)
private val SetupGrey = Color(0xFF6B7280)

/**
 * Shown two ways:
 * 1. First-time required setup (isEditMode = false) — before ProfileScreen
 *    itself, the first time the user opens My Profile. Blocks progress
 *    until first name, last name, gender, and birthday are all filled in —
 *    see PreferencesManager.profileSetupComplete, which is what actually
 *    decides whether MainActivity shows this screen or ProfileScreen.
 * 2. Editing an existing profile (isEditMode = true) — reached from
 *    ProfileScreen's header pencil icon or its "Edit Profile" menu row.
 *    Same form, pre-filled with the saved values, with "Save Changes"
 *    instead of "Save & Continue" and a back button instead of being a
 *    one-way gate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onComplete: suspend (firstName: String, lastName: String, gender: String, birthday: String) -> Unit,
    isEditMode: Boolean = false,
    initialFirstName: String = "",
    initialLastName: String = "",
    initialGender: String = "",
    initialBirthday: String = "", // "yyyy-MM-dd", matches PreferencesManager.profileBirthday
    onBack: (() -> Unit)? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var gender by remember { mutableStateOf(initialGender) } // "male" or "female"

    val calendar = remember { Calendar.getInstance() }
    val parsedInitialBirthday = remember(initialBirthday) {
        initialBirthday.split("-").takeIf { it.size == 3 }?.let { (y, m, d) ->
            Triple(y.toIntOrNull(), m.toIntOrNull(), d.toIntOrNull())
        }
    }
    var birthDay by remember { mutableStateOf(parsedInitialBirthday?.third) }
    var birthMonth by remember { mutableStateOf(parsedInitialBirthday?.second) } // 1..12
    var birthYear by remember { mutableStateOf(parsedInitialBirthday?.first) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val isFormValid = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        gender.isNotBlank() &&
        birthDay != null && birthMonth != null && birthYear != null

    val handleSave: () -> Unit = handleSave@{
        if (!isFormValid) return@handleSave
        val birthday = "%04d-%02d-%02d".format(birthYear, birthMonth, birthDay)
        scope.launch {
            onComplete(firstName.trim(), lastName.trim(), gender, birthday)
            if (isEditMode) {
                onBack?.invoke()
            }
        }
    }

    val p = remember(isDark) { homePalette(isDark) }
    val shape = RoundedCornerShape(50)

    // The page draws its own About-Developer background so it looks the same
    // wherever it is shown (first-time setup, onboarding overlay, edit mode).
    Box(modifier = Modifier.fillMaxSize()) {
        DeveloperBackground(dark = isDark)

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ───────────────────────────────────────────────────
            if (isEditMode && onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 12.dp, 16.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(8.dp, CircleShape, ambientColor = SetupIndigo, spotColor = SetupIndigo)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), SetupIndigoDeep)))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ManageAccounts, null, tint = p.iconIndigo, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                            Text(
                                "Profile",
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF5B2BEF), Color(0xFF9B4DFF))),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Text(
                            "Update your profile information",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = p.body,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(8.dp, CircleShape, ambientColor = p.glow.copy(alpha = 0.4f), spotColor = p.glow.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(
                                if (isDark) Brush.verticalGradient(listOf(Color(0xFF2E2560), Color(0xFF211A48)))
                                else Brush.verticalGradient(listOf(Color.White, Color(0xFFEAE0FF)))
                            )
                            .border(1.5.dp, if (isDark) SolidColor(p.cardBorder) else GlossyWhite, CircleShape)
                            .clickable(enabled = isFormValid) { handleSave() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = if (isFormValid) p.iconIndigo else p.sub,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Scrollable form ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (!isEditMode) {
                    Spacer(Modifier.height(18.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                            Text("Set up your ", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
                            Text(
                                "Profile",
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF5B2BEF), Color(0xFF9B4DFF))),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Just a few details before you get started — this only takes a moment.",
                            fontSize = 13.sp,
                            color = p.body
                        )
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                }

                // ── Avatar ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // little "sparkle" strokes either side of the avatar
                    Canvas(modifier = Modifier.size(width = 220.dp, height = 112.dp)) {
                        val c = SetupIndigo.copy(alpha = 0.55f)
                        val w = 3.dp.toPx()
                        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                            drawLine(c, Offset(x1.dp.toPx(), y1.dp.toPx()), Offset(x2.dp.toPx(), y2.dp.toPx()), w, StrokeCap.Round)
                        line(6f, 38f, 22f, 46f); line(0f, 58f, 16f, 60f); line(8f, 76f, 20f, 72f)
                        line(198f, 46f, 214f, 38f); line(204f, 62f, 220f, 64f); line(196f, 74f, 210f, 80f)
                    }
                    Box(modifier = Modifier.size(116.dp)) {
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .align(Alignment.Center)
                                .shadow(14.dp, CircleShape, ambientColor = SetupIndigo, spotColor = SetupIndigo)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF3A2E5C) else Color(0xFFD8CCF7))
                                .border(4.dp, Brush.linearGradient(listOf(Color(0xFF9B7BFF), Color(0xFF4A1FB8))), CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            when (gender) {
                                "male" -> Image(
                                    painter = painterResource(id = R.drawable.avatar_male),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                                "female" -> Image(
                                    painter = painterResource(id = R.drawable.avatar_female),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                                else -> Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = p.iconIndigo,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 0.dp, bottom = 2.dp)
                                .size(36.dp)
                                .shadow(8.dp, CircleShape, ambientColor = SetupIndigo, spotColor = SetupIndigo)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color(0xFF6A34F0), SetupIndigoDeep)))
                                .border(2.dp, Color.White.copy(alpha = if (isDark) 0.4f else 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, "Photo", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                // ── Names ────────────────────────────────────────────────
                FieldLabel(p, Icons.Filled.Person, "First name")
                GlassTextField(
                    p = p,
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = "e.g. Sandun"
                )

                Spacer(Modifier.height(10.dp))

                FieldLabel(p, Icons.Filled.Group, "Last name")
                GlassTextField(
                    p = p,
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = "e.g. Piumal"
                )

                Spacer(Modifier.height(12.dp))

                // ── Gender ───────────────────────────────────────────────
                FieldLabel(p, Icons.Filled.Male, "Gender")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GenderOption(
                        p = p,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Male,
                        label = "Male",
                        selected = gender == "male",
                        onClick = { gender = "male" }
                    )
                    GenderOption(
                        p = p,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Female,
                        label = "Female",
                        selected = gender == "female",
                        onClick = { gender = "female" }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Birthday ─────────────────────────────────────────────
                FieldLabel(p, Icons.Filled.CalendarMonth, "Birthday")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DropdownField(
                        p = p,
                        modifier = Modifier.weight(1f),
                        label = "Day",
                        selectedText = birthDay?.toString(),
                        options = (1..31).map { it.toString() },
                        onSelect = { birthDay = it.toInt() }
                    )
                    DropdownField(
                        p = p,
                        modifier = Modifier.weight(1.45f),
                        label = "Month",
                        selectedText = birthMonth?.let { monthNames[it - 1] },
                        options = monthNames,
                        onSelect = { birthMonth = monthNames.indexOf(it) + 1 }
                    )
                    DropdownField(
                        p = p,
                        modifier = Modifier.weight(1.1f),
                        label = "Year",
                        selectedText = birthYear?.toString(),
                        options = (calendar.get(Calendar.YEAR) - 10 downTo calendar.get(Calendar.YEAR) - 100).map { it.toString() },
                        onSelect = { birthYear = it.toInt() }
                    )
                }

                Spacer(Modifier.height(22.dp))

                // ── Save ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFormValid) Modifier.shadow(14.dp, shape, ambientColor = SetupIndigo, spotColor = SetupIndigo)
                            else Modifier
                        )
                        .clip(shape)
                        .background(
                            if (isFormValid) Brush.horizontalGradient(listOf(Color(0xFF3A1AA8), Color(0xFF6A34F0), Color(0xFFB02BD8)))
                            else Brush.horizontalGradient(
                                listOf(
                                    if (isDark) Color(0xFF3A3450) else Color(0xFFD9D2EE),
                                    if (isDark) Color(0xFF3A3450) else Color(0xFFD9D2EE)
                                )
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = if (isFormValid) 0.5f else 0.2f), shape)
                        .clickable(enabled = isFormValid) { handleSave() }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Save, null,
                        tint = if (isFormValid) Color.White else p.sub,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isEditMode) "Save Changes" else "Save & Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFormValid) Color.White else p.sub
                    )
                }

                if (!isFormValid) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "All fields are required to continue.",
                        fontSize = 12.sp,
                        color = p.sub,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun FieldLabel(p: HomePalette, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = p.iconIndigo, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = p.title)
    }
}

/** Glass pill text field: round person tile + bold text. */
@Composable
private fun GlassTextField(
    p: HomePalette,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(p, radius = 26, elevation = 6)
            .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (p.dark) Color(0xFF3A2F66) else Color(0xFFE9E0FC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, null, tint = p.iconIndigo, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.title),
            cursorBrush = SolidColor(SetupIndigo),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = p.sub)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun GenderOption(
    p: HomePalette,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .then(
                if (selected) Modifier
                    .shadow(12.dp, shape, ambientColor = SetupIndigo, spotColor = SetupIndigo)
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4A1FB8), Color(0xFF7C4DFF))))
                    .border(2.dp, Color.White.copy(alpha = 0.45f), shape)
                else Modifier.glassCard(p, radius = 26, elevation = 5)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color.White else p.iconIndigo,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) Color.White else p.title
        )
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, null, tint = SetupIndigo, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun DropdownField(
    p: HomePalette,
    modifier: Modifier = Modifier,
    label: String,
    selectedText: String?,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .glassCard(p, radius = 22, elevation = 5)
            .clickable { expanded = true }
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, null, tint = p.iconIndigo, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                selectedText ?: label,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selectedText != null) p.title else p.sub,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = p.iconIndigo, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
