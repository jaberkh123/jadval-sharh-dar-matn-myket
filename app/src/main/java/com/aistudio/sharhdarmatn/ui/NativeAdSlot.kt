package com.aistudio.sharhdarmatn.ui

import android.util.Log
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.sharhdarmatn.R
import com.adivery.sdk.AdiveryAdListener
import com.adivery.sdk.AdiveryNativeAdView

/**
 * جایگاه «تبلیغ همسان» ادیوری — v2.1 (الگوی «جدول بزرگسال»: AdiveryNativeAdView + layout XML)
 *
 * - پایینِ صفحهٔ حل جدول، در محلِ قبلیِ نوارِ سؤال؛ جدول تا جای ممکن بالاتر می‌نشیند.
 * - هر بار که کاربر وارد صفحهٔ حل جدول می‌شود، یک بار لود می‌شود.
 * - با باز شدن کیبورد و نوار سؤال، «از دید کاربر پنهان» می‌شود (لود مجدد نمی‌شود) و
 *   با بسته شدنشان همان تبلیغ دوباره آشکار می‌شود.
 * - شناسهٔ تبلیغگاه همسان داخل res/layout/native_ad_container.xml است.
 */
@Composable
fun NativeAdSlot(modifier: Modifier = Modifier) {
    // ارتفاع ثابت: تا چیدمانِ جدول با لود/عدمِ لودِ تبلیغ نپرد
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                val adView = LayoutInflater.from(ctx).inflate(
                    R.layout.native_ad_container, null
                ) as AdiveryNativeAdView
                adView.setListener(object : AdiveryAdListener() {
                    override fun onAdLoaded() {
                        // تبلیغ همسان آماده شد
                    }

                    override fun onError(reason: String) {
                        Log.e("Adivery", "Native Ad Error: $reason")
                    }
                })
                adView.loadAd() // فقط یک بار در هر ورود به صفحهٔ حل جدول
                adView
            }
        )
    }
}
