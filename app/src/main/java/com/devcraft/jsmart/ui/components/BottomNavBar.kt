package com.devcraft.jsmart.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.devcraft.jsmart.navigation.Routes

data class NavItem(val label: String, val icon: ImageVector, val route: String)

val bottomNavItems = listOf(
    NavItem("Home", Icons.Filled.Home, Routes.EMPLOYEE_HOME),
    NavItem("History", Icons.Filled.CalendarMonth, Routes.ATTENDANCE_HISTORY),
    NavItem("Tasks", Icons.Filled.Assignment, Routes.TASKS),
    NavItem("Leave", Icons.Filled.BeachAccess, Routes.LEAVE_REQUEST),
    NavItem("More", Icons.Filled.MoreHoriz, Routes.MORE)
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}