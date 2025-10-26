package com.technicalchallenge.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SettlementInstructionValidator {

    private static final Pattern UNESCAPED_QUOTE = Pattern.compile("(?<!\\\\)[\"']");
    // allow letters, numbers, spaces and common punctuation; allow escaped quotes
    // (\\" or \\\')
    private static final String ALLOWED_PATTERN = "^(?:(?:\\\\['\"])|[\\p{L}\\p{N} ,.:/()\\-\\n\\r])+$";

    public void validate(String text, TradeValidationResult result) {

        /*
         * Settlement instructions are optional.
         * If fieldValue (settlement instruction text) is blank or null,
         * it is perfectly fine to skip further checks.
         */
        // Presence Check. Validate fieldName (required)

        if (result == null)
            throw new IllegalArgumentException("TradeValidationResult required");
        if (text == null || text.trim().isEmpty())
            return;// Field is optional no value provided is acceptable

        // normalise input once so subsequent checks use the trimmed value
        text = text.trim();

        // Business rule: 10-500 chars. Length Check
        if (text.length() < 10 || text.length() > 500) {
            result.setError("Settlement instructions must be between 10 and 500 characters.");
            return;
        }
        // Content Validation protect against SQL injection
        // attempts

        // Semicolons explicitly forbidden by business rule
        if (text.contains(";")) {
            result.setError("Semicolons are not allowed in settlement instructions.");
            return;
        }

        // Detect unescaped single or double quotes
        Matcher m = UNESCAPED_QUOTE.matcher(text);
        if (m.find()) {
            result.setError(
                    "Unescaped quote found. Escape quotes with a backslash (\\\" for double quotes). Example: Settle note: client said \\\"urgent\\\".");
            return;
        }

        // Structured format check ensure only safe structured text
        // Should support structured multi-line text (label:value style).

        // Final allowed-character check (this also permits escaped quotes \" or \\')
        if (!Pattern.matches(ALLOWED_PATTERN, text)) {
            result.setError(
                    "Settlement instructions contain unsupported characters. Escape quotes with \\\" (e.g. Settle note: client said \\\"urgent\\\").");
            return;
        }
    }
}
