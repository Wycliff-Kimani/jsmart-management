package com.devcraft.jsmart.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.devcraft.jsmart.AuthRepository
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isAdmin: Boolean = false,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToGeofence: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val user = UserSession.get()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf(user?.fullName ?: "User") }
    var phone by remember { mutableStateOf(user?.phone ?: "+254 7XX XXX XXX") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var gender by remember { mutableStateOf(user?.gender ?: "") }
    var avatarUrl by remember { mutableStateOf(user?.avatarUrl) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploadingAvatar = true
                val userId = UserSession.get()?.id
                if (userId != null) {
                    val url = AuthRepository.uploadAvatar(userId, uri, context)
                    if (url != null) {
                        avatarUrl = url
                        snackbarHostState.showSnackbar("Photo updated!")
                    } else {
                        snackbarHostState.showSnackbar("Upload failed. Try again.")
                    }
                }
                isUploadingAvatar = false
            }
        }
    }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var editMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Password Section States
    var showPasswordSection by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSavingPassword by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Cream)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Cream)
                }
                Text(
                    "Profile & Settings",
                    color = Cream,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        if (editMode) {
                            val userId = UserSession.get()?.id
                            if (userId != null) {
                                isSaving = true
                                scope.launch {
                                    val success = AuthRepository.updateUserProfile(
                                        userId = userId,
                                        fullName = name,
                                        phone = phone,
                                        gender = gender
                                    )
                                    isSaving = false
                                    editMode = false
                                    if (success) {
                                        snackbarHostState.showSnackbar("Profile updated successfully")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save. Try again.")
                                    }
                                }
                            }
                        } else {
                            editMode = true
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TealLight,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (editMode) "Save" else "Edit", color = TealLight, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {

                // Avatar + Header Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = TealPrimary,
                                strokeWidth = 3.dp
                            )
                        } else if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .background(TealPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user?.displayInitial ?: "?",
                                    color = Cream,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Edit icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Cream, CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(TealPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = "Edit Avatar",
                                    tint = Cream,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Header Info
                    Text(
                        text = name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = CharcoalMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val departmentLabel = user?.displayDepartment.takeIf { !it.isNullOrBlank() }
                        ?: user?.role?.replaceFirstChar { it.uppercase() }
                        ?: "Staff"

                    Box(
                        modifier = Modifier
                            .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = departmentLabel,
                            color = TealPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                HorizontalDivider(color = SurfaceContainer)
                Spacer(modifier = Modifier.height(20.dp))

                // Personal Information
                SectionHeader("Personal Information")
                Spacer(modifier = Modifier.height(8.dp))

                ProfileField(
                    label = "Full Name",
                    value = name,
                    editable = editMode,
                    icon = Icons.Filled.Person,
                    onValueChange = { name = it }
                )
                ProfileField(
                    label = "Phone Number",
                    value = phone,
                    editable = editMode,
                    icon = Icons.Filled.Phone,
                    onValueChange = { phone = it }
                )
                ProfileField(
                    label = "Email",
                    value = email,
                    editable = false,
                    icon = Icons.Filled.Email,
                    onValueChange = {}
                )

                // Gender Field
                if (editMode) {
                    var expanded by remember { mutableStateOf(false) }
                    val options = listOf("Male", "Female", "Prefer not to say")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = OutlineColor.copy(alpha = 0.3f),
                                focusedContainerColor = SurfaceContainer,
                                unfocusedContainerColor = SurfaceContainer
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        gender = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (gender.isNotBlank()) {
                    ProfileField(
                        label = "Gender",
                        value = gender,
                        editable = false,
                        icon = Icons.Filled.Person,
                        onValueChange = {}
                    )
                }

                ProfileField(
                    label = "Role",
                    value = user?.role?.replaceFirstChar { it.uppercase() } ?: "Staff",
                    editable = false,
                    icon = Icons.Filled.Work,
                    onValueChange = {}
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Preferences
                SectionHeader("Preferences")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null,
                            tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Push Notifications", fontSize = 14.sp, color = CharcoalDark,
                            modifier = Modifier.weight(1f))
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Cream, checkedTrackColor = TealPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    onClick = onNavigateToNotifications
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null,
                            tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("View Notifications", fontSize = 14.sp, color = CharcoalDark,
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OutlineColor)
                    }
                }

                // Admin-only settings
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Admin Settings")
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                        onClick = onNavigateToGeofence
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null,
                                tint = TealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Geofence & Branch Settings", fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium, color = CharcoalDark)
                                Text("Set location pins and radius per branch",
                                    fontSize = 12.sp, color = CharcoalMedium)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OutlineColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Wifi, contentDescription = null,
                                tint = TealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Office WiFi SSID", fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium, color = CharcoalDark)
                                Text("JSmart-Office-2.4G", fontSize = 12.sp, color = CharcoalMedium)
                            }
                            if (editMode) {
                                Icon(Icons.Filled.Edit, contentDescription = null,
                                    tint = TealPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Change Password
                SectionHeader("Security")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    onClick = { showPasswordSection = !showPasswordSection }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null,
                            tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Change Password", fontSize = 14.sp, color = CharcoalDark,
                            modifier = Modifier.weight(1f))
                        Icon(
                            if (showPasswordSection) Icons.Filled.KeyboardArrowUp else Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = OutlineColor
                        )
                    }
                }

                if (showPasswordSection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(SurfaceContainer, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = TealPrimary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = OutlineColor.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = OutlineColor.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                when {
                                    newPassword.length < 8 -> {
                                        scope.launch { snackbarHostState.showSnackbar("Password must be at least 8 characters") }
                                    }
                                    newPassword != confirmPassword -> {
                                        scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                                    }
                                    else -> {
                                        isSavingPassword = true
                                        scope.launch {
                                            val success = AuthRepository.updatePassword(newPassword)
                                            if (success) {
                                                snackbarHostState.showSnackbar("Password updated")
                                                newPassword = ""
                                                confirmPassword = ""
                                                showPasswordSection = false
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to update password")
                                            }
                                            isSavingPassword = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            enabled = !isSavingPassword,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSavingPassword) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Cream, strokeWidth = 2.dp)
                            } else {
                                Text("Update Password", color = Cream, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "JSmart v1.0 • Developed by DevCraft Technologies",
                    fontSize = 11.sp,
                    color = CharcoalMedium.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    editable: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        if (editable) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, fontSize = 12.sp) },
                leadingIcon = { Icon(icon, contentDescription = null, tint = TealPrimary,
                    modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = OutlineColor.copy(alpha = 0.3f)
                )
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = TealPrimary,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(label, fontSize = 11.sp, color = CharcoalMedium)
                    Text(value, fontSize = 14.sp, color = CharcoalDark, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
