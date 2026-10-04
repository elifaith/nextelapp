package pynith.apps.nextel.model

import com.google.gson.annotations.SerializedName

data class User(

    @SerializedName("fullname")
    val fullname: String,

    @SerializedName("fname")
    val fname: String,

    @SerializedName("lname")
    val lname: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("uname")
    val uname: String,

    @SerializedName("userDP")
    val userDP: String,

    @SerializedName("available")
    val available: Double,

    @SerializedName("currency")
    val currency: String,

    @SerializedName("curTEXT")
    val curTEXT: String,

    @SerializedName("referred")
    val referred: Int,

    @SerializedName("uwisp")
    val uwisp: Int,

    @SerializedName("referAmtBal")
    val referAmtBal: String,

    @SerializedName("toRefAMT")
    val toRefAMT: String,

    @SerializedName("utWithdraw")
    val utWithdraw: String,

    @SerializedName("utDeposit")
    val utDeposit: String,

    @SerializedName("utTrade")
    val utTrade: String,

    @SerializedName("authenticate")
    val authenticate: Int,

    @SerializedName("referral")
    val referral: String,

    @SerializedName("suplink")
    val suplink: String,

    @SerializedName("haskyc")
    val haskyc: Int,

    @SerializedName("deposit")
    val deposit: Deposit,

    @SerializedName("kycdata")
    val kycdata: KycData,

    val withdrawals: Map<String, Withdrawal>
)

data class Deposit(
    val bank: String,
    val card: String,
    val crypto: String
)
data class KycData(
    val Address_Proof: String,
    val ID_Proof: String
)

data class Withdrawal(
    val name: String,
    val rate: String,
    val view: Any,
    val currency: String? = null
)