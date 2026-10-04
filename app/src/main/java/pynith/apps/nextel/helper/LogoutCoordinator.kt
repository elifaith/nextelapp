package pynith.apps.nextel.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import pynith.apps.nextel.views.auth.LoginActivity
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Revokes the current Sanctum token, clears WebView auth, and returns to native login. */
object LogoutCoordinator {
    private const val UNIQUE_WORK_NAME = "nextel-revoke-queued-api-tokens"

    fun requestLogout(context: Context) {
        val activity = context as? Activity
        if (activity == null) {
            performLogout(context)
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out of your Nextel account?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out") { _, _ -> performLogout(activity) }
            .show()
    }

    fun logoutAfterWebSession(context: Context) {
        performLogout(context)
    }

    fun retryQueuedRevocations(context: Context) {
        if (SessionService(context).queuedLogoutTokens().isNotEmpty()) {
            scheduleRevocationWorker(context)
        }
    }

    private fun performLogout(context: Context) {
        val session = SessionService(context)
        val token = session.getToken()
        if (!token.isNullOrBlank()) {
            try {
                session.queueLogoutToken(token)
            } catch (_: Exception) {
                // Continue clearing the local session even if the encrypted retry queue fails.
            }
        }

        // Revoke in the background, but don't keep the person on the WebView while waiting.
        if (!token.isNullOrBlank()) {
            NextelApi(context.applicationContext).post("logout", JSONObject(), token) { result ->
                try {
                    when (result) {
                        is ApiResult.Success -> session.removeQueuedLogoutToken(token)
                        is ApiResult.Failure -> {
                            if (result.error.statusCode == 401) {
                                // The API token is already invalid, so it has nothing left to revoke.
                                session.removeQueuedLogoutToken(token)
                            } else {
                                scheduleRevocationWorker(context.applicationContext)
                            }
                        }
                    }
                } catch (_: Exception) {
                    scheduleRevocationWorker(context.applicationContext)
                }
            }
        }

        runOnMain(context) {
            clearWebSession {
                if (context is Activity) {
                    Toast.makeText(context, "You are now logged out", Toast.LENGTH_SHORT).show()
                }
                val appContext = context.applicationContext
                val intent = Intent(appContext, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                appContext.startActivity(intent)
            }
        }
    }

    private fun clearWebSession(onCleared: () -> Unit) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeAllCookies {
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
            onCleared()
        }
    }

    private fun runOnMain(context: Context, action: () -> Unit) {
        if (context is Activity) context.runOnUiThread { action() }
        else Handler(Looper.getMainLooper()).post { action() }
    }

    private fun scheduleRevocationWorker(context: Context) {
        val request = OneTimeWorkRequestBuilder<PendingLogoutWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
