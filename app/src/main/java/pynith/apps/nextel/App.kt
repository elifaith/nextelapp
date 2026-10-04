package pynith.apps.nextel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import pynith.apps.nextel.helper.AppGateCookie
import pynith.apps.nextel.helper.LogoutCoordinator
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.helper.UpdateCheckScheduler
import pynith.apps.nextel.helper.UpdateChecker
import pynith.apps.nextel.views.splash.SplashActivity
import pynith.apps.nextel.views.update.UpdateActivity
import java.lang.ref.WeakReference

class App : Application(), Application.ActivityLifecycleCallbacks {

    private var resumedActivity: WeakReference<Activity>? = null
    private var updatePageLaunchInProgress = false

    override fun onCreate() {
        super.onCreate()

        // The web domain rejects requests without the app_gate cookie, so it
        // must already sit in the WebView cookie store before any component
        // of this process (activity, service or worker) can perform the first
        // request or navigation. Application.onCreate() always runs first.
        AppGateCookie.ensureInstalled()

        userInfo = applicationContext.getSharedPreferences(
            PREF_NAME,
            Activity.MODE_PRIVATE
        )
        editor = userInfo.edit()

        SessionService(this)
        LogoutCoordinator.retryQueuedRevocations(this)

        registerActivityLifecycleCallbacks(this)

        try {
            UpdateCheckScheduler.schedule(applicationContext)
        } catch (_: Exception) {
            // Update checking is best effort.
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)

        if (activity !is UpdateActivity) {
            maybeOpenPendingUpdate(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity?.get() === activity) {
            resumedActivity = null
        }
    }

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    /**
     * Called by the background update checker after the server response
     * has been persisted.
     */
    fun onUpdateCheckFinished() {
        Handler(Looper.getMainLooper()).post {
            resumedActivity?.get()?.let { activity ->
                maybeOpenPendingUpdate(activity)
            }
        }
    }

    private fun maybeOpenPendingUpdate(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        if (activity is SplashActivity || activity is UpdateActivity) {
            return
        }

        if (updatePageLaunchInProgress) {
            return
        }

        val opened = UpdateChecker(this)
            .openUpdatePageIfNeeded(activity)

        if (opened) {
            updatePageLaunchInProgress = true
        }
    }

    companion object {
        lateinit var editor: SharedPreferences.Editor
        lateinit var userInfo: SharedPreferences

        var UserDPUri: Uri? = null

        private const val PREF_NAME = "UserInfo"

        fun logout(context: Context) {
            LogoutCoordinator.requestLogout(context)
        }
    }
}
