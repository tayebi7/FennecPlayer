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
import kotlin.math.abs

object IboDeviceUtils {

    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "0000000000000000"
    }

    fun generateVirtualMac(context: Context): String {
        val prefs = context.getSharedPreferences("fennec_prefs", Context.MODE_PRIVATE)
        val savedMac = prefs.getString("device_mac", null)
        if (!savedMac.isNullOrEmpty()) return savedMac

        val androidId = getAndroidId(context)
        val bytes = MessageDigest.getInstance("MD5").digest(androidId.toByteArray())

        val macBuilder = StringBuilder("00:1A:")
        for (i in 0 until 4) {
            macBuilder.append(String.format("%02X", bytes[i]))
            if (i < 3) macBuilder.append(":")
        }

        val generatedMac = macBuilder.toString()
        prefs.edit().putString("device_mac", generatedMac).apply()
        return generatedMac
    }

    fun getMacAddress(context: Context): String = generateVirtualMac(context)

    fun generateDeviceKey(context: Context): String {
        val prefs = context.getSharedPreferences("fennec_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("device_key", null)
        if (!savedKey.isNullOrEmpty()) return savedKey

        val androidId = getAndroidId(context)
        val hash = abs(androidId.hashCode())
        val key = String.format("%06d", hash % 1000000)

        prefs.edit().putString("device_key", key).apply()
        return key
    }

    fun getDeviceKey(context: Context): String = generateDeviceKey(context)

    fun generateQrCodeBitmap(content: String, size: Int = 400): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
