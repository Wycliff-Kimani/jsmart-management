package com.devcraft.jsmart

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://vzxijaeajanjqbyrjjgm.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6eGlqYWVhamFuanFieXJqamdtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI3OTQ5MTAsImV4cCI6MjA4ODM3MDkxMH0.5in7ruOnTujZqCBUjbTta6bN8hCcQnZaCtu2a_ZoANU"
    ) {
        install(Auth) {
            scheme = "jsmart"
            host = "login"
        }
        install(Postgrest)
        install(Storage)
    }
}

val supabase: io.github.jan.supabase.SupabaseClient get() = SupabaseClient.client