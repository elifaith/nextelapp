package pynith.apps.nextel.helper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/** Opens the Play Store without sending app-rating data to an unsupported API. */
class RatingHelper(private val activity: Activity) {
    fun showRatingDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Enjoying Nextel?")
            .setMessage("Your feedback helps other people discover the app.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Rate in Play Store") { _, _ -> openStoreListing() }
            .show()
    }

    private fun openStoreListing() {
        val packageName = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(activity, "Unable to open the Play Store.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
