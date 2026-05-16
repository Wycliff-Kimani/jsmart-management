package com.devcraft.jsmart.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.AuthRepository
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (isAdmin: Boolean) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1000),
        label = "splash_alpha"
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
        scope.launch {
            // Try to restore existing session
            val restoredUser = AuthRepository.restoreSession()
            kotlinx.coroutines.delay(1500) // minimum splash display time
            if (restoredUser != null) {
                onNavigateToHome(restoredUser.isAdmin)
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TealPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Cream, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Chair,
                    contentDescription = "JSmart Logo",
                    tint = TealPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "JSmart",
                color = Cream,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Text(
                text = "INTERIOR DECOR",
                color = TealLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Developed by DevCraft Technologies",
                color = TealLight.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}