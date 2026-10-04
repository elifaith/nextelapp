package pynith.apps.nextel.helper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import pynith.apps.nextel.model.CONData
import pynith.apps.nextel.views.auth.LoginActivity
import pynith.apps.nextel.views.main.HomeActivity
import org.json.JSONObject

/** Performs the bearer-authenticated API-to-Laravel session/cookie handoff. */
object WebSessionHandoff {
    fun openLivewire(
        activity: Activity,
        onSuspended: ((ApiError) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
    ) {
        val session = SessionService(activity)
        val token = session.getToken()
        if (token.isNullOrBlank()) {
            onError?.invoke("Please sign in again to continue.")
            return
        }

        NextelApi(activity).post("web-session", JSONObject(), token) { result ->
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread

                when (result) {
                    is ApiResult.Failure -> {
                        if (result.error.statusCode == 401) {
                            session.clearSession()
                            returnToNativeLogin(activity)
                        } else if (result.error.statusCode == 403
                            && result.error.data.optString("support_token").isNotBlank()
                            && onSuspended != null
                        ) {
                            onSuspended.invoke(result.error)
                        } else {
                            onError?.invoke(result.error.displayMessage())
                        }
                    }

                    is ApiResult.Success -> {
                        val destination = resolveDestination(result.data.optString("redirect"))
                        if (destination == null || result.cookies.isEmpty()) {
                            onError?.invoke("Could not establish a secure website session. Please try again.")
                            return@runOnUiThread
                        }
                        writeCookies(activity, destination, result.cookies, { error ->
                            onError?.invoke(error)
                        }) {
                            val intent = Intent(activity, HomeActivity::class.java).apply {
                                putExtra(HomeActivity.EXTRA_WEB_URL, destination.toString())
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            activity.startActivity(intent)
                            activity.finish()
                        }
                    }
                }
            }
        }
    }

    private fun resolveDestination(redirect: String): Uri? {
        val webBase = Uri.parse(CONData.WebPage)
        val parsed = redirect.takeIf(String::isNotBlank)?.let(Uri::parse)
        val path = parsed?.encodedPath
            ?.takeIf { it.startsWith("/") && !it.contains("..") }
            ?: "/dashboard"
        val query = parsed?.encodedQuery

        return webBase.buildUpon()
            .encodedPath(path)
            .encodedQuery(query)
            .fragment(null)
            .build()
            .takeIf { it.host == webBase.host && it.scheme == webBase.scheme }
    }

    private fun writeCookies(
        activity: Activity,
        destination: Uri,
        cookies: List<String>,
        onError: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val cookieOrigin = "${destination.scheme}://${destination.host}/"
        val pendingCookies = cookies.filter(String::isNotBlank)
        if (pendingCookies.isEmpty()) {
            onError("The server did not provide website session cookies. Please try again.")
            return
        }

        var remaining = pendingCookies.size
        var failed = false
        pendingCookies.forEach { cookie ->
            cookieManager.setCookie(cookieOrigin, cookie) { accepted ->
                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                    if (accepted != true) failed = true
                    remaining -= 1
                    if (remaining == 0) {
                        cookieManager.flush()
                        if (failed) {
                            onError("Unable to save the website session cookies. Please try again.")
                        } else {
                            onComplete()
                        }
                    }
                }
            }
        }
    }

    private fun returnToNativeLogin(activity: Activity) {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            val intent = Intent(activity, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }
}
