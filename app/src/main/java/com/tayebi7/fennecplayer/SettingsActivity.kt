package com.tayebi7.fennecplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.tayebi7.fennecplayer.data.AppSettings
import com.tayebi7.fennecplayer.data.PlaylistStore
import com.tayebi7.fennecplayer.data.Repo
import com.tayebi7.fennecplayer.ui.applySystemBarsPadding

/** الإعدادات: اللغة (عربي / فرنسي / إنجليزي) ونسبة العرض الافتراضية وإعادة الضبط. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var langButtons: Map<String, Button>
    private lateinit var aspectButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        langButtons = mapOf(
            "" to findViewById(R.id.btnLangAuto),
            "ar" to findViewById(R.id.btnLangAr),
            "fr" to findViewById(R.id.btnLangFr),
            "en" to findViewById(R.id.btnLangEn)
        )
        langButtons.forEach { (tag, b) -> b.setOnClickListener { setLanguage(tag) } }

        aspectButtons = listOf(
            findViewById(R.id.btnAspectFit), findViewById(R.id.btnAspectFill), findViewById(R.id.btnAspectZoom)
        )
        aspectButtons.forEachIndexed { i, b ->
            b.setOnClickListener { AppSettings.setAspect(this, i); markAspect() }
        }

        findViewById<View>(R.id.btnReset).setOnClickListener { confirmReset() }

        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""
        findViewById<android.widget.TextView>(R.id.tvVersion).text = getString(R.string.settings_version, version)

        markLanguage()
        markAspect()
    }

    private fun currentLanguage(): String =
        AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.language.orEmpty()

    private fun markLanguage() {
        val cur = currentLanguage()
        langButtons.forEach { (tag, b) -> b.isActivated = tag == cur }
    }

    private fun markAspect() {
        val cur = AppSettings.aspect(this)
        aspectButtons.forEachIndexed { i, b -> b.isActivated = i == cur }
    }

    private fun setLanguage(tag: String) {
        // تغيير اللغة يعيد إنشاء الشاشات تلقائياً ويُحفظ على الجهاز
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setMessage(R.string.reset_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                PlaylistStore.clearAll(this)
                getSharedPreferences("fennec_settings", MODE_PRIVATE).edit().clear().apply()
                Repo.invalidate()
                startActivity(Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                ))
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
