package pynith.apps.nextel.model

import android.net.Uri
import pynith.apps.nextel.BuildConfig

class CONData {
    companion object {
        val isDEBUG: Boolean = BuildConfig.DEBUG
        val WebPage: String = BuildConfig.WEB_BASE_URL.trimEnd('/') + "/"
        val URLApi: String = BuildConfig.API_BASE_URL.trimEnd('/')
        val appURL: String = "${BuildConfig.WEB_BASE_URL.trimEnd('/')}/dashboard"
        val URLWeb: String = Uri.parse(BuildConfig.WEB_BASE_URL).host.orEmpty()
        val URLApp: String = URLWeb
        val API: String = Uri.parse(BuildConfig.WEB_BASE_URL).authority.orEmpty()

        const val APP_PREFS_EXT: String = "app_prefs"
        const val APP_USER_EXT: String = "no_app_users"
        const val BIOMETRIC_ENABLED = "xbg_biometric_enabled"
        const val USER_NAME = "xbg_user_id"
        const val USER_PASS = "xbg_user_pass"
    }
}
