package com.devcraft.jsmart.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val EMPLOYEE_HOME = "employee_home"
    const val ATTENDANCE_HISTORY = "attendance_history"
    const val LEAVE_REQUEST = "leave_request"
    const val TASKS = "tasks"
    const val MY_REPORTS = "my_reports"
    const val MORE = "more"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val STAFF_MANAGEMENT = "staff_management"
    const val STAFF_DETAIL = "staff_detail/{staffId}"
    const val ADMIN_REPORTS = "admin_reports"
    const val ANNOUNCEMENTS = "announcements"
    const val TASK_CREATION = "task_creation"
    const val LEAVE_APPROVAL = "leave_approval"
    const val GEOFENCE_SETTINGS = "geofence_settings"
    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_DETAIL = "notification_detail/{notificationId}"
    const val ANNOUNCEMENT_DETAIL = "announcement_detail"
    const val PROFILE = "profile"
    const val TASK_DETAIL = "task_detail"
    const val CHAT = "chat"
    const val CHAT_DETAIL = "chat_detail/{otherUserId}/{otherUserName}"
    const val SUPER_ADMIN_CHAT = "super_admin_chat"
    const val CHAT_DETAIL_READONLY = "chat_detail_readonly/{senderId}/{receiverId}/{displayName}"

    // (duplicates removed) keep a single declaration of each route above
}