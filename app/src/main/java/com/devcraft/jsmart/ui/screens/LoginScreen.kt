@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import com.devcraft.jsmart.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.ui.theme.*

@Composable
fun LoginScreen(onNavigateToHome: (isAdmin: Boolean) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Chair,
                    contentDescription = "JSmart Logo",
                    tint = Cream,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "JSmart",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
            Text(
                text = "Interior Decor",
                fontSize = 14.sp,
                color = CharcoalMedium,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Welcome back",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = CharcoalDark,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Sign in to your account",
                fontSize = 14.sp,
                color = CharcoalMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = ""
                        val result = AuthRepository.login(email, password)
                        isLoading = false
                        result.fold(
                            onSuccess = { user ->
                                if (user.role in listOf("admin", "super_admin")) {
                                    onNavigateToHome(true)
                                } else {
                                    onNavigateToHome(false)
                                }
                            },
                            onFailure = {
                                errorMessage = "Invalid email or password"
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Cream, modifier = Modifier.size(20.dp))
                } else {
                    Text("Login", color = Cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Show error below button
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = ErrorRed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Developed by DevCraft Technologies",
                fontSize = 11.sp,
                color = CharcoalMedium.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}