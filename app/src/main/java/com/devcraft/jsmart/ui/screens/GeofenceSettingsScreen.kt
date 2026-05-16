@file:OptIn(ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devcraft.jsmart.ui.theme.*

@Composable
fun GeofenceSettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geofence Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        UnderConstructionContent(
            padding = padding,
            message = "Geofence settings will be available\nin a future update.",
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun UnderConstructionContent(
    padding: PaddingValues,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Construction,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = TealPrimary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Under Construction",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 15.sp,
            color = CharcoalMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TealPrimary
            ),
            border = BorderStroke(1.dp, TealPrimary)
        ) {
            Text("Go Back")
        }
    }
}
