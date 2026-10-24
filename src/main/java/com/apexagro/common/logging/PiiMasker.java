package com.apexagro.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BE3-05: маскирование персональных данных в логах.
 * <ul>
 *   <li>телефон КР ({@code +996 700 123 456}, {@code 996700123456}, {@code 0700123456}) →
 *       {@code +996*******56} — видны только две последние цифры;</li>
 *   <li>ПИН (ИНН физлица, 14 цифр, начинается с 1 или 2) → {@code [PIN]}.</li>
 * </ul>
 * Применяется ко всем строкам JSON-лога (stage/prod) и к сообщениям консольного лога (dev).
 * Для логов в коде, где номер нужен явно, используйте {@link #maskPhone(String)}.
 */
public final class PiiMasker {

    private static final Pattern PIN = Pattern.compile("(?<!\\d)[12]\\d{13}(?!\\d)");

    /** +996 / 996 / 0 + 9 цифр, допускаются пробелы и дефисы между группами. */
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\d+])(?:\\+?996|0)[\\s-]?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{1}[\\s-]?(\\d)[\\s-]?(\\d)(?!\\d)");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.length() < 10 || !containsDigitRun(text)) {
            return text;
        }
        String masked = PIN.matcher(text).replaceAll("[PIN]");
        Matcher m = PHONE.matcher(masked);
        StringBuilder out = new StringBuilder(masked.length());
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement("+996*******" + m.group(1) + m.group(2)));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Маска номера для явного вывода: {@code +996700123456} → {@code +996*******56}. */
    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.length() < 2 ? "***" : "+996*******" + digits.substring(digits.length() - 2);
    }

    private static boolean containsDigitRun(String text) {
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                if (++run >= 3) {
                    return true;
                }
            } else {
                run = 0;
            }
        }
        return false;
    }
}
