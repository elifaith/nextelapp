package pynith.apps.nextel.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserData(
    val id: String = "",
    val name: String = "",
    var username: String = "",
    val phone: String = "",
    val email: String = "",
    var image: String = "",
    val bio: String = "",
    val referral: String = "",
    val location: String = "",
    val provider: String = "",
    val createdAt: Long = 0L
) : Parcelable