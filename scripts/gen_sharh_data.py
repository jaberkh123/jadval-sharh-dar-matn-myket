#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""تبدیل خروجیِ مخزن «شرح در متن» به دادهٔ Kotlin بازی.

ورودی  : {SHARH_REPO}/outputs/{pid}-22x14/sharh.json  +  masks/{pid}-22x14.json
                    + assets/{pid}-photo.jpg (اختیاری)
خروجی  : app/src/main/java/com/aistudio/sharhdarmatn/data/SharhPuzzleData.kt
         app/src/main/res/drawable-nodpi/puzzle_{pid}.jpg

ترتیبِ جدول‌ها = جدیدترین به قدیمی‌ترین (برعکسِ ترتیبِ ثبت در PERSONALITIES).
جدول‌های بدونِ خروجی (ساخت ناموفق مثل messi/biruni) رد می‌شوند.
عکس: اگر assets/{pid}-photo.jpg موجود باشد → drawable + photoResName؛
     وگرنه قابِ خالی (photoResName="") — سلول‌های PHOTO از ماسک همیشه
     روی گرید چسبانده می‌شوند تا ناحیهٔ عکس رزرو بماند.
"""
from __future__ import annotations

import json
import os
import re
import shutil
import sys

SHARH_REPO = "/home/z/my-project/gh-repos/sharh-dar-matn"
GAME_REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_KT = os.path.join(
    GAME_REPO,
    "app/src/main/java/com/aistudio/sharhdarmatn/data/SharhPuzzleData.kt",
)
OUT_RES = os.path.join(GAME_REPO, "app/src/main/res/drawable-nodpi")

sys.path.insert(0, os.path.join(SHARH_REPO, "src"))
from personality import PERSONALITIES  # noqa: E402

# جدول‌هایی که خروجی‌شان هنوز در مخزنِ جدول‌ساز کامیت نشده — تا تصمیمِ کاربر وارد بازی نمی‌شوند
UNPUBLISHED = {"messi", "biruni"}


# نرمال‌سازیِ حروف فارسی — همان نگاشتِ SharhModels.kt اپ



# سؤال‌های موضوعی در sharh.json پیشوند شماره دارند (مثل «۵- ...»)
NUM_PREFIX = re.compile(r"^\s*[۰-۹0-9]+\s*[-–]\s*")


def esc(s: str) -> str:
    """اسکیپِ رشتهٔ Kotlin (نقل‌قول، بک‌اسلش، $)."""
    return (
        s.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("$", "\\$")
        .replace("\n", "\\n")
    )


def cell_to_kotlin(cell: dict) -> str:
    t = cell["type"]
    if t == "BLOCK":
        return "c(BLOCK)"
    if t == "PHOTO":
        return "c(PHOTO)"
    if t == "ANSWER":
        return "c(ANSWER, '%s')" % esc(cell["letter"])
    clues = cell.get("clues", {})
    arrows = cell.get("arrows", [])
    ch = esc(clues["LEFT"]) if "LEFT" in clues else ""
    cv = esc(clues["DOWN"]) if "DOWN" in clues else ""
    ah = "true" if "LEFT" in arrows else "false"
    av = "true" if "DOWN" in arrows else "false"
    if t == "SPLIT_CLUE":
        return "c(SPLIT_CLUE, clueH = \"%s\", clueV = \"%s\", ah = %s, av = %s)" % (
            ch, cv, ah, av)
    # CLUE تک‌سؤالی
    if "LEFT" in clues:
        return "c(CLUE, clueH = \"%s\", ah = %s)" % (ch, ah)
    return "c(CLUE, clueV = \"%s\", av = %s)" % (cv, av)


def row_to_kotlin(cells: list) -> str:
    return "            listOf(%s)," % ", ".join(cell_to_kotlin(c) for c in cells)


def word_to_kotlin(w: dict) -> str:
    cr, cc = w["clue_cell"]
    cells = ", ".join("%d to %d" % (r, c) for r, c in w["cells"])
    ip = "true" if w.get("is_photo") else "false"
    return (
        "            SharhWord(%d, \"%s\", %d, %d, listOf(%s), \"%s\", \"%s\", %s),"
        % (w["id"], w["direction"], cr, cc, cells, esc(w["word"]), esc(w["clue"]), ip)
    )


def main() -> None:
    puzzles = []          # (pid, name, grid_lines, words_lines, rows, cols, photo_name, photo_rect)
    skipped = []
    with_photos = []
    photo_marked = []     # pid → تعداد سؤال‌های موضوعیِ فردِ عکس

    for p in reversed(PERSONALITIES):          # جدیدترین اول
        pid = p.pid
        outdir = os.path.join(SHARH_REPO, "outputs", "%s-22x14" % pid)
        json_path = os.path.join(outdir, "sharh.json")
        mask_path = os.path.join(SHARH_REPO, "masks", "%s-22x14.json" % pid)
        if not (os.path.isfile(json_path) and os.path.isfile(mask_path)):
            skipped.append(pid)
            continue
        if pid in UNPUBLISHED:
            skipped.append(pid + "(unpublished)")
            continue
        data = json.load(open(json_path, encoding="utf-8"))
        mask = json.load(open(mask_path, encoding="utf-8"))
        grid = data["grid"]

        # ناحیهٔ عکس از ماسک → سلول‌های PHOTO روی گرید
        r0, r1 = mask["image_block"]["rows"]
        c0, c1 = mask["image_block"]["cols"]
        for r in range(r0, r1 + 1):
            for c in range(c0, c1 + 1):
                grid[r][c] = {"type": "PHOTO"}

        # عکس واقعی؟
        photo_src = os.path.join(SHARH_REPO, "assets", "%s-photo.jpg" % pid)
        if os.path.isfile(photo_src):
            photo_name = "puzzle_%s" % pid
            with_photos.append(pid)
        else:
            photo_name = ""

        # علامت‌گذاری «سؤال‌های موضوعیِ فردِ عکس»: «تمامیِ» واژه‌های شماره‌دار
        # (قاعدهٔ v1.5 کاربر — قبلاً در v1.4 فقط تکه‌های نام نارنجی می‌شد و کاربر گفت
        # «الان فقط یکی یا دو تا نارنجی میشن»؛ همهٔ سؤال‌های شماره‌دار دربارهٔ خودِ فردند)
        n_marked = 0
        for w in data["words"]:
            if NUM_PREFIX.match(w["clue"]):
                w["is_photo"] = True
                n_marked += 1

        grid_lines = [row_to_kotlin(row) for row in grid]
        words_lines = [word_to_kotlin(w) for w in data["words"]]
        photo_marked.append((pid, n_marked))
        puzzles.append((pid, p.name, grid_lines, words_lines,
                        data["rows"], data["cols"], photo_name,
                        (r0, r1, c0, c1)))

    # ── کپی عکس‌ها ──
    os.makedirs(OUT_RES, exist_ok=True)
    copied = []
    for pid in with_photos:
        src = os.path.join(SHARH_REPO, "assets", "%s-photo.jpg" % pid)
        dst = os.path.join(OUT_RES, "puzzle_%s.jpg" % pid)
        shutil.copyfile(src, dst)
        copied.append(pid)

    # ── تولید Kotlin ──
    parts = []
    parts.append("package com.aistudio.sharhdarmatn.data\n")
    parts.append("import com.aistudio.sharhdarmatn.data.SharhCellType.ANSWER")
    parts.append("import com.aistudio.sharhdarmatn.data.SharhCellType.BLOCK")
    parts.append("import com.aistudio.sharhdarmatn.data.SharhCellType.CLUE")
    parts.append("import com.aistudio.sharhdarmatn.data.SharhCellType.PHOTO")
    parts.append("import com.aistudio.sharhdarmatn.data.SharhCellType.SPLIT_CLUE\n")
    parts.append("""/**
 * دادهٔ جدول‌های بازی — تولیدشده به‌صورت خودکار از مخزن «شرح در متن».
 * این فایل دستی ویرایش نشود؛ برای افزودن جدول جدید:
 *   python3 scripts/gen_sharh_data.py
 * (منبع: %s — %d جدول، %d با عکس)
 */""" % (SHARH_REPO, len(puzzles), len(with_photos)))
    parts.append("""
private fun c(type: SharhCellType, letter: Char = ' ', clueH: String = "", clueV: String = "", ah: Boolean = false, av: Boolean = false) =
    SharhCell(type = type, letter = letter, clueH = clueH, clueV = clueV, hasArrowH = ah, hasArrowV = av)

object SharhPuzzleData {

    val puzzles: List<SharhPuzzle> by lazy {
        listOf(%s)
    }

    fun getPuzzleById(id: String): SharhPuzzle? = puzzles.find { it.id == id }
""" % ", ".join("%sPuzzle()" % pid for pid, *_ in puzzles))

    for pid, name, grid_lines, words_lines, rows, cols, photo_name, (r0, r1, c0, c1) in puzzles:
        nwords = len(words_lines)
        parts.append("""    /**
     * جدول «%s» — 22 سطر × 14 ستون، %d واژه
     * منبع: مخزن جدول‌ساز «شرح در متن» (outputs/%s-22x14)
     */
    private fun %sPuzzle(): SharhPuzzle {
        val grid = listOf(
%s
        )
        val words = listOf(
%s
        )
        return SharhPuzzle(
            id = "%s",
            title = "%s",
            rows = %d,
            cols = %d,
            grid = grid,
            words = words,
            photoResName = "%s",
            photoRect = intArrayOf(%d, %d, %d, %d)
        )
    }
""" % (name, nwords, pid, pid,
            "\n".join(grid_lines),
            "\n".join(words_lines),
            pid, esc(name), rows, cols, photo_name, r0, r1, c0, c1))
    parts.append("}\n")

    with open(OUT_KT, "w", encoding="utf-8") as f:
        f.write("\n".join(parts))

    print("جدول‌های تولیدشده: %d" % len(puzzles))
    print("با عکس: %d → %s" % (len(with_photos), " ".join(with_photos)))
    nophoto = [pid for pid, *_ in puzzles if pid not in with_photos]
    print("قاب خالی (بدون عکس): %d → %s" % (len(nophoto), " ".join(nophoto)))
    marked_map = dict(photo_marked)
    with_mark = [pid for pid, n in photo_marked if n > 0]
    print("سؤال‌های موضوعیِ فردِ عکس (نارنجی): %d جدول → %s" % (len(with_mark), " ".join("%s=%d" % (pid, marked_map[pid]) for pid in with_mark)))
    no_mark = [pid for pid, n in photo_marked if n == 0]
    print("بدونِ سؤالِ شماره‌دار (نارنجی ندارند): %s" % (" ".join(no_mark) if no_mark else "—"))
    print("ردشده (خروجی ندارد): %s" % (" ".join(skipped) if skipped else "—"))

    # ── اعتبارسنجیِ سبک ──
    for pid, name, gl, wl, rows, cols, pn, rect in puzzles:
        assert len(gl) == rows, pid
        assert len(wl) > 0, pid
    print("اعتبارسنجی ساختاری: ✓")


if __name__ == "__main__":
    main()
