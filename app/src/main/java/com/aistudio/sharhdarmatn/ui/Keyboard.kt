package com.aistudio.sharhdarmatn.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.sharhdarmatn.data.SharhWord
import com.aistudio.sharhdarmatn.data.normalizePersianChar
import com.aistudio.sharhdarmatn.ui.theme.LocalDarkTheme
import kotlinx.coroutines.delay

/**
 * کیبورد فارسی روی‌صفحه (بازطراحیِ v2.4 — قاعدهٔ کاربر):
 * به‌جای ۱۵ حرف، ۱۱ حرف در «یک ردیفِ واحد» + کلید حذف — با فونتِ بزرگ‌تر (۲۲sp).
 * حداکثر حروفِ یکتای جواب‌های بازی ۷ است، پس ۱۱ همیشه تمام حروفِ لاین را پوشش می‌دهد
 * (اگر روزی جوابی حروفِ یکتای بیشتری داشت، خودکار تعداد بیشتر می‌شود).
 * کلیدها ارتفاعِ پنلِ ثابتِ کیبورد (۱/۱۰ صفحه) را پر می‌کنند.
 * کیبورد فقط وقتی روی یک لاین کلیک شود نمایش داده می‌شود (پنلِ آن را GameScreen
 * به‌صورت overlay روی تبلیغِ همسان نشان می‌دهد).
 */
@Composable
fun PersianOnScreenKeyboard(
    word: SharhWord? = null,
    onCharTyped: (Char) -> Unit,
    onBackspace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allPersianChars = remember {
        listOf(
            'ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'ژ',
            'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ک', 'گ', 'ل', 'م', 'ن', 'و', 'ه', 'ی'
        )
    }

    // حروف خاص (همزه/اعراب/فاصله) که معادل نرمال ندارند؛ فقط اگر داخل جواب باشند کلید می‌گیرند.
    val specialAnswerChars = remember { setOf('ء', 'ّ', 'ً', ' ') }

    // استخراج حروف یکتای جواب لاین فعال با همان نرمال‌سازیِ مقایسهٔ جواب، تا تضمین شود
    // هر حرفی که خانه‌های این لاین لازم دارند حتماً روی کیبورد هست.
    // (مثلاً جواب «آیفون» → کلید «ا» که با نرمال‌سازی «آ» گرید را می‌پوشاند)
    val answerChars = remember(word?.id, word?.word) {
        word?.word
            ?.map { char -> normalizePersianChar(char) }
            ?.filter { char -> char in allPersianChars || char in specialAnswerChars }
            ?.toSet() ?: emptySet()
    }

    // تولید حروفِ کیبورد (v2.4): هدف ۱۱ حرف — حداکثر حروفِ یکتای جواب‌ها ۷ است
    val targetCount = maxOf(11, answerChars.size)
    val keyboardLetters = remember(word?.id, word?.word) {
        val list = mutableListOf<Char>()
        list.addAll(answerChars)

        val seed = (word?.word?.hashCode() ?: 42) + (word?.id ?: 0)
        val random = kotlin.random.Random(seed.toLong())

        val distractors = allPersianChars.filter { it !in list }.shuffled(random)
        for (char in distractors) {
            if (list.size >= targetCount) break
            list.add(char)
        }

        while (list.size < targetCount) {
            list.add(allPersianChars.random(random))
        }

        list.take(targetCount).shuffled(random)
    }

    // v2.4: یک ردیفِ واحد — ۱۱ کلیدِ حرف + کلیدِ حذف (سمتِ چپ)؛ فونتِ بزرگ‌تر
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (char in keyboardLetters) {
            KeyboardKey(char = char, onClick = { onCharTyped(char) }, modifier = Modifier.weight(1f))
        }
        KeyboardActionKey(
            icon = Icons.Default.Close,
            contentDescription = "حذف حرف",
            onClick = onBackspace,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun KeyboardKey(char: Char, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "KeyScale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(65)
            isPressed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (char == ' ') "␣" else char.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun KeyboardActionKey(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ActionKeyScale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(65)
            isPressed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
    }
}
