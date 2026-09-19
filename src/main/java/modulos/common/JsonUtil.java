package modulos.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    public static List<Map<String, String>> parseArray(String json) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (json == null) {
            return rows;
        }
        String text = json.trim();
        if (!text.startsWith("[") || !text.endsWith("]")) {
            return rows;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    rows.add(parseObject(text.substring(start, i + 1)));
                    start = -1;
                }
            }
        }
        return rows;
    }

    public static Map<String, String> parseObject(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null) {
            return values;
        }
        String text = json.trim();
        if (text.startsWith("{")) {
            text = text.substring(1);
        }
        if (text.endsWith("}")) {
            text = text.substring(0, text.length() - 1);
        }

        for (String part : splitFields(text)) {
            int idx = indexOfColon(part);
            if (idx <= 0) {
                continue;
            }
            String key = clean(part.substring(0, idx));
            String value = clean(part.substring(idx + 1));
            values.put(key, value);
        }
        return values;
    }

    private static List<String> splitFields(String text) {
        List<String> parts = new ArrayList<>();
        boolean inString = false;
        boolean escape = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (c == ',' && !inString) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            parts.add(text.substring(start));
        }
        return parts;
    }

    private static int indexOfColon(String text) {
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (c == ':' && !inString) {
                return i;
            }
        }
        return -1;
    }

    private static String clean(String value) {
        String text = value.trim();
        if ("null".equals(text)) {
            return "";
        }
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            text = text.substring(1, text.length() - 1);
        }
        return text.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }
}
