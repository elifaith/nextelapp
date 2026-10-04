package pynith.apps.nextel.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val data = Data.Builder()
            .putInt("id", intent.getIntExtra("id", 0))
            .putString("title", intent.getStringExtra("title"))
            .putString("body", intent.getStringExtra("body"))
            .putString("channel", intent.getStringExtra("channel"))
            .putString("image", intent.getStringExtra("image"))
            .build()

        val work = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}