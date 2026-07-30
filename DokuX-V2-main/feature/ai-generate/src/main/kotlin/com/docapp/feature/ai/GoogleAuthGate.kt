package com.docapp.feature.ai

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleAccount(val email: String, val idToken: String, val displayName: String?)

/**
 * Login akun Google — syarat wajib untuk akses fitur AI (Gemini/ChatGPT).
 * Tanpa login, fitur AI dikunci total & UI menampilkan toast/snackbar untuk login.
 */
class GoogleAuthGate(private val context: Context, private val webClientId: String) {

    private val credentialManager = CredentialManager.create(context)

    private val _account = MutableStateFlow<GoogleAccount?>(null)
    val account: StateFlow<GoogleAccount?> = _account.asStateFlow()

    val isSignedIn: Boolean get() = _account.value != null

    suspend fun signIn(): Result<GoogleAccount> = try {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

        val account = GoogleAccount(
            email = credential.id,
            idToken = credential.idToken,
            displayName = credential.displayName
        )
        _account.value = account
        Result.success(account)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut() { _account.value = null }
}
