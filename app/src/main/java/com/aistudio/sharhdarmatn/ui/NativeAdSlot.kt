package com.aistudio.sharhdarmatn.ui

import android.util.Log
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.sharhdarmatn.R
import com.adivery.sdk.AdiveryAdListener
import com.adivery.sdk.AdiveryNativeAdView

/**
 * جایگاه «تبلیغ همسان» ادیوری — بازطراحیِ v2.5 (قاعدهٔ کاربر):
 *
 * «تبلیغات همسان و کیبورد و باکس سؤال هر سه تا یه اندازه باشن در پایین صفحه» →
 * این تبلیغ حالا یکی از سه محتوایِ «اسلاتِ ثابتِ» پایین صفحه است (۱/۱۰ ارتفاع صفحه،
 * تمامِ عرض) و دقیقاً همان ظاهر و اندازهٔ نوارِ سؤال و کیبورد را دارد؛ هر سه با یک
 * ظرفِ یکسان (گوشه‌های گردِ بالای ۱۸dp + حاشیهٔ ظریف) جابه‌جا می‌شوند و جدولِ بالای
 * اسلات همیشه هم‌اندازه می‌ماند و هیچ فضای خالی‌ای بینشان باز نمی‌شود.
 *
 * - هر بار که کاربر وارد صفحهٔ حل جدول می‌شود، یک بار لود می‌شود.
 * - هنگام جابه‌جاییِ اسلات به نوار سؤال/کیبورد، از دید کاربر پنهان می‌شود ولی
 *   نمونهٔ تبلیغ زنده می‌ماند (لود مجدد نمی‌شود) و با برگشت به حالت عادی همان
 *   تبلیغ دوباره آشکار می‌شود.
 * - شناسهٔ تبلیغگاه همسان داخل res/layout/native_ad_container.xml است.
 */
@Composable
fun NativeAdSlot(modifier: Modifier = Modifier) {
    // پُرکردنِ کاملِ اسلاتِ ثابت (هم‌اندازهٔ نوار سؤال و کیبورد) — بدونِ پرش چیدمان
    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(12.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        // محتوای تبلیغ (wrap_content) وسطِ اسلاتِ هم‌اندازهٔ پنل‌ها نشسته می‌شود
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
