@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.ui.theme.*

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
    }
}