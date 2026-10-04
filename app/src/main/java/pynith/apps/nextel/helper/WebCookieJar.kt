package pynith.apps.nextel.helper

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Bridges the app's OkHttp API client ([NextelApi]) to the shared WebView
 * cookie store, so native API requests automatically carry the cookies stored
 * there — the `app_gate` entry cookie installed by [AppGateCookie] and the
 * web session cookies written by [WebSessionHandoff].
 *
 * The WebView cookie store remains the single source of truth: cookies are
 * looked up from android.webkit.CookieManager for every request, never
 * hard-coded as request headers. Persisting response cookies is intentionally
 * left to the existing [WebSessionHandoff] flow, which validates them before
 * saving.
 */
object WebCookieJar : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // android.webkit.CookieManager only serves http(s) URLs.
        if (url.scheme != "http" && url.scheme != "https") return emptyList()

        val header = try {
            CookieManager.getInstance().getCookie(url.toString())
        } catch (_: IllegalArgumentException) {
            // URL not representable by the cookie store; treat as no cookies.
            null
        } ?: return emptyList()

        return header.split(';')
            .mapNotNull { Cookie.parse(url, it.trim()) }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit
}
