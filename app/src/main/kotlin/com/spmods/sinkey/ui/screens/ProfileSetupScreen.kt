package com.spmods.sinkey.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Calendar

private val SetupIndigo = Color(0xFF6C4CE0)
private val SetupPink = Color(0xFFE0498A)
private val SetupGrey = Color(0xFF6B7280)

/**
 * One-time required setup shown the first time the user opens My Profile,
 * before ProfileScreen itself. Blocks progress until first name, last
 * name, gender, and birthday are all filled in — see
 * PreferencesManager.profileSetupComplete, which is what actually decides
 * whether MainActivity shows this screen or ProfileScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onComplete: (firstName: String, lastName: String, gender: String, birthday: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") } // "male" or "female"

    val calendar = remember { Calendar.getInstance() }
    var birthDay by remember { mutableStateOf<Int?>(null) }
    var birthMonth by remember { mutableStateOf<Int?>(null) } // 1..12
    var birthYear by remember { mutableStateOf<Int?>(null) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val isFormValid = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        gender.isNotBlank() &&
        birthDay != null && birthMonth != null && birthYear != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(SetupIndigo, SetupPink))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Row {
            Text("Set up your ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text("Profile", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SetupPink)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Just a few details before you get started — this only takes a moment.",
            fontSize = 13.sp,
            color = SetupGrey
        )

        Spacer(Modifier.height(28.dp))

        FieldLabel("First name")
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Sandun") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("Last name")
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Piumal") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("Gender")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderOption(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Male,
                label = "Male",
                selected = gender == "male",
                onClick = { gender = "male" }
            )
            GenderOption(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Female,
                label = "Female",
                selected = gender == "female",
                onClick = { gender = "female" }
            )
        }

        Spacer(Modifier.height(16.dp))

        FieldLabel("Birthday")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DropdownField(
                modifier = Modifier.weight(1f),
                label = "Day",
                selectedText = birthDay?.toString(),
                options = (1..31).map { it.toString() },
                onSelect = { birthDay = it.toInt() }
            )
            DropdownField(
                modifier = Modifier.weight(1.4f),
                label = "Month",
                selectedText = birthMonth?.let { monthNames[it - 1] },
                options = monthNames,
                onSelect = { birthMonth = monthNames.indexOf(it) + 1 }
            )
            DropdownField(
                modifier = Modifier.weight(1f),
                label = "Year",
                selectedText = birthYear?.toString(),
                options = (calendar.get(Calendar.YEAR) - 10 downTo calendar.get(Calendar.YEAR) - 100).map { it.toString() },
                onSelect = { birthYear = it.toInt() }
            )
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFormValid) {
                        Brush.linearGradient(listOf(SetupIndigo, SetupPink))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF3A3450) else Color(0xFFE4E1EE),
                                if (isDark) Color(0xFF3A3450) else Color(0xFFE4E1EE)
                            )
                        )
                    }
                )
                .clickable(enabled = isFormValid) {
                    val birthday = "%04d-%02d-%02d".format(birthYear, birthMonth, birthDay)
                    scope.launch {
                        onComplete(firstName.trim(), lastName.trim(), gender, birthday)
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Save & Continue",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFormValid) Color.White else SetupGrey
            )
        }

        if (!isFormValid) {
            Spacer(Modifier.height(10.dp))
            Text(
                "All fields are required to continue.",
                fontSize = 11.sp,
                color = SetupGrey,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SetupGrey,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SetupIndigo,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = SetupIndigo
)

@Composable
private fun GenderOption(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) SetupIndigo.copy(alpha = if (isDark) 0.3f else 0.12f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) SetupIndigo else SetupGrey,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) SetupIndigo else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DropdownField(
    modifier: Modifier = Modifier,
    label: String,
    selectedText: String?,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = SetupGrey,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    selectedText ?: label,
                    fontSize = 13.sp,
                    color = if (selectedText != null) MaterialTheme.colorScheme.onSurface else SetupGrey,
                    maxLines = 1
                )
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
}
