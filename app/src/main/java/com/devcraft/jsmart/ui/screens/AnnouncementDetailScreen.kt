package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.AnnouncementRepository
import com.devcraft.jsmart.data.AnnouncementRow
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    announcement: AnnouncementRow,
    currentUser: AppUser,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var hasSeen by remember {
        mutableStateOf(announcement.seenBy?.contains(currentUser.id) == true)
    }

    val displayDate = remember(announcement.createdAt) {
        if (announcement.createdAt.isBlank()) ""
        else {
            try {
                val instant = Instant.parse(
                    announcement.createdAt.replace(" ", "T")
                        .let { if (!it.endsWith("Z")) "${it}Z" else it }
                )
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val dt = LocalDateTime.of(
                    localDateTime.year,
                    localDateTime.monthNumber,
                    localDateTime.dayOfMonth,
                    localDateTime.hour,
                    localDateTime.minute,
                    localDateTime.second
                )
                dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH))
            } catch (_: Exception) {
                announcement.createdAt
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcement", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Cream)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Priority chip
            val priorityConfig = when (announcement.priority?.lowercase()) {
                "high", "urgent" -> ErrorRed to Color.White
                "medium", "important" -> WarningAmber to CharcoalDark
                "low" -> SuccessGreen to Color.White
                else -> Color.Gray to Color.White
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = priorityConfig.first
            ) {
                Text(
                    text = announcement.priority ?: "Normal",
                    color = priorityConfig.second,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = announcement.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = CharcoalMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Posted by: ${announcement.postedBy}",
                    fontSize = 12.sp,
                    color = CharcoalMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = CharcoalMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayDate,
                    fontSize = 12.sp,
                    color = CharcoalMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = announcement.body,
                fontSize = 15.sp,
                color = CharcoalDark,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!hasSeen) {
                Button(
                    onClick = {
                        scope.launch {
                            val success = AnnouncementRepository.markAsSeen(
                                announcementId = announcement.id,
                                userId = currentUser.id
                            )
                            if (success) {
                                hasSeen = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Mark as Seen", color = Color.White)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("You've seen this", color = SuccessGreen, fontWeight = FontWeight.Medium)
                }
            }

            if (currentUser.isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                val seenCount = announcement.seenBy?.size ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Seen by $seenCount people",
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

