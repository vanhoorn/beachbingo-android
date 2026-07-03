package com.bestfriends.beachbingo.feature.bingo.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCodeImage(content: String, size: Dp = 200.dp) {
    val bitmap = remember(content) { generateQrBitmap(content) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR-Code",
        modifier = Modifier.size(size)
    )
}

private fun generateQrBitmap(content: String, pixelSize: Int = 512): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, pixelSize, pixelSize, hints)
    val bitmap = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.RGB_565)
    for (x in 0 until pixelSize) {
        for (y in 0 until pixelSize) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
