package com.aistudio.sharhdarmatn.data

/**
 * نرمال‌سازی حروف فارسی — منبع واحد حقیقت برای:
 * ۱) مقایسهٔ جواب کاربر با سلول‌های گرید
 * ۲) تولید حروف کیبورد مجازی تا هر حرفی که گرید لازم دارد حتماً روی کیبورد بیاید
 *    (مثلاً «آ» در گرید → کلید «ا»؛ «ئ» در گرید → کلید «ی»)
 */
fun normalizePersianChar(char: Char): Char = when (char) {
    'آ', 'أ', 'إ' -> 'ا'
    'ي', 'ى', 'ئ' -> 'ی'
    'ك' -> 'ک'
    'ة' -> 'ه'
    'ؤ' -> 'و'
    else -> char
}

/** نوع خانهٔ جدول شرح در متن */
enum class SharhCellType {
    ANSWER,     // خانهٔ حرف جواب
    BLOCK,      // خانهٔ سیاه جداکننده
    CLUE,       // خانهٔ شرح (تک‌سؤالی) — حرف ندارد
    SPLIT_CLUE, // خانهٔ دو‌سؤالی (بالا: افقی، پایین: عمودی)
    PHOTO       // ناحیهٔ عکس موضوعی جدول
}

/**
 * یک خانه از شبکهٔ جدول.
 * @param letter حرف جواب (فقط برای ANSWER)
 * @param clueH متن سرنخ افقی (واژه‌ای که راست‌به‌چپ خوانده می‌شود و از سمت چپِ این خانه شروع می‌شود)
 * @param clueV متن سرنخ عمودی (واژه‌ای که از پایینِ این خانه شروع می‌شود)
 * @param hasArrowH آیا فلش افقی (چپ‌رو) روی خانه است
 * @param hasArrowV آیا فلش عمودی (پایین‌رو) روی خانه است
 */
data class SharhCell(
    val type: SharhCellType,
    val letter: Char = ' ',
    val clueH: String = "",
    val clueV: String = "",
    val hasArrowH: Boolean = false,
    val hasArrowV: Boolean = false
) {
    val isClueCell: Boolean get() = type == SharhCellType.CLUE || type == SharhCellType.SPLIT_CLUE
    val isFillable: Boolean get() = type == SharhCellType.ANSWER
}

/**
 * یک واژه (لاین) از جدول.
 * @param direction «LEFT» یعنی افقی راست‌به‌چپ (خانه‌ها از راست به چپ پر می‌شوند)، «DOWN» عمودی بالا‌به‌پایین
 * @param cells مختصات خانه‌ها به ترتیب حروف جواب (اولین حرف تا آخرین)
 * @param isPhoto این لاین از «سؤال‌های موضوعیِ فردِ داخل عکس» است
 *                  (همهٔ سؤال‌های شماره‌دار؛ رنگِ نارنجیِ ملایم — قاعدهٔ v1.5 کاربر)
 */
data class SharhWord(
    val id: Int,
    val direction: String,
    val clueRow: Int,
    val clueCol: Int,
    val cells: List<Pair<Int, Int>>,
    val word: String,
    val clue: String,
    val isPhoto: Boolean = false
) {
    val isHorizontal: Boolean get() = direction == "LEFT"
    val length: Int get() = cells.size
}

/**
 * یک جدول کامل شرح در متن.
 * @param photoRect ناحیهٔ عکس به‌صورت {startRow, endRow, startCol, endCol} inclusive؛
 *                  اگر اندازهٔ ۴ نداشته باشد یعنی جدول عکس ندارد. عکس همیشه با
 *                  برش مرکزی (center-crop) رندر می‌شود تا نسبت آن هرگز به‌هم نریزد.
 */
data class SharhPuzzle(
    val id: String,
    val title: String,
    val rows: Int,
    val cols: Int,
    val grid: List<List<SharhCell>>,
    val words: List<SharhWord>,
    val photoResName: String,
    val photoRect: IntArray = intArrayOf()
) {
    val hasPhoto: Boolean get() = photoRect.size == 4 && photoResName.isNotEmpty()
    /** تعداد خانه‌های حرف‌دار (ANSWER) */
    val answerCellCount: Int by lazy {
        grid.sumOf { row -> row.count { it.type == SharhCellType.ANSWER } }
    }

    fun cellAt(row: Int, col: Int): SharhCell = grid[row][col]

    /** خانه‌های حرف‌دارِ «سؤال‌های موضوعیِ فردِ عکس» — برای رنگِ نارنجیِ ملایم */
    val photoWordCells: Set<Pair<Int, Int>> by lazy {
        words.filter { it.isPhoto }.flatMap { it.cells }.toSet()
    }

    /** خانه‌های سرنخِ «سؤال‌های موضوعیِ فردِ عکس» */
    val photoClueCells: Set<Pair<Int, Int>> by lazy {
        words.filter { it.isPhoto }.map { it.clueRow to it.clueCol }.toSet()
    }

    /** تمام واژه‌هایی که از خانهٔ (row,col) می‌گذرند */
    fun wordsAt(row: Int, col: Int): List<SharhWord> =
        words.filter { word -> word.cells.contains(row to col) }

    /** ایندکس تخت خانه برای آرایهٔ ورودی کاربر */
    fun flatIndex(row: Int, col: Int): Int = row * cols + col
}
