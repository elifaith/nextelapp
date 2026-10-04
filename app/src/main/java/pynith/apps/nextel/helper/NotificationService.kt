package pynith.apps.nextel.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import pynith.apps.nextel.views.main.HomeActivity
import pynith.apps.nextel.R
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime

object NotificationService {

    private const val CHANNEL_ID = "daily_channel_id"

    fun cancelAll(context: Context) {

        /// Cancel notifications (visible)
        NotificationManagerCompat.from(context).cancelAll()

        /// Cancel WorkManager jobs
        WorkManager.getInstance(context).cancelAllWork()

        /// Cancel AlarmManager alarms
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (id in listOf(1, 2, 3)) {
            val intent = Intent(context, NotificationReceiver::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
        }
    }
    fun scheduleDailyReminders(context: Context) {

        cancelAll(context)

        scheduleExact(
            context,
            id = 1,
            hour = 8,
            minute = 0,
            title = "Morning Reminder ??",
            body = "Start your day with smart trading!"
        )

        scheduleExact(
            context,
            id = 2,
            hour = 15,
            minute = 0,
            title = "Afternoon Check ?",
            body = "Markets are moving ? stay updated."
        )

        scheduleExact(
            context,
            id = 3,
            hour = 19,
            minute = 7,
            title = "Evening Wrap ?",
            body = "Review today's trades and plan ahead."
        )
    }


    fun scheduleExact(
        context: Context,
        id: Int,
        hour: Int,
        minute: Int,
        title: String,
        body: String,
        image: String? = null
    ) {

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("channel", CHANNEL_ID)
            putExtra("image", image)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val trigger = nextTime(hour, minute)

        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger,
            pendingIntent
        )
    }

    private fun nextTime(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now(ZoneId.of("Africa/Lagos"))

        var target = now.withHour(hour).withMinute(minute).withSecond(0)

        if (target.isBefore(now)) target = target.plusDays(1)

        return target.toInstant().toEpochMilli()
    }

    /// ? SHOW NOTIFICATION
    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        body: String,
        channel: String,
        imageUrl: String? = null
    ) {

        /// OPEN APP (Trade)
        val openIntent = Intent(context, HomeActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /// CANCEL ACTION
        val cancelIntent = Intent(context, CancelReceiver::class.java).apply {
            putExtra("id", id)
        }

        val cancelPending = PendingIntent.getBroadcast(
            context,
            id + 1000,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Trade", openPending)
            .addAction(0, "Cancel", cancelPending)

        /// ? IMAGE SUPPORT
        if (!imageUrl.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())

                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }


    fun showTestNotification(context: Context) {

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", 999)
            putExtra("title", "Test Notification ?")
            putExtra("body", "Hybrid system is working!")
            putExtra("channel", "test_channel_id")
            putExtra("image", "https://apps.novatrade.live/assets/images/frontend/login/697377a6967351769174950.jpg") // optional
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Trigger almost immediately (1 second)
        val triggerTime = System.currentTimeMillis() + 1000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun showTestDelay(context: Context) {

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", 1000)
            putExtra("title", "Delayed Test ??")
            putExtra("body", "This appeared after 10 seconds")
            putExtra("channel", "test_channel_id")
            putExtra("image", "https://www.wikihow.com/images/thumb/8/89/14820941.jpg/v4-600px-14820941.jpg") // optional
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerTime = System.currentTimeMillis() + 10_000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun testWorkManagerOnly(context: Context) {

        val data = Data.Builder()
            .putInt("id", 2000)
            .putString("title", "Worker Test ??")
            .putString("body", "Only WorkManager executed")
            .putString("channel", "test_channel_id")
            .build()

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

}