package com.aistudio.sharhdarmatn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aistudio.sharhdarmatn.R

// Custom Persian fonts — Vazirmatn (همان فونتِ مخزنِ جدول‌ساز «شرح در متن» — fonts/Vazirmatn)
// رندرِ PNG جدول‌ها با همین فونت است؛ اپ هم با Vazirmatn هم‌شکلِ نسخهٔ کاغذی می‌شود
// Body texts: Vazirmatn Regular + Bold (no faux bold!)
// Display / titles: Vazirmatn Black for extra impact

val PersianFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, weight = FontWeight.Normal),
    Font(R.font.vazirmatn_bold, weight = FontWeight.Bold),
)

val PersianDisplayFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, weight = FontWeight.Normal),
    Font(R.font.vazirmatn_bold, weight = FontWeight.Bold),
    Font(R.font.vazirmatn_black, weight = FontWeight.Black),
)

// Set of Material typography styles using Vazirmatn Persian fonts
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = PersianFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PersianFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PersianFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PersianDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PersianDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PersianDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PersianFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PersianFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PersianFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
