package com.aistudio.sharhdarmatn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aistudio.sharhdarmatn.ui.AppContent
import com.aistudio.sharhdarmatn.ui.PuzzleViewModel
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import com.aistudio.sharhdarmatn.data.AdManager
import androidx.lifecycle.lifecycleScope

/**
 * جدول شرح در متن — اکتیویتی اصلی
 *
 * نسخهٔ مایکت — تبلیغات با دو شبکه (v2.2):
 *  ۱) ادیوری با پلیس‌منت‌های مایکت:
 *   - همسان: پایین صفحهٔ حل جدول (NativeAdSlot — لود یک بار در هر ورود به صفحه)
 *   - میان‌صفحه‌ای: هر ۱۰ ورود به صفحهٔ حل جدول
 *   - بازگشت به برنامه (App Open): یک بار در میان — هر بار که کاربر برمی‌گردد تبلیغ نشان داده نمی‌شود
 *  ۲) شبکهٔ «تبلیغ» خودمان (روش تبلیغ / ravesh-tabligh) — بنر آیکونی در وسطِ صفحهٔ اول
 *   و بالای صفحهٔ انتخاب جدول؛ init در onCreate و onAppForegrounded در onResume (⭐ ناجیِ بنر مُرده)
 */
class MainActivity : ComponentActivity() {
    private val viewModel: PuzzleViewModel by viewModels()

    companion object {
        // شناسهٔ برنامهٔ ادیوری (نسخهٔ مایکت — همان اپ اصلی)
        const val ADIVERY_APP_ID = "99696e06-1114-4c89-a044-51c6d6d33559"
        // شناسهٔ تبلیغگاه میان‌صفحه‌ای (میان‌صفحه‌ای مایکت)
        const val ADIVERY_INTERSTITIAL_PLACEMENT = "d9671ced-32d5-44c1-8c2a-60b9c1e71fd4"
        // شناسهٔ تبلیغگاه همسان (همسان مایکت) — در res/layout/native_ad_container.xml هم هست
        const val ADIVERY_NATIVE_PLACEMENT = "b8cc6afd-4ae6-4d7e-90af-b1863f311e53"
        // شناسهٔ تبلیغگاه بازگشت به برنامه (بازگشت به برنامه مایکت)
        const val ADIVERY_APP_OPEN_PLACEMENT = "c0489ee1-a7ad-4db5-94a7-81e379f960d4"

        @Volatile
        var isShowingFullscreenAd = false
    }

    private var lastPauseTime = 0L
    private var lastFullscreenAdDismissTime = 0L

    override fun onPause() {
        super.onPause()
        lastPauseTime = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val pauseTime = System.currentTimeMillis() - lastPauseTime
        val timeSinceAdDismiss = System.currentTimeMillis() - lastFullscreenAdDismissTime
        // فقط اگر بیش از ۵ ثانیه خارج از برنامه بوده و از بسته‌شدن تبلیغ قبلی بیش از ۱۵ ثانیه گذشته باشد
        if ((lastPauseTime == 0L || pauseTime > 5000L) && timeSinceAdDismiss > 15000L) {
            if (!isShowingFullscreenAd) {
                // «بازگشت به برنامه» یک بار در میان: هر بار که برمی‌گردد تبلیغ نشون داده نشه
                val prefs = getSharedPreferences("ad_prefs", MODE_PRIVATE)
                val count = prefs.getInt("app_open_return_count", 0) + 1
                prefs.edit().putInt("app_open_return_count", count).apply()
                if (count % 2 == 1 && Adivery.isLoaded(ADIVERY_APP_OPEN_PLACEMENT)) {
                    Adivery.showAppOpenAd(this, ADIVERY_APP_OPEN_PLACEMENT)
                }
            }
        }
        // کشِ تبلیغ بازگشت به برنامه همیشه گرم بماند تا نوبتِ بعدیِ «یک بار در میان» آماده باشد
        if (!Adivery.isLoaded(ADIVERY_APP_OPEN_PLACEMENT)) {
            Adivery.prepareAppOpenAd(this, ADIVERY_APP_OPEN_PLACEMENT)
        }
    }

    override fun onResume() {
        super.onResume()
        // ⭐ روش تبلیغ: با هر بازگشت به foreground، بنر Hidden فوراً دوباره fetch می‌کند
        // (با گارد ۶۰ ثانیه) — بدون این، بعد از هر قطعی تبلیغ تا ساعت‌ها برنمی‌گردد
        AdManager.onAppForegrounded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // راه‌اندازی شبکهٔ «تبلیغ» خودمان (روش تبلیغ) — حلقهٔ fetch + state machine
        AdManager.init(applicationContext, lifecycleScope)

        // راه‌اندازی ادیوری + آماده‌سازی اولیهٔ میان‌صفحه‌ای و بازگشت به برنامه
        try {
            Adivery.setLoggingEnabled(false)

            Adivery.addGlobalListener(object : AdiveryListener() {
                override fun onInterstitialAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                }

                override fun onAppOpenAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                }

                override fun onAppOpenAdClosed(placementId: String) {
                    isShowingFullscreenAd = false
                    lastFullscreenAdDismissTime = System.currentTimeMillis()
                    lastPauseTime = System.currentTimeMillis()
                    // بارگذاری مجدد تبلیغ بازگشت به برنامه پس از بسته‌شدن
                    Adivery.prepareAppOpenAd(this@MainActivity, ADIVERY_APP_OPEN_PLACEMENT)
                }

                override fun onInterstitialAdClosed(placementId: String) {
                    isShowingFullscreenAd = false
                    lastFullscreenAdDismissTime = System.currentTimeMillis()
                    lastPauseTime = System.currentTimeMillis()
                    // بارگذاری خودکار تبلیغ بعدی پس از بسته‌شدن
                    Adivery.prepareInterstitialAd(this@MainActivity, ADIVERY_INTERSTITIAL_PLACEMENT)
                    Adivery.prepareAppOpenAd(this@MainActivity, ADIVERY_APP_OPEN_PLACEMENT)
                }

                override fun log(placementId: String, log: String) {
                    android.util.Log.d("Adivery", "$placementId -> $log")
                }
            })

            Adivery.configure(application, ADIVERY_APP_ID)
            Adivery.prepareInterstitialAd(this, ADIVERY_INTERSTITIAL_PLACEMENT)
            Adivery.prepareAppOpenAd(this, ADIVERY_APP_OPEN_PLACEMENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            AppContent(viewModel = viewModel)
        }
    }
}
