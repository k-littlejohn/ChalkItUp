package com.example.chalkitup.ui

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
//import java.util.prefs.Preferences
import androidx.datastore.preferences.core.Preferences




private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_storage")

class SecureStorage(private val context: Context) {

    private val gson = Gson()
    private val keyEmail = stringPreferencesKey("email")
    private val keyPassword = stringPreferencesKey("password")
    private val keyUserDetail = stringPreferencesKey("userDetail")

    fun setEmail(email: String) {
        runBlocking {
            context.dataStore.edit { it[keyEmail] = email }
        }
    }

    fun getEmail(): String? {
        return runBlocking {
            context.dataStore.data.first()[keyEmail]
        }
    }

    fun setUser(user: UserProfile) {
        val userJson = gson.toJson(user)
        runBlocking {
            context.dataStore.edit { it[keyUserDetail] = userJson }
        }
    }

    fun getUser(): UserProfile? {
        val userJson = runBlocking {
            context.dataStore.data.first()[keyUserDetail]
        }
        return userJson?.let { gson.fromJson(it, UserProfile::class.java) }
    }

    fun setPassword(password: String) {
        val hashedPassword = hashPassword(password)
        runBlocking {
            context.dataStore.edit { it[keyPassword] = hashedPassword }
        }
    }

    fun checkPassword(password: String): Boolean {
        val storedHashedPassword = runBlocking {
            context.dataStore.data.first()[keyPassword]
        }
        return storedHashedPassword == hashPassword(password)
    }

    fun deleteAll() {
        runBlocking {
            context.dataStore.edit {
                it.remove(keyEmail)
                it.remove(keyPassword)
                it.remove(keyUserDetail)
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}