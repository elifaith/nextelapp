package pynith.apps.nextel.helper

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Installs a daily network-constrained check and a non-blocking check when the app starts. */
object UpdateCheckScheduler {
    private const val DAILY_WORK_NAME = "daily_app_upgrade_check"
    private const val LAUNCH_WORK_NAME = "app_upgrade_check_on_launch"
    private const val CATCH_UP_WORK_NAME = "app_upgrade_check_catch_up"
    private const val LEGACY_WORK_NAME = "daily_update_check"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Stop the previous checker name left by older app installs, if present.
        workManager.cancelUniqueWork(LEGACY_WORK_NAME)

        val dailyRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            24,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(Data.Builder().putBoolean(UpdateCheckWorker.KEY_IS_PERIODIC, true).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

        val checker = UpdateChecker(appContext)
        if (checker.shouldCheckNow()) {
            val launchRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                LAUNCH_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                launchRequest
            )
        }
    }

    /**
     * Re-runs a check that WorkManager fired early. This uses its own unique name: the request
     * being replaced here is the one currently executing, so KEEP would resolve to it and the
     * catch-up would never be scheduled.
     */
    internal fun scheduleCatchUp(context: Context, delayMillis: Long) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            CATCH_UP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}