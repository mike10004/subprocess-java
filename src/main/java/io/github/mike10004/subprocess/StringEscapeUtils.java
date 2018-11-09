package io.github.mike10004.subprocess;

/**
 * A utility more primitive than Apache Commons Text's escaper.
 */
class StringEscapeUtils {

    private StringEscapeUtils() {}

    private static final String LEGAL = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`-=,./;[]~!@#$%^&*()~_+{}|:<>?";

    private static final char[][] BACKSLASH_ESCAPES = {
            {'\n', 'n'},
            {'\t', 't'},
            {'\r', 'r'},
            {'\'', '\''},
            {'\"', '\"'},
    };

    private static boolean isLegal(char ch) {
        return LEGAL.indexOf(ch) >= 0;
    }

    private static char backslashMapIndex(char ch) {
        for (char[] mapping : BACKSLASH_ESCAPES) {
            if (ch == mapping[0]) {
                return mapping[1];
            }
        }
        return NULLCHAR;
    }

    private static final char NULLCHAR = '\0';

    public static String escapeJava(String s) {
        if (s == null) {
            return null;
        }
        boolean allLegal = true;
        for (int i = 0; i < s.length() && allLegal; i++) {
            if (!isLegal(s.charAt(i))) {
                allLegal = false;
            }
        }
        if (allLegal) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            char bs;
            if (isLegal(ch)) {
                sb.append(ch);
            } else if ((bs = backslashMapIndex(ch)) != NULLCHAR) {
                sb.append('\\').append(bs);
            } else {
                sb.append('\\')
                        .append('x')
                        .append(hex(ch));

            }
        }
        String escaped = sb.toString();
        return escaped;
    }

    private static String hex(final char ch) {
        return String.format("%02x", (int) ch);
    }

}
