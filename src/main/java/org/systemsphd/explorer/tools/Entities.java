package org.systemsphd.explorer.tools;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal HTML entity handling for extracting text from fetched pages. */
final class Entities {
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]*);");
    private static final Map<String, String> NAMED = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"), Map.entry("quot", "\""),
            Map.entry("apos", "'"), Map.entry("nbsp", " "), Map.entry("ndash", "–"),
            Map.entry("mdash", "—"), Map.entry("lsquo", "‘"), Map.entry("rsquo", "’"),
            Map.entry("ldquo", "“"), Map.entry("rdquo", "”"), Map.entry("hellip", "…"),
            Map.entry("times", "×"), Map.entry("deg", "°"), Map.entry("copy", "©"),
            Map.entry("reg", "®"), Map.entry("micro", "µ"), Map.entry("middot", "·"),
            Map.entry("le", "≤"), Map.entry("ge", "≥"), Map.entry("ne", "≠"),
            Map.entry("alpha", "α"), Map.entry("beta", "β"), Map.entry("gamma", "γ"),
            Map.entry("delta", "δ"), Map.entry("epsilon", "ε"), Map.entry("lambda", "λ"),
            Map.entry("mu", "μ"), Map.entry("pi", "π"), Map.entry("sigma", "σ"),
            Map.entry("tau", "τ"), Map.entry("theta", "θ"), Map.entry("omega", "ω")
    );

    private Entities() {
    }

    static String unescape(String text) {
        Matcher matcher = ENTITY.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String entity = matcher.group(1);
            String replacement;
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                replacement = codePoint(Integer.parseInt(entity.substring(2), 16), matcher.group());
            } else if (entity.startsWith("#")) {
                replacement = codePoint(Integer.parseInt(entity.substring(1)), matcher.group());
            } else {
                replacement = NAMED.getOrDefault(entity, matcher.group());
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String codePoint(int value, String original) {
        return Character.isValidCodePoint(value) ? new String(Character.toChars(value)) : original;
    }

    /** Escapes the characters that would otherwise be read as markup. */
    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
