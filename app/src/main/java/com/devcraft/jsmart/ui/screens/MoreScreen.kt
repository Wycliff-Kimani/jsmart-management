@file:Suppress("SpellCheckingInspection")
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.ui.theme.*
import com.devcraft.jsmart.AuthRepository
import com.devcraft.jsmart.UserSession
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState

@Composable
fun MoreScreen(
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToAnnouncements: () -> Unit = {}
) {
    val user = UserSession.get()
    val fullName = user?.fullName ?: "User"
    val initial = user?.displayInitial ?: "?"
    val subtitle = buildString {
        append(user?.displayDepartment?.ifBlank { null }
            ?: user?.role?.replaceFirstChar { it.uppercase() } ?: "Staff")
        val branch = user?.displayBranch
        if (!branch.isNullOrBlank()) append(" • $branch")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TealPrimary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(TealLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = TealPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(fullName, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = subtitle,
                        color = TealLight,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Staff options — everyone sees these
        SectionHeader("My Account")
        MoreItem(Icons.Filled.Campaign, "Announcements", Routes.ANNOUNCEMENTS, { onNavigateToAnnouncements() })
        MoreItem(Icons.Filled.Message, "Messages", Routes.CHAT, onNavigate)
        MoreItem(Icons.Filled.Person, "Profile & Settings", Routes.PROFILE, onNavigate)
        MoreItem(Icons.Filled.BarChart, "My Reports", Routes.MY_REPORTS, onNavigate)
        MoreItem(Icons.Filled.Notifications, "Notifications", Routes.NOTIFICATIONS, onNavigate)

        // Admin options — only admins see these
        if (isAdmin) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Admin Tools")
            if (user?.isSuperAdmin == true) {
                MoreItem(Icons.Filled.AdminPanelSettings, "All Conversations", Routes.SUPER_ADMIN_CHAT, onNavigate)
            }
            MoreItem(Icons.Filled.Dashboard, "Admin Dashboard", Routes.ADMIN_DASHBOARD, onNavigate)
            MoreItem(Icons.Filled.People, "Staff Management", Routes.STAFF_MANAGEMENT, onNavigate)
            MoreItem(Icons.Filled.Campaign, "Post Announcement", Routes.ANNOUNCEMENTS, onNavigate)
            MoreItem(Icons.Filled.AddTask, "Create & Assign Task", Routes.TASK_CREATION, onNavigate)
            MoreItem(Icons.Filled.EventAvailable, "Leave Approvals", Routes.LEAVE_APPROVAL, onNavigate)
            MoreItem(Icons.Filled.BarChart, "Full Reports", Routes.ADMIN_REPORTS, onNavigate)
            MoreItem(Icons.Filled.MyLocation, "Geofence Settings", Routes.GEOFENCE_SETTINGS, onNavigate)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val scope = rememberCoroutineScope()

        // Logout
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        AuthRepository.logout()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = ErrorRed, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = CharcoalMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun MoreItem(icon: ImageVector, label: String, route: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(route) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = CharcoalDark, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OutlineColor)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = SurfaceContainer)
}