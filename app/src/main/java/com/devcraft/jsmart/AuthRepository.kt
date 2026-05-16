package com.devcraft.jsmart

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import android.net.Uri
import com.devcraft.jsmart.data.UserInsert
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRow(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String,
    val role: String = "staff",
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    val phone: String? = null,
    @SerialName("employee_number") val employeeNumber: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val branches: BranchRow? = null,
    val departments: DepartmentRow? = null,
    val gender: String? = null
)

@Serializable
data class BranchRow(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class DepartmentRow(
    val id: String? = null,
    val name: String? = null
)

object AuthRepository {

    suspend fun uploadAvatar(userId: String, imageUri: Uri, context: Context): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            supabase.storage["avatars"].
            upload(
                path = userId,
                data = bytes,
            ) {
                upsert = true
            }

            val publicUrl = supabase.storage["avatars"].publicUrl(userId)

            // Save URL to users table
            supabase.postgrest["users"].update(
                { set("avatar_url", publicUrl) }
            ) {
                filter { eq("id", userId) }
            }

            // Update local session
            val current = UserSession.get()
            if (current != null) {
                UserSession.set(current.copy(avatarUrl = publicUrl))
            }

            publicUrl
        } catch (e: Exception) {
            Log.e("AuthRepo", "Avatar upload failed: ${e.message}", e)
            null
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        fullName: String,
        phone: String,
        gender: String
    ): Boolean {
        return try {
            supabase.postgrest["users"].update(
                {
                    set("full_name", fullName)
                    set("phone", phone)
                    set("gender", gender)
                }
            ) {
                filter { eq("id", userId) }
            }
            // Update local session too
            val current = UserSession.get()
            if (current != null) {
                UserSession.set(
                    current.copy(fullName = fullName, phone = phone, gender = gender)
                )
            }
            true
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to update profile: ${e.message}", e)
            false
        }
    }

    suspend fun updatePassword(newPassword: String): Boolean {
        return try {
            supabase.auth.updateUser {
                password = newPassword
            }
            true
        } catch (e: Exception) {
            Log.e("AuthRepo", "Password update failed: ${e.message}", e)
            false
        }
    }

    suspend fun login(emailStr: String, passwordStr: String): Result<AppUser> {
        return try {
            supabase.auth.signInWith(Email) {
                email = emailStr
                password = passwordStr
            }
            val appUser = fetchAndStoreUser(emailStr)
                ?: return Result.failure(Exception("Could not load user profile"))
            Result.success(appUser)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Called on app start to restore session
    suspend fun restoreSession(): AppUser? {
        return try {
            // Check if Supabase still has a valid session
            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser != null) {
                // Session still valid — reload profile from DB
                val appUser = fetchAndStoreUser(currentUser.email ?: "")
                appUser
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Restore session error: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchAndStoreUser(emailStr: String): AppUser? {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: return null

            val row = supabase.postgrest["users"]
                .select(Columns.raw("*, branches(id, name), departments(id, name)")) {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingle<UserRow>()

            val appUser = AppUser(
                id = row.id,
                email = row.email ?: emailStr,
                fullName = row.fullName,
                role = row.role,
                branchId = row.branchId,
                branchName = row.branches?.name,
                departmentId = row.departmentId,
                departmentName = row.departments?.name,
                phone = row.phone,
                employeeNumber = row.employeeNumber,
                isActive = row.isActive,
                avatarUrl = row.avatarUrl,
                gender = row.gender
            )

            // Update local session to match DB values on each fetch
            UserSession.set(appUser)
            appUser
        } catch (e: Exception) {
            Log.e("AuthRepository", "fetchAndStoreUser error: ${e.message}", e)
            null
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
        UserSession.clear()
    }

    suspend fun createStaffAccount(
        fullName: String,
        email: String,
        phone: String?,
        role: String,
        branchId: String?,
        departmentId: String?
    ): Boolean {
        return try {
            // Create auth user with temp password
            val tempPassword = "JSmart@${(1000..9999).random()}"

            // Use regular signUp which works with anon-key
            // Note: This automatically signs in as the new user
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = tempPassword
            }

            // Get the new user's ID
            val currentUser = supabase.auth.currentUserOrNull() ?: return false

            // Insert into users table
            supabase.postgrest["users"].insert(
                UserInsert(
                    id = currentUser.id,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = role,
                    branchId = branchId,
                    departmentId = departmentId,
                    isActive = true
                )
            )

            // Since signUp signed us in as the new user, we must log out
            // and let the admin log back in.
            // Alternatively, tell the admin why they were logged out.
            // For now, we perform the insertion and then the admin will likely
            // be "signed in" as the new staff. Clear session to be safe.
            logout()

            true
        } catch (e: Exception) {
            Log.e("AuthRepo", "Create staff error: ${e.message}", e)
            false
        }
    }
}