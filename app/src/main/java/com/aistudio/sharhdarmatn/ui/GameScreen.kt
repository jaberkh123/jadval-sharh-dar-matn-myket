package com.aistudio.sharhdarmatn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.sharhdarmatn.data.SharhCell
import com.aistudio.sharhdarmatn.data.SharhCellType
import com.aistudio.sharhdarmatn.data.SharhWord
import com.aistudio.sharhdarmatn.ui.theme.LocalDarkTheme
import com.aistudio.sharhdarmatn.ui.theme.DarkPrimary
import com.aistudio.sharhdarmatn.ui.theme.DarkSurface
import com.aistudio.sharhdarmatn.ui.theme.PersianFontFamily

/**
 * صفحهٔ بازی «جدول شرح در متن».
 *
 * چیدمان (v2.0):
 *  - نوار بالا: بازگشت | عنوان | راهنمایی | سکه‌ها
 *  - جدول (زوم‌پذیر و اسکرول‌پذیر) — جایگاهِ تبلیغِ بالای جدول حذف شد تا جدول جادارتر شود
 *  - پایینِ صفحه: جایگاهِ «تبلیغ همسان» (Native Ad) در محلِ قبلیِ نوارِ سؤال
 *  - نوارِ سؤال + کیبورد: با کلیک روی یک لاین «همراهِ هم» از پایین بالا می‌آیند و رویِ
 *    جایگاهِ تبلیغ می‌نشینند؛ با بسته شدن با هم پایین می‌روند. با اسکرول جدول محو می‌شوند.
 */
@Composable
fun GameScreen(viewModel: PuzzleViewModel) {
    viewModel.activePuzzle ?: return
    val showHintMenuState = remember { mutableStateOf(false) }

    // تبلیغ میان‌صفحه‌ای ورودی (ادیوری): هر ۱۰ ورود به صفحهٔ حل جدول یک بار نمایش داده می‌شود
    val entryAdContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel.shouldShowEntryInterstitial) {
        if (viewModel.shouldShowEntryInterstitial) {
            viewModel.consumeEntryInterstitialFlag()
            viewModel.showEntryInterstitialAd(entryAdContext)
        }
    }

    // فلگ توقف اسکرول خودکار هنگام تعامل دستی کاربر با جدول
    var suppressAutoScroll by remember { mutableStateOf(false) }

    // ارتفاع واقعی کیبورد برحسب پیکسل — برای محاسبهٔ اسکرول خودکار
    val keyboardHeightPx = remember { mutableFloatStateOf(0f) }

    // ارتفاع نوارِ سؤال — برای اسکرول خودکار SharhGrid
    val questionBarHeightPx = remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val questionBarHeightDp = with(density) { questionBarHeightPx.floatValue.toDp() }

    // v2.4 — ارتفاعِ ثابتِ پنل‌های پایین (نوارِ سؤال و کیبورد): هر دو هم‌اندازه‌اند؛
    // مثال کاربر: یک‌دهمِ طولِ صفحه و تمامِ عرض — و رویِ تبلیغِ همسان می‌نشینند
    val panelHeightDp = (LocalConfiguration.current.screenHeightDp / 10).dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ─────────── نوار بالا ───────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.handleBackPress() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, shape = CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت"
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // بدونِ نامِ شخصیت — «جدول ۳» تا جواب لو نرود (قاعدهٔ v1.4)
                    Text(
                        text = "جدول ${viewModel.currentPuzzleNumber().toString().toPersianDigits()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "شرح در متن مشاهیر",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    )
                }

                // دکمهٔ راهنمایی (v2.1 — بزرگ‌تر با برچسب «راهنمایی»)
                Row(
                    modifier = Modifier
                        .clickable { showHintMenuState.value = true }
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(23.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f), RoundedCornerShape(23.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "راهنمایی",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "راهنمایی",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = PersianFontFamily
                    )
                }

                // سکه‌ها (بدون دکمهٔ + تبلیغ — تبلیغات حذف شده‌اند)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (LocalDarkTheme.current) DarkSurface else Color(0xFFADE8F4),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.2.dp,
                            if (LocalDarkTheme.current) DarkPrimary else Color(0xFF00B4D8),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "سکه",
                        tint = if (LocalDarkTheme.current) com.aistudio.sharhdarmatn.ui.theme.DarkOnPrimary else Color(0xFF0096C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = viewModel.getTotalCoins().toString().toPersianDigits(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (LocalDarkTheme.current) DarkPrimary else Color(0xFF0077B6)
                        )
                    )
                }
            }

            // ─────────── جدول ───────────
            // v2.3: تبلیغِ همسان دیگر overlay نیست و در چیدمان، زیرِ جدول نشسته —
            // جدولِ weight(1f) فقط تا بالای تبلیغ جا می‌گیرد (تبلیغ هرگز روی جدول نمی‌آید)
            // v2.4: پدینگِ پایین = مجموعِ ارتفاعِ پنل‌های باز (نوارِ سؤال و/یا کیبورد؛
            // هر کدام ثابت ۱/۱۰ صفحه) تا لاینِ فعال بالای پنل‌ها دیده شود
            val gridBottomInset by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (viewModel.activeWord != null) {
                    var inset = 0.dp
                    if (viewModel.isQuestionBarVisible) inset += panelHeightDp
                    if (viewModel.isKeyboardVisible) inset += panelHeightDp
                    inset
                } else 0.dp,
                animationSpec = tween(200)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = gridBottomInset)
            ) {
                SharhGrid(
                    viewModel = viewModel,
                    keyboardHeightPx = keyboardHeightPx,
                    questionBarHeightPx = questionBarHeightPx,
                    suppressAutoScroll = suppressAutoScroll,
                    onUserManualScroll = { suppressAutoScroll = true },
                    onCellInteracted = { suppressAutoScroll = false }
                )
            }

            // ── v2.3: جایگاه «تبلیغ همسان» در چیدمان (in-flow) — زیرِ جدول ──
            // همیشه دیده می‌شود و جدول را بالا می‌برد؛ هنگام باز شدن کیبورد،
            // پنلِ سؤال+کیبورد (overlay) روی آن می‌نشیند ولی نمونهٔ تبلیغ زنده
            // می‌ماند و دوباره لود نمی‌شود (قاعدهٔ «لودِ یک‌بار per ورود»).
            NativeAdSlot()

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        // ─────────── پنل‌های پایین (v2.4): نوارِ سؤال و کیبورد — دو پنلِ مستقل ───────────
        // هر دو اندازه‌ای ثابت و برابر دارند (۱/۱۰ ارتفاع صفحه، تمامِ عرض) و رویِ
        // جایگاهِ تبلیغِ همسان می‌نشینند؛ تبلیغ همیشه پایین جا دارد و زنده می‌ماند.
        //   • کلیک روی خانهٔ سؤال → فقط نوارِ سؤال بالا می‌آید
        //   • کلیک روی لاین (خانهٔ حرف‌دار) → فقط کیبورد بالا می‌آید
        //   • دکمهٔ «کیبورد» در نوارِ سؤال → کیبورد هم روی همان لاین باز می‌شود
        //   • جابه‌جا کردن جدول → هر دو محو می‌شوند (فقط تبلیغ همسان می‌ماند)
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            // ۱) نوارِ سؤال — فقط با کلیک روی خانهٔ سؤال بالا می‌آید
            AnimatedVisibility(
                visible = viewModel.isQuestionBarVisible && viewModel.activeWord != null,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(180))
            ) {
                QuestionBar(
                    word = viewModel.activeWord,
                    panelHeight = panelHeightDp,
                    isKeyboardVisible = viewModel.isKeyboardVisible,
                    onKeyboardToggle = {
                        if (viewModel.isKeyboardVisible) viewModel.hideKeyboardPanel()
                        else viewModel.showKeyboardFromQuestionBar()
                    },
                    onHeightChanged = { questionBarHeightPx.floatValue = it.toFloat() }
                )
            }

            // ۲) کیبورد — فقط با کلیک روی لاین یا دکمهٔ «کیبورد» (اسلاید از پایین)
            AnimatedVisibility(
                visible = viewModel.isKeyboardVisible && viewModel.activeWord != null,
                enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(panelHeightDp)
                        .onSizeChanged { keyboardHeightPx.floatValue = it.height.toFloat() }
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
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    PersianOnScreenKeyboard(
                        word = viewModel.activeWord,
                        onCharTyped = { viewModel.onKeyPressed(it) },
                        onBackspace = { viewModel.onBackspacePressed() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // نوارِ سیستم (نویگیشن‌بار) — زیرِ کلِ پنل‌ها
            Spacer(Modifier.navigationBarsPadding())
        }
    } // پایان Box ریشه

    // ─────────── دیالوگ‌ها ───────────
    // پنجرهٔ سؤالِ وسطِ صفحه (v1.5) از v1.9 با «نوارِ سؤالِ پایینِ صفحه» جایگزین شده است
    if (showHintMenuState.value) {
        HintMenuDialog(
            onRevealLetter = { viewModel.useHintRevealLetter(); showHintMenuState.value = false },
            onRevealWord = { viewModel.useHintRevealWord(); showHintMenuState.value = false },
            onClearWrong = { viewModel.useHintClearWrong(); showHintMenuState.value = false },
            onDismiss = { showHintMenuState.value = false }
        )
    }

    if (viewModel.showHintResultDialog) {
        HintResultDialog(viewModel)
    }

    if (viewModel.showCompletedDialog) {
        GameCompletionDialog(viewModel)
    }

    if (viewModel.showIncorrectCompletionDialog) {
        GameIncorrectCompletionDialog(viewModel)
    }

    if (viewModel.showOnboarding) {
        OnboardingDialog(viewModel)
    }
}

// ───────────────────────── پنجرهٔ سؤال (قاعدهٔ v1.5) ─────────────────────────

/** کارت یک سؤال داخل پنجره — سؤال‌های فردِ عکس با نارنجیِ ملایم نشان داده می‌شوند */
@Composable
private fun QuestionCard(
    directionLabel: String,
    text: String,
    length: Int,
    isPhoto: Boolean,
    onClick: (() -> Unit)?
) {
    val cardBg = if (isPhoto) Color(0xFFFFF1E0) else Color(0xFFEAF6FD)
    val cardBorder = if (isPhoto) Color(0xFFE8590C).copy(alpha = 0.45f)
    else Color(0xFF0096C7).copy(alpha = 0.35f)
    val badgeBg = if (isPhoto) Color(0xFFE8590C).copy(alpha = 0.14f) else Color(0xFF0096C7).copy(alpha = 0.12f)
    val badgeFg = if (isPhoto) Color(0xFFD9480F) else Color(0xFF0077B6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(14.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = directionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = badgeFg, fontWeight = FontWeight.ExtraBold
                    ),
                    fontFamily = PersianFontFamily
                )
            }
            if (isPhoto) {
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "مربوط به عکس",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = badgeFg, fontWeight = FontWeight.ExtraBold
                        ),
                        fontFamily = PersianFontFamily
                    )
                }
            }
            if (length > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${length.toString().toPersianDigits()} حرف",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = PersianFontFamily
                    )
                }
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold
            ),
            fontFamily = PersianFontFamily
        )
    }
}

/**
 * نوارِ سؤالِ پایینِ صفحه (بازطراحیِ v2.4 — قاعدهٔ کاربر):
 * پنلِ ثابتِ هم‌اندازهٔ کیبورد (۱/۱۰ ارتفاع صفحه، تمامِ عرض) که رویِ تبلیغِ همسان
 * می‌نشیند و فقط با کلیک روی خانهٔ سؤال بالا می‌آید.
 *  • محتوا: بَجِ جهت (افقی/عمودی) + بَج «مربوط به عکس» + تعداد حروف + متنِ سؤال
 *    (اگر بلند باشد داخلِ خودِ نوار اسکرول می‌شود)
 *  • گوشهٔ چپِ نوار: دکمهٔ «کیبورد» — کلیک → کیبورد بالای همان لاین باز/بسته می‌شود
 */
@Composable
fun QuestionBar(
    word: SharhWord?,
    panelHeight: Dp,
    isKeyboardVisible: Boolean,
    onKeyboardToggle: () -> Unit,
    onHeightChanged: (Int) -> Unit
) {
    if (word == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .onSizeChanged { onHeightChanged(it.height) }
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // بَج‌ها + متنِ سؤال (اسکرول‌پذیر داخلِ نوار)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuestionBadge(
                        text = if (word.isHorizontal) "افقی" else "عمودی",
                        isPhoto = false
                    )
                    if (word.isPhoto) {
                        QuestionBadge(text = "مربوط به عکس", isPhoto = true)
                    }
                    QuestionBadge(
                        text = "${word.length.toString().toPersianDigits()} حرف",
                        isPhoto = false,
                        neutral = true
                    )
                }
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = word.clue,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = PersianFontFamily
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // دکمهٔ «کیبورد» — گوشهٔ چپِ نوار (انتهای ردیف در RTL)؛ کلیک → باز/بسته شدن کیبورد
            Row(
                modifier = Modifier
                    .clickable { onKeyboardToggle() }
                    .background(
                        if (isKeyboardVisible) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isKeyboardVisible) 1f else 0.4f
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = if (isKeyboardVisible) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "کیبورد",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isKeyboardVisible) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    ),
                    fontFamily = PersianFontFamily
                )
            }
        }
    }
}

/** بَج کوچک (جهت / مربوط به عکس / تعداد حروف) داخل نوارِ سؤالِ v2.4 */
@Composable
private fun QuestionBadge(text: String, isPhoto: Boolean, neutral: Boolean = false) {
    val bg = when {
        isPhoto -> Color(0xFFE8590C).copy(alpha = 0.14f)
        neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
        else -> Color(0xFF0096C7).copy(alpha = 0.12f)
    }
    val fg = when {
        isPhoto -> Color(0xFFD9480F)
        neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        else -> Color(0xFF0077B6)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp
            ),
            fontFamily = PersianFontFamily
        )
    }
}

// ───────────────────────── منوی راهنمایی ─────────────────────────

@Composable
fun HintMenuDialog(
    onRevealLetter: () -> Unit,
    onRevealWord: () -> Unit,
    onClearWrong: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "انتخاب نوع راهنمایی",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontFamily = PersianFontFamily
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRevealLetter,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("آشکار کردن حرف خانهٔ فعال (−۳۰ سکه)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRevealWord,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("آشکار کردن کل کلمهٔ فعال (−۱۰۰ سکه)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onClearWrong,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "حذف تمام حروف غلط جدول (−۳۰ سکه)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", fontFamily = PersianFontFamily) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ───────────────────────── نتیجهٔ راهنما ─────────────────────────

@Composable
fun HintResultDialog(viewModel: PuzzleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissHintResultDialog() },
        title = {
            Text(
                text = viewModel.hintDialogTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontFamily = PersianFontFamily
            )
        },
        text = {
            Text(
                text = viewModel.hintDialogContent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = PersianFontFamily
            )
        },
        confirmButton = {
            Button(
                onClick = { viewModel.dismissHintResultDialog() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بستن", fontWeight = FontWeight.Bold, fontFamily = PersianFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ───────────────────────── دیالوگ تکمیل جدول ─────────────────────────

@Composable
fun GameCompletionDialog(viewModel: PuzzleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.closeCompletedDialog() },
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF2E9E5B),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "جدول حل شد!",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontFamily = PersianFontFamily,
                    fontSize = 24.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "زمان: ${formatTime(viewModel.timerSeconds)}",
                    fontFamily = PersianFontFamily,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "واژه‌های حل‌شده: ${viewModel.countSolvedWords().toString().toPersianDigits()}",
                    fontFamily = PersianFontFamily,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "۱۰۰ سکه جایزه گرفتی!",
                    fontFamily = PersianFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0096C7)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.closeCompletedDialog() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بستن", fontWeight = FontWeight.Bold, fontFamily = PersianFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    )
}

// ───────────────────────── دیالوگ جدول پرشده ولی نادرست ─────────────────────────

@Composable
fun GameIncorrectCompletionDialog(viewModel: PuzzleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.closeIncorrectCompletionDialog() },
        title = {
            Text(
                text = "چند جا هنوز اشتباه است",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontFamily = PersianFontFamily
            )
        },
        text = {
            Text(
                text = "همهٔ خانه‌ها پر شده‌اند ولی بعضی حروف درست نیستند. حروف قرمز را بازبینی کن.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = PersianFontFamily
            )
        },
        confirmButton = {
            Button(
                onClick = { viewModel.closeIncorrectCompletionDialog() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بسیار خب", fontWeight = FontWeight.Bold, fontFamily = PersianFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ───────────────────────── دیالوگ آموزش اولیه ─────────────────────────

@Composable
fun OnboardingDialog(viewModel: PuzzleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissOnboarding() },
        title = {
            Text(
                text = "راهنمای کوتاه بازی",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "۱) شرحِ هر واژه درون خودِ جدول نوشته شده و فلشِ کنار آن، جهت و محل شروع واژه را نشان می‌دهد.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "۲) روی خانهٔ سرنخ (آبی یا نارنجی) کلیک کنی، لاین همان سؤال انتخاب می‌شود و نوارِ سؤال از پایینِ صفحه بالا می‌آید؛ با دکمهٔ «کیبورد» در گوشهٔ نوار یا کلیک روی خانه‌های حرف‌دارِ همان لاین، کیبورد باز می‌شود. در خانهٔ دوسؤالی، کلیکِ اول سؤالِ افقی و کلیکِ بعدی سؤالِ عمودی را نشان می‌دهد. سؤال‌های نارنجی مربوط به فردِ داخل عکس‌اند.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "۳) کیبورد حروفِ همان لاین انتخاب‌شده را دارد و با جابه‌جا کردن جدول محو می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "۴) عکس بالای جدول بخشی از سؤال‌های موضوعی است.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.dismissOnboarding() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بسیار خب، شروع بازی!", fontWeight = FontWeight.Bold, fontFamily = PersianFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
