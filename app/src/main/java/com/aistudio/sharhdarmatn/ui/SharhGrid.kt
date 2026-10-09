package com.aistudio.sharhdarmatn.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.aistudio.sharhdarmatn.data.SharhCell
import com.aistudio.sharhdarmatn.data.SharhCellType
import com.aistudio.sharhdarmatn.ui.theme.CorrectLight
import com.aistudio.sharhdarmatn.ui.theme.CorrectTextLight
import com.aistudio.sharhdarmatn.ui.theme.EmptyCellLight
import com.aistudio.sharhdarmatn.ui.theme.HighlightedLight
import com.aistudio.sharhdarmatn.ui.theme.IncorrectLight
import com.aistudio.sharhdarmatn.ui.theme.IncorrectTextLight
import com.aistudio.sharhdarmatn.ui.theme.LocalDarkTheme
import com.aistudio.sharhdarmatn.ui.theme.PersianFontFamily
import com.aistudio.sharhdarmatn.ui.theme.SelectedLight
import kotlin.math.roundToInt

// رنگ‌های خانه‌های شرح در متن (قاعدهٔ کاربر v1.4 — پالت پاستلی: CDB4DB / FFC8DD / FFAFCC / BDE0FE / A2D2FF)
// سرنخ‌ها (تک‌سؤالی و دوسؤالی) آبیِ پاستلی، جداکننده‌ها از v1.8 آبیِ هاشوردار،
// سرنخ و لاینِ «همهٔ سؤال‌های موضوعیِ فردِ عکس» نارنجیِ ملایم‌اند (قاعدهٔ v1.5 —
// در v1.4 فقط تکه‌های نام نارنجی می‌شد و کاربر گفت «فقط یکی یا دو تا نارنجی میشن»)
// قاعدهٔ v1.8: فلش‌ها بیرونِ خانه و چسبیده به دیواره رسم می‌شوند تا کلِ فضای خانه
// به متنِ سؤال برسد؛ فونت سؤال +۲ واحد؛ پدینگِ لبه‌ها کمتر؛ جداکننده آبیِ هاشوردار.
val ClueCellBg = Color(0xFFBDE0FE)      // آبیِ پاستلی سرنخ
val SplitClueCellBg = Color(0xFFCDB4DB) // بنفشِ پاستلی سرنخ دوسؤالی (بدونِ استفاده — از v1.7 همه سرنخ‌ها آبی‌اند)
val ClueTextColor = Color(0xFF1A1A2E)
val ArrowColor = Color(0xFF03045E)

// خانهٔ جداکنندهٔ «آبیِ هاشوردار» — قاعدهٔ کاربر v1.8 (جای مشکیِ توپ)
val BlockBgLight = Color(0xFFD8EAF9)     // زمینهٔ آبیِ روشن
val BlockHatchLight = Color(0xFF85B3DE)  // خط‌های هاشورِ مورب
val BlockBgDark = Color(0xFF1B3A57)      // نسخهٔ تیره
val BlockHatchDark = Color(0xFF3F6B94)

// خانوادهٔ نارنجیِ ملایم — برای سرنخ‌ها و لاین‌های «همهٔ سؤال‌های موضوعیِ فردِ داخل عکس»
val PhotoClueCellBg = Color(0xFFFFD8A8)    // سرنخِ سؤال‌های موضوعیِ فردِ عکس
val PhotoLineBg = Color(0xFFFFE8CC)        // خانه‌های حرفِ لاین‌های موضوعیِ فردِ عکس
val PhotoSelectedLight = Color(0xFFFFC078) // خانهٔ فعال روی لاینِ عکس
val PhotoHighlightedLight = Color(0xFFFFD8A8)
val PhotoSelectedBorder = Color(0xFFE8590C)
val PhotoHighlightedBorder = Color(0xFFFF922B)

/**
 * جدول «شرح در متن» — زوم‌پذیر (دکمه + پینچ)، اسکرول‌پذیر و با اسکرول خودکار هوشمند:
 *
 * قاعدهٔ کاربر: اگر کیبوردِ باز‌شده روی لاینِ کلیک‌شده بیفتد، جدول کمی جابه‌جا می‌شود
 * تا «ابتدای لاین» به وسطِ ناحیهٔ خالی (بالای کیبورد) بیاید.
 * وقتی کاربر خودش جدول را جابه‌جا می‌کند، کیبورد محو و اسکرول خودکار متوقف می‌شود.
 */
@Composable
fun SharhGrid(
    viewModel: PuzzleViewModel,
    keyboardHeightPx: androidx.compose.runtime.MutableFloatState,
    questionBarHeightPx: androidx.compose.runtime.MutableFloatState,
    suppressAutoScroll: Boolean,
    onUserManualScroll: () -> Unit,
    onCellInteracted: () -> Unit
) {
    val puzzle = viewModel.activePuzzle ?: return

    // زوم پیوسته (۱ تا ۲٫۵ برابر)
    var zoomF by remember(puzzle.id) { mutableFloatStateOf(1f) }

    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val density = LocalDensity.current

    val word = viewModel.activeWord
    val highlightedCells = remember(word) { word?.cells?.toHashSet() ?: emptySet() }
    val photoWordCells = remember(puzzle.id) { puzzle.photoWordCells }
    val photoClueCells = remember(puzzle.id) { puzzle.photoClueCells }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ───── نوار زوم ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = { zoomF = (((zoomF * 100f).roundToInt() - 20).coerceAtLeast(100)) / 100f },
                        enabled = zoomF > 1.01f,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("−", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (zoomF > 1.01f) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                    Text(
                        text = "${(zoomF * 100f).roundToInt().toString().toPersianDigits()}٪",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.material3.IconButton(
                        onClick = { zoomF = (((zoomF * 100f).roundToInt() + 20).coerceAtMost(250)) / 100f },
                        enabled = zoomF < 2.49f,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (zoomF < 2.49f) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "پینچ دو انگشت هم کار می‌کند",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }

        // ───── ناحیهٔ جدول ─────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val viewportWpx = constraints.maxWidth.toFloat()
            val viewportHpx = constraints.maxHeight.toFloat()
            val vpMinWidth = maxWidth   // برای وسط‌چین‌کردنِ محتوای اسکرول‌پذیر

            val baseCell = minOf(maxWidth / puzzle.cols, maxHeight / puzzle.rows)
            val cellSize = baseCell * zoomF
            val gridWidth = cellSize * puzzle.cols
            val gridHeight = cellSize * puzzle.rows

            // v2.5: کیبورد داخلِ اسلاتِ ثابتِ زیرِ جدول است و هرگز جدول را نمی‌پوشاند —
            // پس فقط وقتی محتوا واقعاً بزرگ‌تر از دید است اسکرول/پن فعال می‌شود
            val isScrollableActive =
                gridWidth > maxWidth || gridHeight > maxHeight || zoomF > 1.001f

            val containerModifier = if (isScrollableActive) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .size(width = gridWidth, height = gridHeight)
                    .align(Alignment.Center)
            }

            // ═════ اسکرول خودکار هوشمند ═════
            LaunchedEffect(
                viewModel.activeWord?.id,
                viewModel.activeRow,
                viewModel.activeCol,
                viewModel.isKeyboardVisible,
                keyboardHeightPx.floatValue,
                questionBarHeightPx.floatValue, // فیکس v1.9: تغییرِ ارتفاعِ نوارِ سؤال هم اسکرول را بازمحاسبه کند
                zoomF,
                suppressAutoScroll
            ) {
                if (suppressAutoScroll) return@LaunchedEffect
                val activeWord = viewModel.activeWord ?: return@LaunchedEffect
                if (viewModel.activeRow == -1 || viewModel.activeCol == -1) return@LaunchedEffect

                kotlinx.coroutines.delay(80) // فرصت چیدمان

                val cellPx = with(density) { cellSize.toPx() }
                // v2.5: viewportِ جدول ثابت است (اسلاتِ پایین همیشه هم‌اندازه است و
                // کیبورد جدول را نمی‌پوشاند) — پس ارتفاعِ دید همان ارتفاعِ خودِ جدول‌خانه است
                val visibleH = viewportHpx.coerceAtLeast(cellPx)
                val marginPx = with(density) { 6.dp.toPx() }
                // فضای تنفسِ لبه‌ها — «بینِ جدول و اسلاتِ پایین فضای خالیِ زیاد نباشد»
                val breathe = cellPx * 0.9f
                // فاصلهٔ ملایمِ لاینِ فعال از لبهٔ پایینِ دید (بالای اسلاتِ ثابت)
                val bottomGap = cellPx * 0.9f

                val curLeft = hScroll.value.toFloat()
                val curTop = vScroll.value.toFloat()
                // v2.5: «اتاقِ اسکرولِ زیرِ جدول» حذف شد — کیبورد دیگر رویِ جدول نمی‌آید
                val contentHpx = with(density) { gridHeight.toPx() }
                val maxScrollX = with(density) { gridWidth.toPx() - viewportWpx }.coerceAtLeast(0f)
                val maxScrollY = (contentHpx - viewportHpx).coerceAtLeast(0f)

                if (viewModel.isKeyboardVisible) {
                    // ══ الگوریتم «دیدِ راحت»: مستطیلِ لاین باید با فاصلهٔ تنفس داخلِ
                    // ناحیهٔ دید (بالای کیبورد) بماند؛ کم‌ترین جابه‌جایی ممکن ══
                    val minR = activeWord.cells.minOf { it.first }
                    val maxR = activeWord.cells.maxOf { it.first }
                    val minC = activeWord.cells.minOf { it.second }
                    val maxC = activeWord.cells.maxOf { it.second }
                    val bx0 = minC * cellPx
                    val bx1 = (maxC + 1) * cellPx
                    val by0 = minR * cellPx
                    val by1 = (maxR + 1) * cellPx

                    // ── عمودی: پایین (کیبورد) و بالا ══
                    var targetY = curTop
                    if (by1 > targetY + visibleH - bottomGap) targetY = by1 + bottomGap - visibleH
                    if (by0 < targetY + marginPx) targetY = by0 - marginPx
                    // لاینِ بلندتر از ناحیهٔ دید → ابتدای لاین وسطِ ناحیهٔ دید
                    if (by1 - by0 > visibleH - bottomGap - marginPx) {
                        targetY = activeWord.cells[0].first * cellPx + cellPx / 2f - visibleH / 2f
                    }
                    targetY = targetY.coerceIn(0f, maxScrollY)
                    if (kotlin.math.abs(targetY - curTop) > 1f) {
                        vScroll.animateScrollTo(targetY.roundToInt(), tween(400, easing = FastOutSlowInEasing))
                    }

                    // ── افقی: لبه‌های چپ و راست هم با فاصلهٔ تنفس ══
                    var targetX = curLeft
                    if (bx0 < targetX + breathe) targetX = bx0 - breathe
                    if (bx1 > targetX + viewportWpx - breathe) targetX = bx1 + breathe - viewportWpx
                    // لاینِ پهن‌تر از دید → ابتدای لاین وسط
                    if (bx1 - bx0 > viewportWpx - 2 * breathe) {
                        targetX = activeWord.cells[0].second * cellPx + cellPx / 2f - viewportWpx / 2f
                    }
                    targetX = targetX.coerceIn(0f, maxScrollX)
                    if (kotlin.math.abs(targetX - curLeft) > 1f) {
                        hScroll.animateScrollTo(targetX.roundToInt(), tween(400, easing = FastOutSlowInEasing))
                    }
                } else {
                    // ── بدون کیبورد: فقط خانهٔ فعال باید دیده شود ──
                    val cellX0 = viewModel.activeCol * cellPx
                    val cellX1 = cellX0 + cellPx
                    val cellY0 = viewModel.activeRow * cellPx
                    val cellY1 = cellY0 + cellPx

                    if (cellY0 < curTop || cellY1 > curTop + viewportHpx) {
                        val targetY = (cellY0 + cellPx / 2f - viewportHpx / 2f).coerceIn(0f, maxScrollY)
                        vScroll.animateScrollTo(targetY.roundToInt(), tween(350, easing = FastOutSlowInEasing))
                    }
                    if (cellX0 < curLeft || cellX1 > curLeft + viewportWpx) {
                        val targetX = (cellX0 + cellPx / 2f - viewportWpx / 2f).coerceIn(0f, maxScrollX)
                        hScroll.animateScrollTo(targetX.roundToInt(), tween(350, easing = FastOutSlowInEasing))
                    }
                }
            }

            // ═════ ظرف اصلی ═════
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = containerModifier
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isScrollableActive) {
                                    Modifier
                                        .pointerInput(puzzle.id) {
                                            // پینچ‌زوم + درگ (اسکرول) — هر دو کیبورد را محو می‌کنند
                                            detectTransformGestures { centroid, pan, gestureZoom, _ ->
                                                if (pan != Offset.Zero || gestureZoom != 1f) {
                                                    onUserManualScroll()
                                                    viewModel.hideKeyboard()
                                                }
                                                if (gestureZoom != 1f) {
                                                    val oldZoom = zoomF
                                                    val newZoom = (oldZoom * gestureZoom).coerceIn(1f, 2.5f)
                                                    if (newZoom != oldZoom) {
                                                        val baseCellPx = baseCell.toPx()
                                                        val contentX = centroid.x + hScroll.value
                                                        val contentY = centroid.y + vScroll.value
                                                        val ratio = newZoom / oldZoom
                                                        val newMaxX = (baseCellPx * newZoom * puzzle.cols - viewportWpx).coerceAtLeast(0f)
                                                        val newMaxY = (baseCellPx * newZoom * puzzle.rows - viewportHpx).coerceAtLeast(0f)
                                                        val targetX = (contentX * ratio - centroid.x).coerceIn(0f, newMaxX)
                                                        val targetY = (contentY * ratio - centroid.y).coerceIn(0f, newMaxY)
                                                        zoomF = newZoom
                                                        hScroll.dispatchRawDelta(targetX - hScroll.value)
                                                        vScroll.dispatchRawDelta(targetY - vScroll.value)
                                                    }
                                                }
                                                if (pan != Offset.Zero) {
                                                    hScroll.dispatchRawDelta(-pan.x)
                                                    vScroll.dispatchRawDelta(-pan.y)
                                                }
                                            }
                                        }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        // محتوای اسکرول‌پذیر: «جدول + عکس»؛
                        // CenterHorizontally تا جدولِ باریک‌تر از صفحه وسط‌چین بماند
                        Column(
                            modifier = (if (isScrollableActive) Modifier.widthIn(min = vpMinWidth) else Modifier)
                                .verticalScroll(vScroll, enabled = false)
                                .horizontalScroll(hScroll, enabled = false),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(width = gridWidth, height = gridHeight)
                            ) {
                        // شبکهٔ خانه‌ها (LTR برای هم‌ترازی درست ستون‌ها با مختصات داده)
                        Column(
                            modifier = Modifier.size(width = gridWidth, height = gridHeight)
                        ) {
                            for (row in 0 until puzzle.rows) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (col in 0 until puzzle.cols) {
                                        val cell = puzzle.cellAt(row, col)
                                        val isSelected = viewModel.activeRow == row && viewModel.activeCol == col
                                        val isHighlighted = (row to col) in highlightedCells
                                        val hasInput = viewModel.userGridInputs.getOrNull(puzzle.flatIndex(row, col)) ?: ' ' != ' '
                                        val isCorrect = viewModel.isCellCorrect(row, col)

                                        key(puzzle.id, row, col) {
                                            SharhCellView(
                                                cell = cell,
                                                cellSize = cellSize,
                                                userChar = viewModel.userGridInputs.getOrNull(puzzle.flatIndex(row, col)) ?: ' ',
                                                isSelected = isSelected,
                                                isHighlighted = isHighlighted,
                                                hasInput = hasInput,
                                                isCorrect = isCorrect,
                                                isPhotoCell = (row to col) in photoWordCells,
                                                isPhotoClue = (row to col) in photoClueCells,
                                                onClick = {
                                                    when (cell.type) {
                                                        SharhCellType.ANSWER -> {
                                                            onCellInteracted()
                                                            viewModel.onAnswerCellClicked(row, col)
                                                        }
                                                        SharhCellType.CLUE, SharhCellType.SPLIT_CLUE -> {
                                                            // فیکس v1.7: مثل کلیک روی لاین، پرچمِ اسکرولِ دستی ریست شود تا
                                                            // اسکرولِ خودکار بعد از پن/زومِ دستی هم برای «کلیک روی سؤال» کار کند
                                                            onCellInteracted()
                                                            viewModel.onClueCellClicked(row, col)
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ───── فلش‌های جهت‌نما — بیرونِ خانه و چسبیده به دیواره (قاعدهٔ کاربر v1.8) ─────
                        // یک لایهٔ رویی روی همهٔ خانه‌ها رسم می‌شود تا فلش زیرِ خانه‌های مجاور دفن نشود:
                        //   فلشِ افقی (چپ‌رو): بیرونِ دیوارِ چپِ خانهٔ سرنخ و چسبیده به آن
                        //   فلشِ عمودی (پایین‌رو): بیرونِ دیوارِ پایینِ خانهٔ سرنخ و چسبیده به آن
                        // این‌طوری کلِ فضای داخلِ خانه به متنِ سؤال می‌رسد.
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val cellPx = cellSize.toPx()
                            val half = cellPx * 0.085f // نیم‌ضخامتِ پایهٔ فلش
                            val len = cellPx * 0.17f   // طولِ نوکِ فلش
                            for (r in 0 until puzzle.rows) {
                                for (c in 0 until puzzle.cols) {
                                    val decorCell = puzzle.cellAt(r, c)
                                    if (!decorCell.isClueCell) continue
                                    val x0 = c * cellPx
                                    val y0 = r * cellPx
                                    if (decorCell.hasArrowH) {
                                        // هم‌تراز با سؤالِ افقی: تک‌سؤالی وسطِ خانه، دوسؤالی وسطِ نیمهٔ بالا
                                        val cy = y0 + cellPx * (if (decorCell.hasArrowV) 0.25f else 0.50f)
                                        val p = Path().apply {
                                            moveTo(x0, cy - half)
                                            lineTo(x0, cy + half)
                                            lineTo(x0 - len, cy)
                                            close()
                                        }
                                        drawPath(p, ArrowColor)
                                    }
                                    if (decorCell.hasArrowV) {
                                        val cx = x0 + cellPx * 0.50f
                                        val y1 = y0 + cellPx
                                        val p = Path().apply {
                                            moveTo(cx - half, y1)
                                            lineTo(cx + half, y1)
                                            lineTo(cx, y1 + len)
                                            close()
                                        }
                                        drawPath(p, ArrowColor)
                                    }
                                }
                            }
                        }

                        // ───── عکس موضوعی جدول (با برش مرکزی — نسبت عکس همیشه حفظ می‌شود) ─────
                        val photoPainter = if (puzzle.hasPhoto) puzzlePhotoPainter(puzzle.photoResName) else null
                        if (photoPainter != null) {
                            val rect = puzzle.photoRect
                            Image(
                                painter = photoPainter,
                                contentDescription = "عکس موضوعی جدول",
                                modifier = Modifier
                                    .offset(x = cellSize * rect[2], y = cellSize * rect[0])
                                    .size(width = cellSize * (rect[3] - rect[2] + 1), height = cellSize * (rect[1] - rect[0] + 1))
                                    .border(1.dp, ArrowColor),
                                contentScale = ContentScale.Crop
                            )
                        } else if (puzzle.photoRect.size == 4) {
                            // قابِ خالیِ عکس — کادرِ نارنجیِ ملایم + برچسبِ «عکس»
                            // (نامِ شخصیت نمایش داده نمی‌شود تا جواب لو نرود — قاعدهٔ v1.4)
                            val rect = puzzle.photoRect
                            Box(
                                modifier = Modifier
                                    .offset(x = cellSize * rect[2], y = cellSize * rect[0])
                                    .size(width = cellSize * (rect[3] - rect[2] + 1), height = cellSize * (rect[1] - rect[0] + 1))
                                    .background(PhotoClueCellBg.copy(alpha = 0.40f))
                                    .border(1.dp, PhotoSelectedBorder.copy(alpha = 0.50f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "عکس",
                                    fontSize = (cellSize.value * 0.60f).coerceAtMost(26f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ArrowColor.copy(alpha = 0.65f),
                                    fontFamily = PersianFontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                            } // پایانِ Boxِ «جدول + عکس»
                        } // پایانِ Columnِ اسکرول‌پذیر
                    }
                }
            }
        }
    }
}

// ───────────────────────── یک خانهٔ جدول ─────────────────────────

@Composable
fun SharhCellView(
    cell: SharhCell,
    cellSize: Dp,
    userChar: Char,
    isSelected: Boolean,
    isHighlighted: Boolean,
    hasInput: Boolean,
    isCorrect: Boolean,
    isPhotoCell: Boolean = false,
    isPhotoClue: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val sharedInteractionSource = remember { MutableInteractionSource() }

    val cellBg: Color = when (cell.type) {
        SharhCellType.BLOCK -> if (isDark) BlockBgDark else BlockBgLight // آبیِ هاشوردار (قاعدهٔ v1.8)
        SharhCellType.PHOTO -> Color.Transparent
        SharhCellType.CLUE -> if (isPhotoClue) PhotoClueCellBg else ClueCellBg
        SharhCellType.SPLIT_CLUE -> if (isPhotoClue) PhotoClueCellBg else ClueCellBg // قاعدهٔ کاربر v1.7: دوسؤالی هم مثل تک‌سؤالی آبی (بنفش حذف شد)
        SharhCellType.ANSWER -> when {
            isSelected ->
                if (isPhotoCell) PhotoSelectedLight
                else if (isDark) com.aistudio.sharhdarmatn.ui.theme.SelectedDark else SelectedLight
            hasInput && isCorrect -> if (isDark) com.aistudio.sharhdarmatn.ui.theme.CorrectDark else CorrectLight
            hasInput && !isCorrect -> if (isDark) com.aistudio.sharhdarmatn.ui.theme.IncorrectDark else IncorrectLight
            isHighlighted ->
                if (isPhotoCell) PhotoHighlightedLight
                else if (isDark) com.aistudio.sharhdarmatn.ui.theme.HighlightedDark else HighlightedLight
            isPhotoCell -> PhotoLineBg   // لاین‌های موضوعیِ فردِ عکس — نارنجیِ ملایمِ همیشگی
            else -> if (isDark) com.aistudio.sharhdarmatn.ui.theme.EmptyCellDark else EmptyCellLight
        }
    }

    val textColor: Color = when (cell.type) {
        SharhCellType.ANSWER -> when {
            hasInput && isCorrect -> if (isDark) com.aistudio.sharhdarmatn.ui.theme.CorrectTextDark else CorrectTextLight
            hasInput && !isCorrect -> if (isDark) com.aistudio.sharhdarmatn.ui.theme.IncorrectTextDark else IncorrectTextLight
            else -> MaterialTheme.colorScheme.onSurface
        }
        else -> ClueTextColor
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(cellBg)
            .then(
                when (cell.type) {
                    SharhCellType.BLOCK -> Modifier
                        // فیکس v1.9: بدونِ کلیپ، دُمِ خط‌های هاشور از کادرِ خانه بیرون می‌زد و
                        // روی خانهٔ سمت چپ (سؤال یا جواب) «نیمه‌هاشورِ» اشتباه می‌کشید!
                        .clipToBounds()
                        .drawBehind {
                            // هاشورِ موربِ خانهٔ جداکنندهٔ آبی (قاعدهٔ کاربر v1.8 — جای مشکیِ توپ)
                            drawBlockHatch(if (isDark) BlockHatchDark else BlockHatchLight)
                        }
                    SharhCellType.PHOTO -> Modifier
                    else -> Modifier.drawBehind {
                        // فقط خط‌چینِ جداکنندهٔ سرنخِ دوسؤالی — فلش‌ها از v1.8 بیرونِ خانه رسم می‌شوند
                        drawCellDecor(cell, cellSize)
                    }
                }
            )
            .border(
                width = when {
                    isSelected -> 2.dp
                    isHighlighted -> 1.5.dp
                    cell.type == SharhCellType.PHOTO -> 0.dp
                    else -> 0.5.dp
                },
                color = when {
                    isSelected -> if (isPhotoCell) PhotoSelectedBorder else Color(0xFF0077B6)
                    isHighlighted -> if (isPhotoCell) PhotoHighlightedBorder else Color(0xFF0096C7)
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                }
            )
            .then(
                if (cell.type == SharhCellType.ANSWER || cell.isClueCell) {
                    Modifier.clickable(
                        interactionSource = sharedInteractionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .zIndex(if (isSelected) 1f else 0f),
        contentAlignment = Alignment.Center
    ) {
        when (cell.type) {
            SharhCellType.ANSWER -> {
                if (userChar != ' ') {
                    val charFontSize = (cellSize.value * 0.58f).coerceAtMost(25f).sp
                    Text(
                        text = userChar.toString(),
                        fontSize = charFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                            textAlign = TextAlign.Center,
                            textDirection = TextDirection.Content
                        )
                    )
                }
            }
            else -> SharhCellContent(cell, cellSize, textColor)
        }
    }
}

/** محتوای سلول: متن سرنخ */
@Composable
private fun SharhCellContent(cell: SharhCell, cellSize: Dp, textColor: Color) {
    when (cell.type) {
        SharhCellType.CLUE -> {
            val text = if (cell.hasArrowH) cell.clueH else cell.clueV
            if (text.isNotEmpty()) {
                val fs = clueFontSize(text.length, cellSize)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // قاعدهٔ کاربر v1.8: فلش بیرونِ خانه است → پدینگِ لبه‌ها حداقلی تا
                        // کلمات سؤال بهتر جا شوند
                        .padding(
                            start = cellSize * 0.05f,
                            end = cellSize * 0.04f,
                            top = cellSize * 0.03f,
                            bottom = cellSize * 0.04f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = fs,
                        lineHeight = fs * 1.25f,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PersianFontFamily,
                        textAlign = TextAlign.Center,
                        style = TextStyle(textDirection = TextDirection.Content)
                    )
                }
            }
        }

        SharhCellType.SPLIT_CLUE -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .padding(
                            start = cellSize * 0.05f,
                            end = cellSize * 0.03f,
                            top = cellSize * 0.02f,
                            bottom = cellSize * 0.01f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (cell.clueH.isNotEmpty()) {
                        val fs = clueFontSize(cell.clueH.length, cellSize) * 0.92f
                        Text(
                            text = cell.clueH,
                            color = textColor,
                            fontSize = fs,
                            lineHeight = fs * 1.22f,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PersianFontFamily,
                            textAlign = TextAlign.Center,
                            style = TextStyle(textDirection = TextDirection.Content)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(
                            start = cellSize * 0.03f,
                            end = cellSize * 0.03f,
                            top = cellSize * 0.01f,
                            bottom = cellSize * 0.04f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (cell.clueV.isNotEmpty()) {
                        val fs = clueFontSize(cell.clueV.length, cellSize) * 0.92f
                        Text(
                            text = cell.clueV,
                            color = textColor,
                            fontSize = fs,
                            lineHeight = fs * 1.22f,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PersianFontFamily,
                            textAlign = TextAlign.Center,
                            style = TextStyle(textDirection = TextDirection.Content)
                        )
                    }
                }
            }
        }

        SharhCellType.ANSWER -> {
            // حرف توسط والد (GameScreen state) رسم می‌شود
        }
        else -> {}
    }
}

/**
 * اندازهٔ فونت متن سرنخ بر اساس طول متن و اندازهٔ خانه.
 * قاعدهٔ کاربر v1.8: «فونت سوال‌ها خیلی کوچیکه — ۲ سایز بزرگتر کن» → +۲sp روی همهٔ حالت‌ها.
 */
private fun clueFontSize(len: Int, cellSize: Dp): androidx.compose.ui.unit.TextUnit {
    val factor = when {
        len <= 12 -> 0.185f
        len <= 22 -> 0.15f
        len <= 32 -> 0.128f
        len <= 45 -> 0.11f
        else -> 0.096f
    }
    // +۲sp روی مقدار عددی (TextUnit در این نسخهٔ Compose عملگر + برای TextUnit ندارد)
    return ((cellSize.value * factor) + 2f).sp
}

/** خط‌چین جداکنندهٔ دو نیمهٔ سرنخ دوسؤالی — فلش‌های جهت‌نما از v1.8 بیرونِ خانه رسم می‌شوند */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCellDecor(cell: SharhCell, cellSize: Dp) {
    if (cell.type != SharhCellType.SPLIT_CLUE) return

    val y = size.height * 0.5f
    val dash = size.width * 0.07f
    val gap = size.width * 0.045f
    var x = size.width * 0.07f
    val endX = size.width * 0.93f
    while (x < endX) {
        drawLine(
            color = ArrowColor.copy(alpha = 0.5f),
            start = Offset(x, y),
            end = Offset(minOf(x + dash, endX), y),
            strokeWidth = 1.4f
        )
        x += dash + gap
    }
}

/**
 * هاشورِ موربِ ۴۵ درجهٔ خانهٔ جداکنندهٔ آبی (قاعدهٔ کاربر v1.8).
 * فیکس v1.9: هر خط فقط در «بخشِ داخلِ کادر» رسم می‌شود — قبلاً خط‌ها از x=-height
 * شروع می‌شدند و بیرونِ خانه می‌افتادند و روی خانهٔ مجاورِ چپ هاشورِ اشتباه می‌کشیدند.
 * (کلیپ با clipToBounds هم انجام شده؛ این ریاضیِ امن، تضمینِ دوبل است.)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlockHatch(hatchColor: Color) {
    val step = size.width * 0.24f    // فاصلهٔ خط‌ها
    val stroke = size.width * 0.055f // ضخامتِ خط‌ها
    val w = size.width
    val h = size.height
    // خطِ ۴۵ درجه: نقاط (x0 + t, t) برای t ∈ [0, h] — فقط بخشِ داخل [0,w]×[0,h]
    var x0 = -h
    while (x0 < w) {
        val tStart = maxOf(0f, -x0)
        val tEnd = minOf(h, w - x0)
        if (tStart < tEnd) {
            drawLine(
                color = hatchColor,
                start = Offset(x0 + tStart, tStart),
                end = Offset(x0 + tEnd, tEnd),
                strokeWidth = stroke
            )
        }
        x0 += step
    }
}
