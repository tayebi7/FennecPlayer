package com.tayebi7.fennecplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.Settings
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest

object IboDeviceUtils {

    private const val PREFS = "fennec_prefs"

    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "0000000000000000"

    fun generateVirtualMac(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString("device_mac", null)?.takeIf { it.isNotEmpty() }?.let { return it }

        val bytes = MessageDigest.getInstance("MD5").digest(getAndroidId(context).toByteArray())
        val mac = "00:1A:" + (0 until 4).joinToString(":") { "%02X".format(bytes[it].toInt() and 0xFF) }
        prefs.edit().putString("device_mac", mac).apply()
        return mac
    }

    fun getMacAddress(context: Context): String = generateVirtualMac(context)

    fun generateDeviceKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString("device_key", null)?.takeIf { it.isNotEmpty() }?.let { return it }

        // floorMod بدل abs لأن abs(Int.MIN_VALUE) يبقى سالباً
        val key = "%06d".format(Math.floorMod(getAndroidId(context).hashCode(), 1_000_000))
        prefs.edit().putString("device_key", key).apply()
        return key
    }

    fun getDeviceKey(context: Context): String = generateDeviceKey(context)

    fun generateQrCodeBitmap(content: String, size: Int = 400): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            null
        }
    }
}
