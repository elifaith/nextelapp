package pynith.apps.nextel.helper

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

fun generateQrCode(text: String, size: Int = 512): Bitmap {

    val bits = QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        size,
        size
    )

    val width = bits.width
    val height = bits.height

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.RGB_565
    )

    for (x in 0 until width) {
        for (y in 0 until height) {

            bitmap.setPixel(
                x,
                y,
                if (bits[x, y]) Color.BLACK else Color.WHITE
            )
        }
    }

    return bitmap
}