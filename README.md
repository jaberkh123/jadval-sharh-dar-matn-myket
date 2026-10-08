# شرح در متن مشاهیر — نسخهٔ مایکت

نسخهٔ مایکتِ بازی «شرح در متن مشاهیر» (Kotlin + Jetpack Compose).

همان اپِ نسخهٔ بازار ([jadval-sharh-dar-matn](https://github.com/jaberkh123/jadval-sharh-dar-matn)) با دو تفاوت:

## تبلیغات (ادیوری — پلیس‌منت‌های مایکت)
| نوع | شناسه |
|---|---|
| برنامهٔ ادیوری (مشترک با بازار) | `99696e06-1114-4c89-a044-51c6d6d33559` |
| همسان (Native) | `b8cc6afd-4ae6-4d7e-90af-b1863f311e53` |
| بازگشت به برنامه (App Open) | `c0489ee1-a7ad-4db5-94a7-81e379f960d4` |
| میان‌صفحه‌ای (Interstitial) | `d9671ced-32d5-44c1-8c2a-60b9c1e71fd4` |

- همسان: پایین صفحهٔ حل جدول، یک بار لود در هر ورود؛ با کیبورد فقط پنهان می‌شود
- میان‌صفحه‌ای: هر ۱۰ ورود به صفحهٔ حل جدول
- بازگشت به برنامه: یک بار در میان
- پلیس‌منت همسان داخل `app/src/main/res/layout/native_ad_container.xml`

## نظر دادن
دکمهٔ نظرات به **مایکت** می‌رود: `myket://comment?id=` با پکیج `ir.mservices.market` (فال‌بک: `https://myket.ir/app/`)

## امضا
همان کی‌استور: `sharh-dm-release.jks` (alias: `sharhdm`)
