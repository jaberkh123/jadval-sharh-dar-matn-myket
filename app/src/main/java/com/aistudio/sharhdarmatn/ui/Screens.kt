package com.aistudio.sharhdarmatn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.sharhdarmatn.data.SharhPuzzle
import com.aistudio.sharhdarmatn.data.SharhCellType
import com.aistudio.sharhdarmatn.data.normalizePersianChar
import com.aistudio.sharhdarmatn.ui.theme.LocalDarkTheme
import com.aistudio.sharhdarmatn.ui.theme.PersianFontFamily

// ───────────────────────── Helpers ─────────────────────────

fun String.toPersianDigits(): String =
    map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")

fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs).toPersianDigits()
}

/** گرفتن painter عکس جدول از drawable با نام رشته‌ای */
@Composable
fun puzzlePhotoPainter(resName: String): androidx.compose.ui.graphics.painter.Painter? {
    if (resName.isEmpty()) return null
    val context = LocalContext.current
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }
    return if (resId != 0) painterResource(resId) else null
}

/**
 * (قبلاً placeholder بود — از v2.2 بنر واقعی تبلیغات خودمان AdIconsBanner در همان‌جا نشسته است)
 */

// ───────────────────────── ریشهٔ برنامه ─────────────────────────

@Composable
fun AppContent(viewModel: PuzzleViewModel) {
    val isDark = false
    val currentScreen = viewModel.currentScreen

    BackHandler(enabled = currentScreen != Screen.HOME) {
        viewModel.handleBackPress()
    }

    val density = LocalDensity.current
    val fontScale = minOf(density.fontScale, 1.15f)

    CompositionLocalProvider(
        LocalDarkTheme provides isDark,
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density.density, fontScale = fontScale)
    ) {
        com.aistudio.sharhdarmatn.ui.theme.MyApplicationTheme(darkTheme = isDark) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.HOME -> LandingScreen(viewModel)
                            Screen.PUZZLES -> HomeScreen(viewModel)
                            Screen.GAME -> GameScreen(viewModel)
                            Screen.SETTINGS -> SettingsScreen(viewModel)
                            Screen.HELP -> HelpScreen(viewModel)
                        }
                    }

                    if (viewModel.showRatingDialog) {
                        RatingDialog(viewModel)
                    }
                }
            }
        }
    }
}

// ───────────────────────── صفحهٔ اول (Landing — مثل شهر جدول) ─────────────────────────

/**
 * صفحهٔ اول بازی (v2.2) — ساختار مثل «شهر جدول»:
 *  • سربرگ برند: آیکون اپ + نام + شعار
 *  • دکمهٔ «ادامهٔ جدول» برای جدول نیمه‌کاره — کوچک‌تر از شهر جدول (ردیف فشرده ۶۴dp)
 *  • بنر تبلیغات خودمان (روش تبلیغ) در وسط صفحه — ثابت و همیشه دیده‌شده
 *  • دکمهٔ اصلی «ورود به بازی» → صفحهٔ انتخاب جدول
 *  • دکمهٔ «خروج»
 *  • بدون دکمهٔ راهنمایی (طبق درخواست)
 */
@Composable
fun LandingScreen(viewModel: PuzzleViewModel) {
    val progressMap by viewModel.allProgress.collectAsState()
    val context = LocalContext.current

    // آخرین جدول نیمه‌کاره برای دکمهٔ «ادامهٔ جدول»
    val lastPlayed = progressMap.values.maxByOrNull { it.lastPlayedTime }
    val lastPuzzle = lastPlayed?.let {
        com.aistudio.sharhdarmatn.data.SharhPuzzleData.getPuzzleById(it.id)
    }
    val canContinue = lastPuzzle != null && lastPlayed?.isCompleted == false

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── بخش بالایی اسکرول‌شونده (برند + ادامهٔ جدول) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(6.dp))

                // آیکون اپ
                Image(
                    painter = painterResource(com.aistudio.sharhdarmatn.R.drawable.ic_brand),
                    contentDescription = "آیکون شرح در متن مشاهیر",
                    modifier = Modifier
                        .size(108.dp)
                        .shadow(6.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )

                // سربرگ برند
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "شرح در متن مشاهیر",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 26.sp
                        )
                    )
                    Text(
                        text = "شرحِ هر واژه، درون خودِ جدول است",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        ),
                        fontFamily = PersianFontFamily
                    )
                }

                // دکمهٔ «ادامهٔ جدول» — کوچک‌تر از شهر جدول: ردیف فشردهٔ ۶۴dp
                if (canContinue && lastPuzzle != null) {
                    val solved = solvedWordsFromProgress(lastPuzzle, lastPlayed?.userInput)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(4.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                SoundManager.playClick()
                                viewModel.startPuzzle(lastPuzzle)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "ادامهٔ جدول",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ادامهٔ جدول",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    ),
                                    fontFamily = PersianFontFamily
                                )
                                Text(
                                    text = "${solved.toString().toPersianDigits()} از ${lastPuzzle.words.size.toString().toPersianDigits()} واژه حل شده",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    ),
                                    fontFamily = PersianFontFamily
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ── بنر تبلیغات خودمان (روش تبلیغ) — وسط صفحه، ثابت و همیشه دیده‌شده ──
            AdIconsBanner()

            Spacer(Modifier.height(14.dp))

            // ── دکمهٔ اصلی: ورود به بازی ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .clickable {
                        SoundManager.playClick()
                        viewModel.navigateTo(Screen.PUZZLES)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ورود به بازی",
                            textAlign = TextAlign.Center,
                            fontFamily = PersianFontFamily,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "انتخاب جدول و شروع چالش",
                            textAlign = TextAlign.Center,
                            fontFamily = PersianFontFamily,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── دکمهٔ خروج (ثانویه — مثل شهر جدول) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { (context as? android.app.Activity)?.finish() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "خروج",
                        fontFamily = PersianFontFamily,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // پانوشت برند
            Text(
                text = "بازی شرح در متن مشاهیر",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                ),
                fontFamily = PersianFontFamily
            )
            Text(
                text = "نسخه ۲٫۲".toPersianDigits(),
                textAlign = TextAlign.Center,
                fontFamily = PersianFontFamily,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}

// ───────────────────────── صفحهٔ انتخاب جدول ─────────────────────────

@Composable
fun HomeScreen(viewModel: PuzzleViewModel) {
    val progressMap by viewModel.allProgress.collectAsState()
    val puzzles = com.aistudio.sharhdarmatn.data.SharhPuzzleData.puzzles

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        // سربرگ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "شرح در متن مشاهیر",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "شرحِ هر واژه، درون خودِ جدول است",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // سکه‌ها
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = if (LocalDarkTheme.current) com.aistudio.sharhdarmatn.ui.theme.DarkSurface else Color(0xFFADE8F4),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.5.dp,
                        if (LocalDarkTheme.current) com.aistudio.sharhdarmatn.ui.theme.DarkPrimary else Color(0xFF00B4D8),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "سکه",
                    tint = if (LocalDarkTheme.current) com.aistudio.sharhdarmatn.ui.theme.DarkOnPrimary else Color(0xFF0096C7),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = viewModel.getTotalCoins().toString().toPersianDigits(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (LocalDarkTheme.current) com.aistudio.sharhdarmatn.ui.theme.DarkPrimary else Color(0xFF0077B6)
                    )
                )
            }
        }

        // بنر تبلیغات خودمان (روش تبلیغ) — بالای صفحهٔ انتخاب جدول
        AdIconsBanner()

        Spacer(Modifier.height(4.dp))

        puzzles.forEachIndexed { index, puzzle ->
            HomePuzzleCard(viewModel, puzzle, progressMap[puzzle.id], number = index + 1)
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(4.dp))

        // دکمه‌های تنظیمات و راهنما
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("تنظیمات", fontFamily = PersianFontFamily, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.HELP) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("راهنما", fontFamily = PersianFontFamily, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun HomePuzzleCard(
    viewModel: PuzzleViewModel,
    puzzle: SharhPuzzle,
    progress: com.aistudio.sharhdarmatn.data.PuzzleProgressEntity?,
    number: Int
) {
    val solvedCount = remember(progress?.userInput) {
        solvedWordsFromProgress(puzzle, progress?.userInput)
    }
    val totalWords = puzzle.words.size
    val isCompleted = progress?.isCompleted == true
    val isStarted = progress != null && progress.userInput.any { it != ' ' }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.startPuzzle(puzzle) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // عکس موضوعی جدول
            val painter = puzzlePhotoPainter(puzzle.photoResName)
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 104.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF03045E).copy(alpha = 0.08f))
            ) {
                if (painter != null) {
                    // نسبت عکس با برش مرکزی حفظ می‌شود و هیچ‌گاه کشیده نمی‌شود
                    Image(
                        painter = painter,
                        contentDescription = "عکس جدول ${number.toString().toPersianDigits()}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // قابِ خالیِ عکس — تا وقتی سازنده عکس را اضافه کند
                    // (بدونِ نامِ شخصیت تا جواب لو نرود — قاعدهٔ v1.4)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "عکس",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03045E).copy(alpha = 0.45f),
                            fontFamily = PersianFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "تکمیل شده",
                        tint = Color(0xFF2E9E5B),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // بدونِ نامِ افراد — «جدول ۱۲» تا جواب لو نرود (قاعدهٔ v1.4)
                Text(
                    text = "جدول ${number.toString().toPersianDigits()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${puzzle.cols.toString().toPersianDigits()} ستون × ${puzzle.rows.toString().toPersianDigits()} ردیف | ${totalWords.toString().toPersianDigits()} واژه",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                Spacer(Modifier.height(8.dp))

                val frac = if (totalWords == 0) 0f else solvedCount.toFloat() / totalWords
                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (isCompleted) Color(0xFF2E9E5B) else Color(0xFFFFAFCC),
                    trackColor = Color(0xFFFFC8DD).copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${solvedCount.toString().toPersianDigits()} از ${totalWords.toString().toPersianDigits()} واژه حل شده",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { viewModel.startPuzzle(puzzle) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        isCompleted -> "مرور"
                        isStarted -> "ادامه"
                        else -> "شروع"
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = PersianFontFamily
                )
            }
        }
    }
}

/** تعداد واژه‌های درست‌حل‌شده از روی رشتهٔ پیشرفت ذخیره‌شده */
internal fun solvedWordsFromProgress(puzzle: SharhPuzzle, userInput: String?): Int {
    if (userInput == null) return 0
    val inputs = userInput.padEnd(puzzle.rows * puzzle.cols, ' ')
    return puzzle.words.count { word ->
        word.cells.all { (r, c) ->
            val ch = inputs.getOrNull(r * puzzle.cols + c) ?: ' '
            ch != ' ' && normalizePersianChar(ch) == normalizePersianChar(puzzle.cellAt(r, c).letter)
        }
    }
}

// ───────────────────────── دیالوگ امتیازدهی ─────────────────────────

@Composable
fun RatingDialog(viewModel: PuzzleViewModel) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { viewModel.dismissRatingDialog() },
        title = {
            Text(
                text = "حمایت از ما",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontFamily = PersianFontFamily
            )
        },
        text = {
            Text(
                text = "برای حمایت از ما و بهبود بازی، لطفاً نظر و امتیاز خود را ثبت کنید.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = PersianFontFamily
            )
        },
        confirmButton = {
            Button(onClick = { viewModel.rateApp(context) }) {
                Text("ثبت نظر", fontFamily = PersianFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissRatingDialog() }) {
                Text("بستن", fontFamily = PersianFontFamily)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ───────────────────────── Settings ─────────────────────────

@Composable
fun SettingsScreen(viewModel: PuzzleViewModel) {
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Text(
            text = "تنظیمات",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        SettingsToggleItem(
            title = "صدا",
            subtitle = "فعال یا غیرفعال کردن جلوه‌های صوتی بازی",
            checked = viewModel.isSoundEnabled,
            onToggle = { viewModel.toggleSound() }
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(8.dp))
            Text(
                "بازنشانی کل پیشرفت",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
                fontFamily = PersianFontFamily
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "شرح در متن مشاهیر | نسخه ۲٫۲",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("بازنشانی پیشرفت", fontFamily = PersianFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "آیا مطمئن هستید؟ تمام پیشرفت، زمان و سکه‌های شما پاک می‌شود.",
                    fontFamily = PersianFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        viewModel.resetAllProgress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("بله، پاک کن", fontFamily = PersianFontFamily) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("انصراف", fontFamily = PersianFontFamily) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// ───────────────────────── Help ─────────────────────────

@Composable
fun HelpScreen(viewModel: PuzzleViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Text(
            text = "راهنمای بازی",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(vertical = 14.dp)
        )

        val items = listOf(
            "در جدول «شرح در متن»، شرحِ هر واژه درون خودِ شبکه نوشته شده است؛ نه در فهرست کنار جدول.",
            "خانه‌های آبیِ روشن خانهٔ سرنخ هستند: متن سؤال در آن‌ها نوشته شده و فلشی که بیرونِ خانه و چسبیده به دیوارهٔ آن قرار دارد، جهت و محل شروع واژه را نشان می‌دهد.",
            "فلش چپ‌رو (بیرونِ دیوارِ چپِ خانهٔ سرنخ) یعنی واژهٔ افقی است و اولین حرفش بلافاصله سمت چپِ خانهٔ سرنخ قرار می‌گیرد (راست‌به‌چپ خوانده می‌شود).",
            "فلش پایین‌رو (بیرونِ دیوارِ پایینِ خانهٔ سرنخ) یعنی واژهٔ عمودی است و اولین حرفش بلافاصله زیرِ خانهٔ سرنخ قرار می‌گیرد.",
            "خانه‌های آبیِ هاشوردار فقط جداکننده‌اند و حرف و سؤالی ندارند.",
            "روی خانه‌های آبیِ روشنِ دوسؤالی دو سرنخ قرار دارد: نیمهٔ بالایی سرنخِ افقی و نیمهٔ پایینی سرنخِ عمودی است که با خط‌چین از هم جدا شده‌اند.",
            "خانه‌ها و سؤال‌های نارنجیِ ملایم، همه سؤال‌های موضوعیِ فردِ داخلِ عکس‌اند؛ اگر آن لاین‌ها را درست حل کنی، تمام رازهای زندگیِ شخصِ عکس پیدا می‌شود.",
            "با کلیک روی لاین، نوارِ سؤال همراه با کی‌بورد از پایینِ صفحه بالا می‌آید و سؤالِ همان لاین را نشان می‌دهد؛ با بسته شدنِ کی‌بورد، نوارِ سؤال هم همراهش پایین می‌رود.",
            "روی خانهٔ سرنخ کلیک کنید تا لاینِ همان سؤال انتخاب شود؛ در خانه‌های دوسؤالی، کلیکِ اول سؤالِ افقی و کلیکِ دوباره سؤالِ عمودی را نشان می‌دهد.",
            "روی هر خانهٔ حرف‌دار کلیک کنید تا کی‌بورد در پایین صفحه باز شود. کی‌بورد همیشه حروفِ همان لاین انتخاب‌شده را دارد.",
            "اگر کی‌بورد روی لاین انتخاب‌شده بیفتد، جدول خودکار کمی جابه‌جا می‌شود تا ابتدای لاین بالای کی‌بورد و نوارِ سؤال دیده شود؛ نوارِ سؤال و کی‌بورد هرگز لاینِ جواب را نمی‌پوشانند.",
            "با جابه‌جا کردن جدول، کی‌بورد به‌صورت خودکار محو می‌شود.",
            "عکسِ بالای جدول بخشی از سرنخ‌های موضوعی است؛ به آن دقت کنید.",
            "با دکمه‌های بزرگ‌نمایی یا حرکت دو انگشت (پینچ) می‌توانید جدول را زوم کنید.",
            "حروف درست به رنگ آبی و حروف اشتباه به رنگ قرمز نمایش داده می‌شوند."
        )

        items.forEachIndexed { index, text ->
            Row(modifier = Modifier.padding(vertical = 7.dp)) {
                Text(
                    text = "${(index + 1).toString().toPersianDigits()}.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
