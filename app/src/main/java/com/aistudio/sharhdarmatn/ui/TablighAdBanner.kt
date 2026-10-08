/*
 * ─────────────────────────────────────────────────────────────────────────────
 *  TablighAdBanner.kt — بنر تبلیغات خودمان (روش تبلیغ / ravesh-tabligh)
 * ─────────────────────────────────────────────────────────────────────────────
 *  عیناً از reference-code/AdBanner.kt ریپوی ravesh-tabligh کپی شده (با تطبیق
 *  پکیج و نمادهای تم این اپ) — همان کدِ در حال استفاده در اپ «شهر سودوکو».
 *
 *  محتوا:
 *   • adRatioToAspect  — تبدیل «1:1»/«9:3» سرور به float
 *   • AdIconsBanner    — بنر اصلی: ۳ آیکون در عرض، ترتیب رندوم، ورود پلکانی (v2)
 *   • IconAdItem       — سلول هر آیکون: ۹۰٪ عرض، انیمیشن ورود فقط یک‌بار (v2)
 *
 *  v2 — فیکس باگ اسکرول: وضعیت «دیده‌شده» در سطح بنر نگه‌داری می‌شود؛ هر آیکون
 *  فقط یک بار انیمیشن ورود می‌گیرد و در اسکرول‌های بعدی هرگز غیب/ظاهر نمی‌شود.
 *
 *  جایگذاری: وسطِ صفحهٔ اول (LandingScreen) + بالای صفحهٔ انتخاب جدول (HomeScreen).
 *  در حالت Loading/Hidden هیچ‌چیز رندر نمی‌کند (اصل سکوت کامل — بدون خطا و کرش).
 */
package com.aistudio.sharhdarmatn.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.aistudio.sharhdarmatn.data.AdManager
import com.aistudio.sharhdarmatn.data.AdsUiState
import com.aistudio.sharhdarmatn.data.IconAd
import com.aistudio.sharhdarmatn.ui.theme.DarkOnBackground
import com.aistudio.sharhdarmatn.ui.theme.DarkSurface
import com.aistudio.sharhdarmatn.ui.theme.LocalDarkTheme
import com.aistudio.sharhdarmatn.ui.theme.PersianFontFamily
import kotlinx.coroutines.delay

/** تبدیل رشتهٔ نسبت تصویر API («1:1» یا «9:3») به float عرض÷ارتفاع. */
fun adRatioToAspect(ratio: String?, fallback: Float = 1f): Float =
    ratio?.split(":")?.takeIf { it.size == 2 }
        ?.mapNotNull { it.trim().toFloatOrNull() }
        ?.takeIf { it.size == 2 && it[1] != 0f }
        ?.let { (w, h) -> w / h }
        ?: fallback

/**
 * بنر تبلیغات آیکونی (صفحهٔ اول + صفحهٔ انتخاب جدول) — طبق مستند روش تبلیغ:
 *  • فقط در حالت Ready و با آیکون‌های واقعیِ API رندر می‌شود؛
 *    در Loading/Hidden هیچ‌چیز نشان داده نمی‌شود (بدون خطا، بدون کرش، بدون قفل شدن).
 *  • در عرض صفحه دقیقاً ۳ آیکون جا می‌گیرد؛ اگر تعداد آیکون‌ها بیشتر باشد،
 *    ردیف به‌صورت افقی اسکرول می‌خورد (سرریز به سمت راست).
 *  • تپ روی هر آیکون → باز شدن مقصد همان آیکون: تبلیغِ کافه‌بازار مستقیم در خود
 *    اپ بازار باز می‌شود (bazaar://details?id + setPackage — بدون مرورگر)، سایر
 *    مقصدها با مرورگر. آمار کلیک در هر دو حالت ثبت می‌شود (AdManager.openAd).
 *  • انیمیشن ورود پلکانی: هر بار صفحه لود می‌شود آیکون‌ها به ترتیب از چپ به راست،
 *    هر کدام ۱ ثانیه بعد از قبلی، با fade-in نرم ظاهر می‌شوند (جلب توجه به تبلیغ).
 */
@Composable
fun AdIconsBanner(modifier: Modifier = Modifier) {
    val st = AdManager.state
    if (st !is AdsUiState.Ready || st.icons.isEmpty()) return

    val ctx = LocalContext.current
    val isDark = LocalDarkTheme.current
    val cardBg = if (isDark) DarkSurface.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (isDark) DarkOnBackground.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val headerColor = if (isDark) DarkOnBackground.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    val titleColor = if (isDark) DarkOnBackground.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // عرض هر آیتم = (عرض داخل کارت − دو فاصلهٔ بین سه آیتم) ÷ ۳ → دقیقاً ۳ آیکون در صفحه
        val hPad = 12.dp
        val spacing = 10.dp
        val itemWidth = ((maxWidth - hPad * 2) - spacing * 2) / 3
        val iconAspect = adRatioToAspect(st.iconsRatio)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg, RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp)
        ) {
            // سربرگ کوچک بنر
            Row(
                modifier = Modifier.padding(horizontal = hPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = headerColor, modifier = Modifier.size(13.dp))
                Text(
                    text = "پیشنهاد ما",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = headerColor, fontWeight = FontWeight.Bold, fontSize = 10.sp
                    ),
                    fontFamily = PersianFontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ردیف آیکون‌ها — ترتیب آیکون‌ها هر بار رندوم است (اولین آیکون هر fetch
            // می‌تواند متفاوت باشد)؛ جهتِ ردیف همیشه LTR می‌ماند تا آیکون‌های بیشتر
            // با اسکرول به سمتِ راست ظاهر شوند (مستقل از زبان دستگاه).
            //
            // v2 — حافظهٔ «آیکون‌های دیده‌شده» در سطح بنر:
            // این مجموعه در طول عمر بنر (و بین اسکرول‌های LazyRow) زنده می‌ماند؛
            // هر آیکون فقط یک بار انیمیشن ورود می‌گیرد و بعد از آن، در هر
            // اسکرول (چپ/راست) فوراً و بدون هیچ انیمیشنی دیده می‌شود.
            // کلید remember عمداً st.icons است: با refresh واقعیِ داده از سرور،
            // مجموعه از نو ساخته می‌شود و ورود پلکانی دوباره پخش می‌شود.
            // نکته: در compose runtime 1.7 (BOM این پروژه) mutableStateSetOf وجود ندارد؛
            // همان رفتار با mutableStateListOf هم تأمین می‌شود (contains/add واکنشی است).
            val appearedSlots = remember(st.icons) { mutableStateListOf<Int>() }
            val displayIcons = remember(st.icons) { st.icons.shuffled() }
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = hPad),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    itemsIndexed(displayIcons, key = { _, it -> it.slot }) { index, icon ->
                        IconAdItem(
                            icon = icon,
                            appearIndex = index,
                            alreadyShown = icon.slot in appearedSlots,
                            onAppeared = { appearedSlots.add(icon.slot) },
                            reloadKey = st.icons,
                            itemWidth = itemWidth,
                            aspect = iconAspect,
                            titleColor = titleColor
                        ) {
                            AdManager.openAd(ctx, icon)
                        }
                    }
                }
            }
        }
    }
}

/**
 * سلول یک آیکون تبلیغ: تصویر ۹۰٪ عرض سلول + عنوان یک‌خطی زیر آن.
 *
 * v2 — انیمیشن ورود فقط یک‌بار (فیکس باگ اسکرول):
 *  • اگر آیکون قبلاً یک‌بار دیده شده (alreadyShown)، مستقیم و بدون هیچ
 *    انیمیشنی نمایش داده می‌شود — در هیچ اسکرولی دیگر غیب/ظاهر نمی‌شود.
 *  • ورود پلکانی ۱ثانیه‌ای فقط برای ۳ آیکونِ پنجرهٔ اولِ صفحه است؛
 *    آیکون‌های بیرون از پنجره با یک فید کوتاه (۲۵۰ms) و فقط بار اول وارد
 *    می‌شوند تا حس «لود دوباره» ندهد.
 *  • تصویر با ImageRequest دارای memoryCacheKey ثابت و crossfade خاموش لود
 *    می‌شود تا خود Coil هم هیچ‌گاه دوباره‌سازی بصری ایجاد نکند.
 */
@Composable
private fun IconAdItem(
    icon: IconAd,
    appearIndex: Int,
    alreadyShown: Boolean,
    onAppeared: () -> Unit,
    reloadKey: Any?,
    itemWidth: Dp,
    aspect: Float,
    titleColor: Color,
    onClick: () -> Unit
) {
    val appearAlpha = remember { Animatable(if (alreadyShown) 1f else 0f) }
    LaunchedEffect(reloadKey, icon.slot) {
        if (alreadyShown) {
            // قبلاً دیده شده: بدون هیچ انیمیشنی — فقط مطمئن شو کاملاً پیدا است.
            if (appearAlpha.value < 1f) appearAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        appearAlpha.snapTo(0f)
        // از همین لحظه «دیده‌شده» علامت بخور تا حتی اگر آیتم وسط انیمیشن از دید
        // خارج شد و دوباره برگشت، هیچ‌وقت انیمیشن از نو پخش نشود.
        onAppeared()
        if (appearIndex < 3) {
            delay(appearIndex * 1_000L)           // پنجرهٔ اول: آیکون n → n ثانیه بعد
            appearAlpha.animateTo(1f, tween(700)) // fade-in نرم ~۰٫۷ ثانیه
        } else {
            appearAlpha.animateTo(1f, tween(250)) // بیرون از پنجره: فید کوتاه، فقط بار اول
        }
    }

    val imageCtx = androidx.compose.ui.platform.LocalContext.current
    val imageModel = remember(icon.slot, icon.imageUrl, imageCtx) {
        ImageRequest.Builder(imageCtx)
            .data(icon.imageUrl)
            .memoryCacheKey("tabligh-icon-${icon.slot}")
            .crossfade(false)
            .build()
    }

    Column(
        modifier = Modifier
            .graphicsLayer { alpha = appearAlpha.value }
            .width(itemWidth)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        coil.compose.AsyncImage(
            model = imageModel,
            contentDescription = icon.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(12.dp))
        )
        icon.title?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = titleColor, fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontFamily = PersianFontFamily,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}
