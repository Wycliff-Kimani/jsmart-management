package com.devcraft.jsmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.ui.components.BottomNavBar
import com.devcraft.jsmart.ui.screens.*
import com.devcraft.jsmart.data.TaskSession
import com.devcraft.jsmart.data.AnnouncementSession
import com.devcraft.jsmart.ui.theme.JSmartTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JSmartTheme { JSmartApp() }
        }
    }
}

@Composable
fun JSmartApp() {
    val navController = rememberNavController()
    val currentUser = UserSession.currentUser.collectAsState()
    val isAdmin = currentUser.value?.let {
        it.role in listOf("admin", "super_admin")
    } ?: false
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Routes.SPLASH

    LaunchedEffect(Unit) {
        val authUser = supabase.auth.currentUserOrNull()
        if (authUser == null) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            try {
                val row = supabase.postgrest["users"]
                    .select(Columns.raw("*, branches(id, name), departments(id, name)")) {
                        filter { eq("id", authUser.id) }
                    }
                    .decodeSingle<UserRow>()

                val appUser = AppUser(
                    id = row.id,
                    email = row.email ?: authUser.email ?: "",
                    fullName = row.fullName,
                    role = row.role,
                    branchId = row.branchId,
                    branchName = row.branches?.name,
                    departmentId = row.departmentId,
                    departmentName = row.departments?.name,
                    phone = row.phone,
                    employeeNumber = row.employeeNumber,
                    isActive = row.isActive
                )
                UserSession.set(appUser)
            } catch (_: Exception) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val bottomBarRoutes = listOf(
        Routes.EMPLOYEE_HOME,
        Routes.ATTENDANCE_HISTORY,
        Routes.LEAVE_REQUEST,
        Routes.TASKS,
        Routes.MORE,
        Routes.ADMIN_DASHBOARD,
        Routes.STAFF_MANAGEMENT,
        Routes.ADMIN_REPORTS,
        Routes.ANNOUNCEMENTS,
        Routes.MY_REPORTS,
        Routes.PROFILE,
        Routes.NOTIFICATIONS,
        Routes.TASK_CREATION,
        Routes.TASK_DETAIL,
        Routes.LEAVE_APPROVAL,
        Routes.GEOFENCE_SETTINGS
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(currentRoute = currentRoute) { route ->
                    val actualRoute = if (route == Routes.EMPLOYEE_HOME && isAdmin) {
                        Routes.ADMIN_DASHBOARD
                    } else {
                        route
                    }
                    navController.navigate(actualRoute) {
                        popUpTo(Routes.EMPLOYEE_HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToHome = { admin ->
                        navController.navigate(
                            if (admin) Routes.ADMIN_DASHBOARD else Routes.EMPLOYEE_HOME
                        ) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(onNavigateToHome = { admin ->
                    val destination = if (admin) Routes.ADMIN_DASHBOARD else Routes.EMPLOYEE_HOME
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                })
            }
            composable(Routes.EMPLOYEE_HOME) {
                EmployeeHomeScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.ATTENDANCE_HISTORY) {
                AttendanceHistoryScreen()
            }
            composable(Routes.LEAVE_REQUEST) {
                if (isAdmin) {
                    LeaveApprovalScreen(onBack = { navController.popBackStack() })
                } else {
                    LeaveRequestScreen()
                }
            }
            composable(Routes.TASKS) {
                TasksScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.MORE) {
                val logoutScope = rememberCoroutineScope()
                MoreScreen(
                    isAdmin = isAdmin,
                    onNavigate = { navController.navigate(it) },
                    onLogout = {
                        logoutScope.launch {
                            AuthRepository.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToAnnouncements = {
                        navController.navigate(Routes.ANNOUNCEMENTS)
                    }
                )
            }
            composable(Routes.MY_REPORTS) {
                MyReportsScreen()
            }
            composable(Routes.ADMIN_DASHBOARD) {
                AdminDashboardScreen(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.TASK_CREATION) {
                TaskCreationScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TASK_DETAIL) {
                currentUser.value?.let { user ->
                    TaskSession.currentTask?.let { task ->
                        TaskDetailScreen(
                            task = task,
                            currentUser = user,
                            navController = navController
                        )
                    }
                }
            }
            composable(Routes.LEAVE_APPROVAL) {
                LeaveApprovalScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_REPORTS) {
                AdminReportsScreen()
            }
            composable(Routes.STAFF_MANAGEMENT) {
                StaffManagementScreen(onNavigate = { navController.navigate(it) })
            }
            composable(
                route = Routes.STAFF_DETAIL,
                arguments = listOf(navArgument("staffId") { type = NavType.StringType })
            ) { backStackEntry ->
                val staffId = backStackEntry.arguments?.getString("staffId") ?: ""
                StaffDetailScreen(
                    staffId = staffId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ANNOUNCEMENTS) {
                currentUser.value?.let { user ->
                    AnnouncementsScreen(currentUser = user, navController = navController)
                }
            }
            composable(Routes.ANNOUNCEMENT_DETAIL) {
                currentUser.value?.let { user ->
                    AnnouncementSession.current?.let { announcement ->
                        AnnouncementDetailScreen(
                            announcement = announcement,
                            currentUser = user,
                            navController = navController
                        )
                    }
                }
            }
            composable(Routes.GEOFENCE_SETTINGS) {
                GeofenceSettingsScreen(navController = navController)
            }
            composable(Routes.NOTIFICATIONS) {
                currentUser.value?.let { user ->
                    NotificationsScreen(
                        currentUser = user,
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = {
                            navController.navigate(Routes.NOTIFICATION_DETAIL)
                        }
                    )
                }
            }
            composable(Routes.NOTIFICATION_DETAIL) {
                currentUser.value?.let { user ->
                    NotificationDetailScreen(
                        currentUser = user,
                        onBack = { navController.popBackStack() },
                        onNavigateToChat = { id, name ->
                            navController.navigate(
                                Routes.CHAT_DETAIL
                                    .replace("{otherUserId}", id)
                                    .replace("{otherUserName}", name)
                            )
                        }
                    )
                }
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    isAdmin = isAdmin,
                    onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onNavigateToGeofence = { navController.navigate(Routes.GEOFENCE_SETTINGS) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CHAT) {
                currentUser.value?.let { user ->
                    ChatScreen(
                        currentUser = user,
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = { id, name ->
                            navController.navigate(
                                Routes.CHAT_DETAIL
                                    .replace("{otherUserId}", id)
                                    .replace("{otherUserName}", name)
                            )
                        }
                    )
                }
            }
            composable(
                route = Routes.CHAT_DETAIL,
                arguments = listOf(
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("otherUserName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
                currentUser.value?.let { user ->
                    ChatDetailScreen(
                        currentUser = user,
                        otherUserId = otherUserId,
                        otherUserName = otherUserName,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Routes.SUPER_ADMIN_CHAT) {
                currentUser.value?.let { user ->
                    if (user.isSuperAdmin) {
                        SuperAdminChatScreen(
                            currentUser = user,
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { sId, rId, name ->
                                navController.navigate(
                                    Routes.CHAT_DETAIL_READONLY
                                        .replace("{senderId}", sId)
                                        .replace("{receiverId}", rId)
                                        .replace("{displayName}", name)
                                )
                            }
                        )
                    }
                }
            }
            composable(
                route = Routes.CHAT_DETAIL_READONLY,
                arguments = listOf(
                    navArgument("senderId") { type = NavType.StringType },
                    navArgument("receiverId") { type = NavType.StringType },
                    navArgument("displayName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val senderId = backStackEntry.arguments?.getString("senderId") ?: ""
                val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
                val displayName = backStackEntry.arguments?.getString("displayName") ?: ""
                ChatDetailReadOnly(
                    senderId = senderId,
                    receiverId = receiverId,
                    displayName = displayName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
