package com.aistudio.sharhdarmatn.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.sharhdarmatn.data.AppDatabase
import com.aistudio.sharhdarmatn.data.PuzzleRepository
import com.aistudio.sharhdarmatn.data.PuzzleProgressEntity
import com.aistudio.sharhdarmatn.data.SharhCellType
import com.aistudio.sharhdarmatn.data.SharhPuzzle
import com.aistudio.sharhdarmatn.data.SharhWord
import com.aistudio.sharhdarmatn.data.SharhPuzzleData
import com.aistudio.sharhdarmatn.data.normalizePersianChar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** صفحات بازی — از v2.2 صفحهٔ اول (Landing) جدا از صفحهٔ انتخاب جدول است */
enum class Screen {
    HOME,      // صفحهٔ اول (مثل شهر جدول: برند + ادامهٔ جدول + تبلیغ + ورود به بازی)
    PUZZLES,   // صفحهٔ انتخاب جدول (لیست جدول‌ها)
    GAME,
    SETTINGS,
    HELP
}

/**
 * ViewModel اصلی بازی «جدول شرح در متن».
 *
 * منطق کی‌بورد: کی‌بورد فقط وقتی روی یک خانهٔ حرف‌دار (لاین) کلیک شود ظاهر می‌شود،
 * در پایین صفحه به‌صورت overlay قرار می‌گیرد، و با اسکرول/جابه‌جایی جدول توسط کاربر محو می‌شود.
 */
class PuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PuzzleRepository(application, db.puzzleDao())

    // ───────────────────────── Navigation ─────────────────────────
    var currentScreen by mutableStateOf(Screen.HOME)
        private set
    private var previousScreen: Screen = Screen.HOME

    // ───────────────────────── Settings / Prefs ─────────────────────────
    var isSoundEnabled by mutableStateOf(repository.isSoundEnabled())

    private val sharedPrefs = application.getSharedPreferences("sharh_dm_prefs", android.content.Context.MODE_PRIVATE)

    var globalBonusCoins by mutableStateOf(0)
        private set

    var launchCount by mutableStateOf(0)
        private set

    // ───────────────────────── DB Progress ─────────────────────────
    private val _allProgress = MutableStateFlow<Map<String, PuzzleProgressEntity>>(emptyMap())
    val allProgress: StateFlow<Map<String, PuzzleProgressEntity>> = _allProgress.asStateFlow()

    // ───────────────────────── Active Game State ─────────────────────────
    var activePuzzle by mutableStateOf<SharhPuzzle?>(null)
        private set

    /** ورودی کاربر برای هر خانه (فقط خانه‌های ANSWER معنا دارند؛ بقیه فاصله) */
    var userGridInputs by mutableStateOf<List<Char>>(emptyList())
        private set

    var activeRow by mutableStateOf(-1)
        private set
    var activeCol by mutableStateOf(-1)
        private set

    /** لاین (واژه) فعال — روی آن highlights اعمال می‌شود و حروفش روی کیبورد است */
    var activeWord by mutableStateOf<SharhWord?>(null)
        private set

    private var lastDirection: String = "LEFT"

    /** کی‌بورد فقط پس از کلیک روی یک لاین ظاهر می‌شود */
    var isKeyboardVisible by mutableStateOf(false)
        private set

    var timerSeconds by mutableStateOf(0L)
        private set
    var hintDeductions by mutableStateOf(0)

    var showCompletedDialog by mutableStateOf(false)
        private set
    var showIncorrectCompletionDialog by mutableStateOf(false)

    var hintDialogTitle by mutableStateOf("")
    var hintDialogContent by mutableStateOf("")
    var showHintResultDialog by mutableStateOf(false)

    fun dismissHintResultDialog() {
        showHintResultDialog = false
    }

    var showOnboarding by mutableStateOf(false)

    var showRatingDialog by mutableStateOf(false)
        private set

    fun dismissRatingDialog() {
        showRatingDialog = false
        SoundManager.playClick()
    }

    fun rateApp(context: android.content.Context) {
        showRatingDialog = false
        SoundManager.playClick()
        val packageName = context.packageName
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("myket://comment?id=$packageName")
            setPackage("ir.mservices.market")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://myket.ir/app/$packageName")
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "یافتن مایکت مقدور نبود", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ───────── تبلیغ میان‌صفحه‌ای ورودی (ادیوری — هر ۱۰ ورود به صفحهٔ حل جدول) ─────────
    var shouldShowEntryInterstitial by mutableStateOf(false)
        private set

    fun consumeEntryInterstitialFlag() {
        shouldShowEntryInterstitial = false
    }

    fun showEntryInterstitialAd(context: android.content.Context) {
        val placementId = com.aistudio.sharhdarmatn.MainActivity.ADIVERY_INTERSTITIAL_PLACEMENT
        if (com.adivery.sdk.Adivery.isLoaded(placementId)) {
            com.aistudio.sharhdarmatn.MainActivity.isShowingFullscreenAd = true
            com.adivery.sdk.Adivery.showAd(placementId)
        } else {
            // اگر آماده نبود، برای دفعهٔ بعد آماده می‌شود
            com.adivery.sdk.Adivery.prepareInterstitialAd(context, placementId)
        }
    }

    // Score feedback
    var lastScoreForFeedback by mutableStateOf(-1)
    var scoreChangeAmount by mutableStateOf(0)
    var scoreChangeTriggerId by mutableStateOf(0)

    private fun updateScoreFeedback() {
        val current = calculateScore()
        if (lastScoreForFeedback == -1) {
            lastScoreForFeedback = current
            return
        }
        val diff = current - lastScoreForFeedback
        if (diff != 0) {
            scoreChangeAmount = diff
            scoreChangeTriggerId++
            lastScoreForFeedback = current
            if (diff > 0) SoundManager.playScoreGain() else SoundManager.playScoreLoss()
        }
    }

    private var timerJob: Job? = null

    init {
        SoundManager.setSoundEnabled(isSoundEnabled)

        val count = sharedPrefs.getInt("launch_count", 0) + 1
        sharedPrefs.edit().putInt("launch_count", count).apply()
        launchCount = count

        if (!sharedPrefs.contains("global_bonus_coins")) {
            sharedPrefs.edit().putInt("global_bonus_coins", 200).apply()
            globalBonusCoins = 200
        } else {
            globalBonusCoins = sharedPrefs.getInt("global_bonus_coins", 0)
        }

        viewModelScope.launch {
            repository.allProgress.collectLatest { progressList ->
                val progressMap = progressList.associateBy { it.id }
                _allProgress.value = progressMap
                // از v2.2: ورود خودکار به جدولِ نیمه‌کارهٔ آخر حذف شد — صفحهٔ اول با
                // دکمهٔ «ادامهٔ جدول» این کار را می‌کند (مثل شهر جدول)
            }
        }
    }

    // ───────────────────────── Navigation ─────────────────────────
    fun navigateTo(screen: Screen) {
        if (screen != Screen.HELP) previousScreen = currentScreen
        currentScreen = screen
        if (screen != Screen.GAME) {
            stopTimer()
            hideKeyboard()
        }
    }

    fun navigateBack() {
        currentScreen = previousScreen
        if (currentScreen == Screen.GAME && !isCompleted()) startTimer()
    }

    fun handleBackPress(): Boolean {
        SoundManager.playClick()
        return when (currentScreen) {
            Screen.HOME -> false // خروج پیش‌فرض سیستم
            Screen.PUZZLES -> {
                navigateTo(Screen.HOME)
                true
            }
            Screen.GAME -> {
                if (isKeyboardVisible) {
                    // مثل کیبورد واقعی: اول کیبورد بسته می‌شود، بعد از جدول خارج می‌شویم
                    hideKeyboard()
                } else {
                    navigateTo(Screen.PUZZLES)
                }
                true
            }
            Screen.SETTINGS -> {
                navigateTo(Screen.PUZZLES)
                true
            }
            Screen.HELP -> {
                navigateBack()
                true
            }
        }
    }

    // ───────────────────────── شروع جدول ─────────────────────────
    fun startPuzzle(puzzle: SharhPuzzle) {
        activePuzzle = puzzle
        sharedPrefs.edit().putString("last_active_puzzle_id", puzzle.id).apply()
        showCompletedDialog = false
        showIncorrectCompletionDialog = false
        SoundManager.setSoundEnabled(isSoundEnabled)

        val progress = _allProgress.value[puzzle.id]
        val gridSize = puzzle.rows * puzzle.cols
        if (progress != null) {
            val loadedInputs = progress.userInput.map { it }
            userGridInputs = if (loadedInputs.size == gridSize) {
                loadedInputs
            } else {
                loadedInputs.take(gridSize) + List(maxOf(0, gridSize - loadedInputs.size)) { ' ' }
            }
            timerSeconds = progress.timeSpentSeconds
            hintDeductions = 0
            if (progress.isCompleted) {
                showCompletedDialog = true
            }
        } else {
            userGridInputs = List(gridSize) { ' ' }
            timerSeconds = 0
            hintDeductions = 0
        }

        // انتخاب اولین واژه (بدون نمایش کی‌بورد — کی‌بورد فقط با کلیک کاربر می‌آید)
        selectFirstAvailableWord(puzzle)
        isKeyboardVisible = false

        if (!repository.hasShownOnboarding()) {
            showOnboarding = true
        }

        // درخواست امتیاز هر ۵ ورود (به‌جز مضرب‌های ۱۰ — تا با تبلیغ میان‌صفحه‌ای تداخل نکند)
        val entries = sharedPrefs.getInt("game_screen_entry_count", 0) + 1
        sharedPrefs.edit().putInt("game_screen_entry_count", entries).apply()
        // تبلیغ میان‌صفحه‌ای ورودی (ادیوری): هر ۱۰ ورود به صفحهٔ حل جدول یک بار
        shouldShowEntryInterstitial = entries % 10 == 0
        if (entries % 5 == 0 && entries % 10 != 0) showRatingDialog = true

        navigateTo(Screen.GAME)

        lastScoreForFeedback = calculateScore()
        scoreChangeAmount = 0
        scoreChangeTriggerId = 0

        if (!isCompleted()) startTimer()
    }

    private fun selectFirstAvailableWord(puzzle: SharhPuzzle) {
        val word = puzzle.words.minByOrNull { it.id }
        if (word != null) {
            activeWord = word
            activeRow = word.cells[0].first
            activeCol = word.cells[0].second
            lastDirection = word.direction
        } else {
            activeWord = null
            activeRow = -1
            activeCol = -1
        }
    }

    // ───────────────────────── کلیک خانه‌ها ─────────────────────────
    /**
     * کلیک روی خانهٔ حرف‌دار (ANSWER): لاین مربوطه انتخاب و کی‌بورد در پایین صفحه ظاهر می‌شود.
     * اگر خانه به دو واژه (افقی+عمودی) تعلق داشته باشد، کلیک دوباره جهت را عوض می‌کند.
     */
    fun onAnswerCellClicked(row: Int, col: Int) {
        val puzzle = activePuzzle ?: return
        val cell = puzzle.cellAt(row, col)
        if (cell.type != SharhCellType.ANSWER) return

        SoundManager.playCellSelect()

        val wordsHere = puzzle.wordsAt(row, col)
        if (wordsHere.isEmpty()) return

        val sameCell = activeRow == row && activeCol == col
        val currentWord = activeWord

        val newWord: SharhWord = when {
            // همان خانهٔ فعلی → اگر واژهٔ دیگری هم از آن می‌گذرد، بین دو واژه سوییچ کن
            sameCell && currentWord != null && wordsHere.size > 1 -> {
                wordsHere.firstOrNull { it.id != currentWord.id } ?: currentWord
            }
            // خانه در واژهٔ فعال است → همان واژه، فقط سلول انتخابی عوض می‌شود
            currentWord != null && currentWord.cells.contains(row to col) -> currentWord
            // ترجیح: واژه‌ای با جهت قبلی
            else -> wordsHere.firstOrNull { it.direction == lastDirection } ?: wordsHere.first()
        }

        activeWord = newWord
        activeRow = row
        activeCol = col
        lastDirection = newWord.direction

        // ✨ کی‌بورد فقط همین‌جا ظاهر می‌شود (کلیک روی لاین)
        isKeyboardVisible = true
    }

    /**
     * کلیک روی خانهٔ سرنخ (CLUE یا SPLIT_CLUE) — قاعدهٔ v1.9 کاربر:
     * پنجرهٔ سؤالِ وسطِ صفحه حذف شد؛ به‌جای آن «نوارِ سؤال» همیشه در پایینِ صفحه
     * (بالای کیبورد) سؤالِ لاینِ فعال را نشان می‌دهد. کلیک روی سرنخ یعنی انتخابِ لاین:
     *  - تک‌سؤالی: همان لاین انتخاب و کیبورد باز می‌شود.
     *  - دوسؤالی: کلیکِ اول سؤالِ «افقی» و کلیکِ بعدی سؤالِ «عمودی» را نشان می‌دهد (تناوب).
     */
    fun onClueCellClicked(row: Int, col: Int) {
        val puzzle = activePuzzle ?: return
        val cell = puzzle.cellAt(row, col)
        if (!cell.isClueCell) return
        SoundManager.playCellSelect()

        // واژه‌هایی که این خانه، سرنخِ آن‌هاست (دادهٔ وریفای‌شده: ۱ یا ۲ تا)
        val candidates = puzzle.words.filter { it.clueRow == row && it.clueCol == col }
        if (candidates.isEmpty()) return

        val chosen: SharhWord = if (candidates.size == 1) {
            candidates.first()
        } else {
            // دوسؤالی: اولین کلیک → افقی؛ اگر همین حالا افقیِ این خانه فعال است → عمودی (و بالعکس)
            val h = candidates.firstOrNull { it.isHorizontal }
            val v = candidates.firstOrNull { !it.isHorizontal }
            val currentId = activeWord?.id
            when {
                h != null && currentId == h.id -> v ?: h
                v != null && currentId == v.id -> h ?: v
                else -> h ?: candidates.first()
            }
        }

        activeWord = chosen
        activeRow = chosen.cells.first().first      // ابتدای لاین (خانهٔ مجاور سرنخ)
        activeCol = chosen.cells.first().second
        lastDirection = chosen.direction

        // قاعدهٔ v1.9: سرنخ مثل لاین عمل می‌کند — کیبورد همان‌جا بالای آن می‌آید
        // و نوارِ سؤال در پایین (بالای کیبورد) سؤالِ همین لاین را نشان می‌دهد
        isKeyboardVisible = true
    }

    /** شمارهٔ جدولِ فعال در فهرست (۱-بنیاد) — برای نمایشِ «جدول ۳» بدونِ لو رفتنِ نام */
    fun currentPuzzleNumber(): Int =
        activePuzzle?.let { p -> SharhPuzzleData.puzzles.indexOfFirst { it.id == p.id } + 1 } ?: 0

    /**
     * وقتی کاربر جدول را اسکرول/جابه‌جا می‌کند، کی‌بورد محو می‌شود.
     * از GameScreen (ژست درگ/پینچ) صدا زده می‌شود.
     */
    fun hideKeyboard() {
        if (isKeyboardVisible) isKeyboardVisible = false
    }

    // ───────────────────────── ورودی کی‌بورد ─────────────────────────
    fun normalizeCharForComparison(char: Char): Char = normalizePersianChar(char)

    fun onKeyPressed(char: Char) {
        val puzzle = activePuzzle ?: return
        val word = activeWord ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        SoundManager.playType()

        val normalized = normalizeCharForComparison(char)
        val idx = puzzle.flatIndex(activeRow, activeCol)
        val newList = userGridInputs.toMutableList()
        newList[idx] = normalized
        userGridInputs = newList
        saveCurrentProgress()

        if (checkAndHandleCompletion()) return

        // رفتن به خانهٔ بعدیِ همان لاین
        moveToNextCellInWord(word)
    }

    fun onBackspacePressed() {
        val puzzle = activePuzzle ?: return
        val word = activeWord ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        SoundManager.playDelete()

        val idx = puzzle.flatIndex(activeRow, activeCol)
        val newList = userGridInputs.toMutableList()

        if (userGridInputs[idx] != ' ') {
            newList[idx] = ' '
            userGridInputs = newList
        } else {
            // برگرد به خانهٔ قبلی لاین و آن را پاک کن
            val posInWord = word.cells.indexOf(activeRow to activeCol)
            if (posInWord > 0) {
                val (pr, pc) = word.cells[posInWord - 1]
                activeRow = pr
                activeCol = pc
                val pidx = puzzle.flatIndex(pr, pc)
                newList[pidx] = ' '
                userGridInputs = newList
            }
        }
        saveCurrentProgress()
    }

    private fun moveToNextCellInWord(word: SharhWord) {
        val posInWord = word.cells.indexOf(activeRow to activeCol)
        if (posInWord >= 0 && posInWord < word.cells.size - 1) {
            val (nr, nc) = word.cells[posInWord + 1]
            activeRow = nr
            activeCol = nc
        }
    }

    // ───────────────────────── وضعیت واژه‌ها ─────────────────────────
    fun isCellCorrect(row: Int, col: Int): Boolean {
        val puzzle = activePuzzle ?: return false
        val cell = puzzle.cellAt(row, col)
        if (cell.type != SharhCellType.ANSWER) return false
        val idx = puzzle.flatIndex(row, col)
        val user = userGridInputs.getOrNull(idx) ?: ' '
        return user != ' ' && normalizeCharForComparison(user) == normalizeCharForComparison(cell.letter)
    }

    fun isWordSolved(word: SharhWord): Boolean {
        val puzzle = activePuzzle ?: return false
        return word.cells.all { (r, c) -> isCellCorrect(r, c) }
    }

    fun countSolvedWords(): Int {
        val puzzle = activePuzzle ?: return 0
        return puzzle.words.count { isWordSolved(it) }
    }

    private fun isGridFullyFilled(): Boolean {
        val puzzle = activePuzzle ?: return false
        for (r in 0 until puzzle.rows) {
            for (c in 0 until puzzle.cols) {
                if (puzzle.cellAt(r, c).type == SharhCellType.ANSWER && userGridInputs[puzzle.flatIndex(r, c)] == ' ') {
                    return false
                }
            }
        }
        return true
    }

    private fun isCompleted(): Boolean {
        val puzzle = activePuzzle ?: return false
        for (r in 0 until puzzle.rows) {
            for (c in 0 until puzzle.cols) {
                val cell = puzzle.cellAt(r, c)
                if (cell.type == SharhCellType.ANSWER) {
                    val u = userGridInputs[puzzle.flatIndex(r, c)]
                    if (u == ' ' || normalizeCharForComparison(u) != normalizeCharForComparison(cell.letter)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun checkAndHandleCompletion(): Boolean {
        if (isCompleted()) {
            stopTimer()
            SoundManager.playSuccess()

            val puzzle = activePuzzle
            if (puzzle != null) {
                val wasCompletedBefore = _allProgress.value[puzzle.id]?.isCompleted == true
                if (!wasCompletedBefore) awardGlobalBonusCoins()
            }

            showCompletedDialog = true
            isKeyboardVisible = false
            saveCurrentProgress(completed = true)
            return true
        } else if (isGridFullyFilled()) {
            showIncorrectCompletionDialog = true
        }
        return false
    }

    fun dismissOnboarding() {
        showOnboarding = false
        repository.setShownOnboarding(true)
        SoundManager.playClick()
    }

    fun closeCompletedDialog() {
        showCompletedDialog = false
        SoundManager.playClick()
    }

    fun closeIncorrectCompletionDialog() {
        showIncorrectCompletionDialog = false
        SoundManager.playClick()
    }

    // ───────────────────────── راهنماها (سکه‌ای) ─────────────────────────
    fun useHintRevealLetter() {
        val puzzle = activePuzzle ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        val idx = puzzle.flatIndex(activeRow, activeCol)
        val cell = puzzle.cellAt(activeRow, activeCol)
        if (cell.type != SharhCellType.ANSWER) return

        val isAlreadyCorrect = normalizeCharForComparison(userGridInputs[idx]) == normalizeCharForComparison(cell.letter)
        if (isAlreadyCorrect) {
            hintDialogTitle = "حرف خانهٔ انتخاب‌شده"
            hintDialogContent = "حرف این خانه «${cell.letter}» است (این خانه قبلاً درست پر شده است)."
            showHintResultDialog = true
            return
        }

        val currentCoins = getTotalCoins()
        if (currentCoins < 30) {
            hintDialogTitle = "سکه ناکافی"
            hintDialogContent = "برای آشکار شدن این حرف به ۳۰ سکه نیاز دارید.\nسکه‌های فعلی شما: $currentCoins\n\nبا تکمیل جدول‌ها سکه دریافت کنید."
            showHintResultDialog = true
            return
        }

        val newList = userGridInputs.toMutableList()
        newList[idx] = cell.letter
        userGridInputs = newList
        hintDeductions += 30
        saveCurrentProgress()
        checkAndHandleCompletion()

        hintDialogTitle = "حرف خانه آشکار شد"
        hintDialogContent = "حرف این خانه «${cell.letter}» است."
        showHintResultDialog = true
    }

    fun useHintRevealWord() {
        val puzzle = activePuzzle ?: return
        val word = activeWord ?: return
        if (isCompleted()) return

        val anyNeedsReveal = word.cells.any { (r, c) -> !isCellCorrect(r, c) }
        if (!anyNeedsReveal) {
            hintDialogTitle = "کلمهٔ انتخاب‌شده"
            hintDialogContent = "کلمهٔ این لاین «${word.word}» است (این کلمه قبلاً درست حل شده است)."
            showHintResultDialog = true
            return
        }

        val currentCoins = getTotalCoins()
        if (currentCoins < 100) {
            hintDialogTitle = "سکه ناکافی"
            hintDialogContent = "برای آشکار شدن این کلمه به ۱۰۰ سکه نیاز دارید.\nسکه‌های فعلی شما: $currentCoins\n\nبا تکمیل جدول‌ها سکه دریافت کنید."
            showHintResultDialog = true
            return
        }

        val newList = userGridInputs.toMutableList()
        word.cells.forEach { (r, c) ->
            newList[puzzle.flatIndex(r, c)] = puzzle.cellAt(r, c).letter
        }
        userGridInputs = newList
        hintDeductions += 100
        saveCurrentProgress()
        checkAndHandleCompletion()

        hintDialogTitle = "کلمه آشکار شد"
        hintDialogContent = "کلمهٔ کامل این لاین:\n«${word.word}»"
        showHintResultDialog = true
    }

    fun useHintClearWrong() {
        val puzzle = activePuzzle ?: return
        if (isCompleted()) return

        val currentCoins = getTotalCoins()
        if (currentCoins < 30) {
            hintDialogTitle = "سکه ناکافی"
            hintDialogContent = "برای پاک کردن حروف اشتباه به ۳۰ سکه نیاز دارید.\nسکه‌های فعلی شما: $currentCoins\n\nبا تکمیل جدول‌ها سکه دریافت کنید."
            showHintResultDialog = true
            return
        }

        var clearedAny = false
        val newList = userGridInputs.toMutableList()
        for (r in 0 until puzzle.rows) {
            for (c in 0 until puzzle.cols) {
                val idx = puzzle.flatIndex(r, c)
                val cell = puzzle.cellAt(r, c)
                if (cell.type == SharhCellType.ANSWER && userGridInputs[idx] != ' ' && !isCellCorrect(r, c)) {
                    newList[idx] = ' '
                    clearedAny = true
                }
            }
        }
        if (clearedAny) {
            userGridInputs = newList
            hintDeductions += 30
            saveCurrentProgress()
            hintDialogTitle = "پاک‌سازی انجام شد"
            hintDialogContent = "تمام حروف اشتباه شما از جدول پاک شدند."
            showHintResultDialog = true
        } else {
            hintDialogTitle = "پاک‌سازی جدول"
            hintDialogContent = "هیچ حرف اشتباهی در جدول وجود ندارد."
            showHintResultDialog = true
        }
    }

    // ───────────────────────── امتیاز و ذخیره ─────────────────────────
    fun calculateScore(): Int {
        val solvedWords = countSolvedWords() * 5
        val completionScore = if (isCompleted()) 200 else 0
        return solvedWords + completionScore - hintDeductions
    }

    fun saveCurrentProgress(completed: Boolean = false) {
        val puzzle = activePuzzle ?: return
        val isComp = completed || isCompleted()
        if (isComp) {
            sharedPrefs.edit().remove("last_active_puzzle_id").apply()
        }
        updateScoreFeedback()
        val gridStr = userGridInputs.joinToString("")
        viewModelScope.launch {
            repository.saveProgress(
                puzzleId = puzzle.id,
                userInput = gridStr,
                isCompleted = isComp,
                timeSpentSeconds = timerSeconds,
                score = calculateScore(),
                hintsUsed = hintDeductions
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                timerSeconds++
                if (timerSeconds % 15 == 0L) saveCurrentProgress()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ───────────────────────── تنظیمات ─────────────────────────
    fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        repository.setSoundEnabled(isSoundEnabled)
        SoundManager.setSoundEnabled(isSoundEnabled)
        if (isSoundEnabled) SoundManager.playClick()
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.clearAllProgress()
            val editor = sharedPrefs.edit()
            editor.remove("last_active_puzzle_id")
            editor.putInt("global_bonus_coins", 200)
            editor.apply()
            globalBonusCoins = 200
            activePuzzle = null
            userGridInputs = emptyList()
            timerSeconds = 0
            hintDeductions = 0
            showCompletedDialog = false
            isKeyboardVisible = false
            navigateTo(Screen.PUZZLES)
        }
    }

    // ───────────────────────── سکه‌ها ─────────────────────────
    fun awardGlobalBonusCoins() {
        globalBonusCoins += 100
        sharedPrefs.edit().putInt("global_bonus_coins", globalBonusCoins).apply()
    }

    fun getTotalCoins(): Int {
        val savedSum = _allProgress.value.values.filter { it.id != activePuzzle?.id }.sumOf { it.score }
        val activeScore = if (activePuzzle != null) calculateScore() else 0
        return savedSum + activeScore + globalBonusCoins
    }

    fun getTotalCoinsEarned(): Int {
        val savedSum = _allProgress.value.values.filter { it.id != activePuzzle?.id }.sumOf { it.score + it.hintsUsed }
        val activeSum = if (activePuzzle != null) calculateScore() + hintDeductions else 0
        return savedSum + activeSum + globalBonusCoins
    }

    // ───────────────────────── ادامهٔ بازی نیمه‌تمام ─────────────────────────
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
