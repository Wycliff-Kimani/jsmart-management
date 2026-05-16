package com.devcraft.jsmart.data

import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInsert(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class DepartmentRow(
    val id: String,
    val name: String
)

@Serializable
data class StaffRow(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String? = null,
    val role: String = "staff",
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    val phone: String? = null,
    @SerialName("employee_number") val employeeNumber: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val branches: BranchInfo? = null,
    val departments: DepartmentInfo? = null
)

@Serializable
data class BranchInfo(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class DepartmentInfo(
    val id: String? = null,
    val name: String? = null
)

object StaffRepository {

    suspend fun getAllDepartments(): List<DepartmentRow> {
        return try {
            supabase.postgrest["departments"]
                .select {
                    order("name", Order.ASCENDING)
                }
                .decodeList<DepartmentRow>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllStaff(): List<StaffRow> {
        return try {
            supabase.postgrest["users"].select(
                columns = Columns.raw("*, branches(id, name), departments(id, name)")
            ) {
                filter {
                    eq("is_active", true)
                }
                order("full_name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<StaffRow>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStaffByBranch(branchId: String): List<StaffRow> {
        return try {
            supabase.postgrest["users"].select(
                columns = Columns.raw("*, branches(id, name), departments(id, name)")
            ) {
                filter {
                    eq("is_active", true)
                    eq("branch_id", branchId)
                }
                order("full_name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<StaffRow>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStaffById(userId: String): StaffRow? {
        return try {
            supabase.postgrest["users"].select(
                columns = Columns.raw("*, branches(id, name), departments(id, name)")
            ) {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<StaffRow>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateStaffBranch(userId: String, newBranchId: String): Boolean {
        return try {
            supabase.postgrest["users"].update(
                {
                    set("branch_id", newBranchId)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateStaffRole(userId: String, newRole: String): Boolean {
        return try {
            supabase.postgrest["users"].update(
                {
                    set("role", newRole)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deactivateStaff(userId: String): Boolean {
        return try {
            supabase.postgrest["users"].update(
                {
                    set("is_active", false)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun reactivateStaff(userId: String): Boolean {
        return try {
            supabase.postgrest["users"].update(
                {
                    set("is_active", true)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
