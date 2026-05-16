@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress("SpellCheckingInspection")
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.AnnouncementRepository
import com.devcraft.jsmart.data.AnnouncementRow
import com.devcraft.jsmart.data.AnnouncementSession
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun AnnouncementsScreen(currentUser: AppUser, navController: NavController) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("All Branches") }
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var announcements by remember { mutableStateOf<List<AnnouncementRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPosting by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf("Normal") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isAdmin = currentUser.role in listOf("admin", "super_admin")

    LaunchedEffect(Unit) {
        announcements = AnnouncementRepository.getAnnouncements()
        isLoading = false
    }

    val branches = listOf("All Branches", "Bypass Kamakis", "CBD Branch")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(innerPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Campaign, contentDescription = null,
                    tint = Cream, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Announcements", color = Cream,
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            if (isAdmin) {
                // Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = TealPrimary,
                    contentColor = Cream
                ) {
                    listOf("Post New", "Past Announcements").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(title, fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index)
                                        FontWeight.SemiBold else FontWeight.Normal)
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (isAdmin) {
                    when (selectedTab) {
                        0 -> PostAnnouncementTab(
                            title = title,
                            onTitleChange = { title = it },
                            body = body,
                            onBodyChange = { body = it },
                            selectedBranch = selectedBranch,
                            onBranchChange = { selectedBranch = it },
                            branches = branches,
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            selectedPriority = selectedPriority,
                            onPriorityChange = { selectedPriority = it },
                            isPosting = isPosting,
                            onPost = {
                                scope.launch {
                                    if (title.isBlank() || body.isBlank()) {
                                        snackbarHostState.showSnackbar("Title and message are required")
                                        return@launch
                                    }
                                    isPosting = true
                                    val branchId = when (selectedBranch) {
                                        "Bypass Kamakis" -> "965dc64f-9636-4062-88dc-a5c727f19245"
                                        "CBD Branch" -> "1ce1489b-8b5b-498e-9d6a-fad2d4000cbe"
                                        else -> null
                                    }
                                    val success = AnnouncementRepository.postAnnouncement(
                                        title = title,
                                        body = body,
                                        branchId = branchId,
                                        postedBy = currentUser.id
                                    )
                                    if (success) {
                                        title = ""
                                        body = ""
                                        selectedBranch = "All Branches"
                                        selectedPriority = "Normal"
                                        announcements = AnnouncementRepository.getAnnouncements()
                                        snackbarHostState.showSnackbar("Announcement posted successfully")
                                        selectedTab = 1
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to post. Please try again.")
                                    }
                                    isPosting = false
                                }
                            }
                        )
                        1 -> PastAnnouncementsTab(announcements, isLoading, navController)
                    }
                } else {
                    PastAnnouncementsTab(announcements, isLoading, navController)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun PostAnnouncementTab(
    title: String, onTitleChange: (String) -> Unit,
    body: String, onBodyChange: (String) -> Unit,
    selectedBranch: String, onBranchChange: (String) -> Unit,
    branches: List<String>,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    selectedPriority: String, onPriorityChange: (String) -> Unit,
    isPosting: Boolean,
    onPost: () -> Unit
) {
    Text("New Announcement", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(12.dp))

    // Branch selector
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedBranch,
            onValueChange = {},
            readOnly = true,
            label = { Text("Send To") },
            leadingIcon = {
                Icon(Icons.Filled.Group, contentDescription = null, tint = TealPrimary)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = { Text(branch) },
                    onClick = {
                        onBranchChange(branch)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Announcement Title") },
        placeholder = { Text("e.g. Public Holiday Notice") },
        leadingIcon = {
            Icon(Icons.Filled.Title, contentDescription = null, tint = TealPrimary)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = body,
        onValueChange = onBodyChange,
        label = { Text("Message") },
        placeholder = { Text("Write your announcement here...") },
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(12.dp),
        maxLines = 8
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Priority toggle
    Text("Priority Level", fontSize = 13.sp,
        fontWeight = FontWeight.Medium, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Normal", "Important", "Urgent").forEach { priority ->
            val isSelected = selectedPriority == priority
            val color = when (priority) {
                "Urgent" -> ErrorRed
                "Important" -> WarningAmber
                else -> TealPrimary
            }
            FilterChip(
                selected = isSelected,
                onClick = { onPriorityChange(priority) },
                label = { Text(priority, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color,
                    selectedLabelColor = Cream
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onPost,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
        enabled = !isPosting
    ) {
        if (isPosting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Cream)
        } else {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Cream)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Post Announcement", color = Cream,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PastAnnouncementsTab(announcements: List<AnnouncementRow>, isLoading: Boolean, navController: NavController) {
    Text("Previous Announcements", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }
    } else if (announcements.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No announcements yet", color = CharcoalMedium, textAlign = TextAlign.Center)
        }
    } else {
        announcements.forEach { announcement ->
            AnnouncementCard(announcement, navController)
        }
    }
}

@Composable
fun AnnouncementCard(announcement: AnnouncementRow, navController: NavController) {
    val priorityColor = when (announcement.priority) {
        "Urgent" -> ErrorRed
        "Important" -> WarningAmber
        else -> TealPrimary
    }

    val displayDate = remember(announcement.createdAt) {
        if (announcement.createdAt.isBlank()) ""
        else {
            try {
                val instant = Instant.parse(
                    announcement.createdAt.replace(" ", "T")
                        .let { if (!it.endsWith("Z")) "${it}Z" else it }
                )
                val localDateTime = instant.toLocalDateTime(
                    TimeZone.currentSystemDefault()
                )

                // Use java.time for formatting
                val dt = LocalDateTime.of(
                    localDateTime.year,
                    localDateTime.monthNumber,
                    localDateTime.dayOfMonth,
                    localDateTime.hour,
                    localDateTime.minute,
                    localDateTime.second
                )
                dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
            } catch (_: Exception) {
                announcement.createdAt.take(10)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                AnnouncementSession.current = announcement
                navController.navigate(Routes.ANNOUNCEMENT_DETAIL)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(announcement.title,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = CharcoalDark, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.12f)
                ) {
                    Text(announcement.priority ?: "Normal", color = priorityColor,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(announcement.body, fontSize = 12.sp,
                color = CharcoalMedium)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null,
                        tint = CharcoalMedium, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    val branchName = when (announcement.branchId) {
                        "965dc64f-9636-4062-88dc-a5c727f19245" -> "Bypass Kamakis"
                        "1ce1489b-8b5b-498e-9d6a-fad2d4000cbe" -> "CBD Branch"
                        else -> "All Branches"
                    }
                    Text(branchName, fontSize = 11.sp, color = CharcoalMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null,
                        tint = CharcoalMedium, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(displayDate, fontSize = 11.sp, color = CharcoalMedium)
                }
            }
        }
    }
}