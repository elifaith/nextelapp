package pynith.apps.nextel.helper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Retries token revocation after an offline or interrupted logout. */
class PendingLogoutWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val session = SessionService(applicationContext)
        val api = NextelApi(applicationContext)
        val tokens = session.queuedLogoutTokens()

        for (token in tokens) {
            when (val result = api.requestBlocking("POST", "logout", JSONObject(), token)) {
                is ApiResult.Success -> session.removeQueuedLogoutToken(token)
                is ApiResult.Failure -> {
                    if (result.error.statusCode == 401) {
                        session.removeQueuedLogoutToken(token)
                    } else {
                        return@withContext Result.retry()
                    }
                }
            }
        }

        Result.success()
    }
}
