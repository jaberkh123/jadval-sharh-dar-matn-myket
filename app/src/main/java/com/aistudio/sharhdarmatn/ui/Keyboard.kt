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
 * کیبورد فارسی روی‌صفحه — دقیقاً با همان طراحی و رفتار بازی شهر جدول:
 * همیشه ۱۵ کلید تولید می‌شود که «حتماً تمام حروفِ لاین (واژه) فعال» را دارد؛
 * بقیهٔ کلیدها حروف حواس‌پرت‌کن تصادفی (ولی قطعی و ثابت برای هر واژه) هستند.
 *
 * کیبورد فقط وقتی روی یک لاین کلیک شود نمایش داده می‌شود (بالا‌تنهٔ آن را
 * GameScreen به‌صورت overlay در پایین صفحه نشان می‌دهد).
 */
@Composable
fun PersianOnScreenKeyboard(
    word: SharhWord? = null,
    onCharTyped: (Char) -> Unit,
    onBackspace: () -> Unit = {}
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

    // تولید دقیقاً ۱۵ حرف که همهٔ حروف جواب فعلی را دارند
    val keyboardLetters = remember(word?.id, word?.word) {
        val list = mutableListOf<Char>()
        list.addAll(answerChars)

        val seed = (word?.word?.hashCode() ?: 42) + (word?.id ?: 0)
        val random = kotlin.random.Random(seed.toLong())

        val distractors = allPersianChars.filter { it !in list }.shuffled(random)
        for (char in distractors) {
            if (list.size >= 15) break
            list.add(char)
        }

        while (list.size < 15) {
            list.add(allPersianChars.random(random))
        }

        list.take(15).shuffled(random)
    }

    val row1 = keyboardLetters.take(8)
    val row2Letters = keyboardLetters.drop(8).take(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ردیف ۱ (۸ کلید)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (char in row1) {
                KeyboardKey(char = char, onClick = { onCharTyped(char) }, modifier = Modifier.weight(1f))
            }
        }

        // ردیف ۲ (۷ حرف + ۱ کلید حذف = ۸ کلید)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (char in row2Letters) {
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
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
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
            fontSize = 18.sp,
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
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
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
            modifier = Modifier.size(20.dp)
        )
    }
}
