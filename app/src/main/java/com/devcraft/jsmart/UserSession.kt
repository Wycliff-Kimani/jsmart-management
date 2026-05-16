package com.devcraft.jsmart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val branchId: String? = null,
    val branchName: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val phone: String? = null,
    val employeeNumber: String? = null,
    val isActive: Boolean = true,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("gender") val gender: String? = null
) {
    val isAdmin: Boolean get() = role == "admin" || role == "super_admin"
    val isSuperAdmin: Boolean get() = role == "super_admin"
    val displayInitial: String get() = fullName.firstOrNull()?.toString() ?: "?"
    val displayBranch: String get() = branchName ?: "JSmart"
    val displayDepartment: String get() = departmentName ?: ""
}

object UserSession {
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser

    fun set(user: AppUser) { _currentUser.value = user }
    fun clear() { _currentUser.value = null }
    fun get(): AppUser? = _currentUser.value

    val isLoggedIn: Boolean get() = _currentUser.value != null
    val isAdmin: Boolean get() = get()?.isAdmin == true
    val isSuperAdmin: Boolean get() = get()?.isSuperAdmin == true
}