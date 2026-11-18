package com.tayebi7.fennecplayer.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * أندرويد 15+ يفرض العرض خلف أشرطة النظام (edge-to-edge)، لذلك نضيف حشوة
 * بمقدار أشرطة النظام وقص الشاشة (notch) حتى لا تُحجب الأزرار. على الإصدارات
 * الأقدم وعلى TV لا تكون هناك insets فلا يتغير شيء.
 */
fun View.applySystemBarsPadding() {
    val l = paddingLeft
    val t = paddingTop
    val r = paddingRight
    val b = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
