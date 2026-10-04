package pynith.apps.nextel.helper

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        NotificationService.showNotification(
            applicationContext,
            inputData.getInt("id", 0),
            inputData.getString("title") ?: "",
            inputData.getString("body") ?: "",
            inputData.getString("channel") ?: "",
            inputData.getString("image")
        )

        return Result.success()
    }
}