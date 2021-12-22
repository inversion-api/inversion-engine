package io.inversion.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class JSONPathTokenizer {
    final char escapeChar = '\\';
    final Set  openQuotes;
    final Set  closeQuotes;
    final Set  breakIncluded;
    final Set  breakExcluded;
    final Set  unquotedIgnored;
    final Set  leadingIgnored;
    char[]        chars   = null;
    int           head    = 0;
    boolean       escaped = false;
    boolean       quoted  = false;
    StringBuilder next    = new StringBuilder();

    public JSONPathTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars) {
        this(openQuoteChars, closeQuoteChars, breakIncludedChars, breakExcludedChars, unquotedIgnoredChars, leadingIgnoredChars, null);
    }

    public JSONPathTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars, String chars) {
        openQuotes = toSet(openQuoteChars);
        closeQuotes = toSet(closeQuoteChars);
        breakIncluded = toSet(breakIncludedChars);
        breakExcluded = toSet(breakExcludedChars);
        unquotedIgnored = toSet(unquotedIgnoredChars);
        leadingIgnored = toSet(leadingIgnoredChars);

        withChars(chars);
    }

    /**
     * Resets any ongoing tokenization to tokenize this new string;
     *
     * @param chars the characters to tokenize
     */
    public JSONPathTokenizer withChars(String chars) {
        if (chars != null) {
            this.chars = chars.toCharArray();
        }
        head = 0;
        next = new StringBuilder();
        escaped = false;
        quoted = false;

        return this;
    }

    public List<String> asList() {
        List<String> list = new ArrayList<>();
        String       next;
        while ((next = next()) != null)
            list.add(next);

        return list;
    }

    Set toSet(String string) {
        Set resultSet = new HashSet();
        for (int i = 0; i < string.length(); i++)
            resultSet.add(string.charAt(i));

        return resultSet;
    }

    public String next() {
        if (head >= chars.length) {
            return null;
        }

        while (head < chars.length) {
            char c = chars[head];
            head += 1;

            //System.out.println("c = '" + c + "'");

            if (next.length() == 0 && leadingIgnored.contains(c))
                continue;

            if (c == escapeChar) {
                if (escaped)
                    append(c);

                escaped = !escaped;
                continue;
            }

            if (!quoted && unquotedIgnored.contains(c)) {
                continue;
            }

            if (!quoted && !escaped && openQuotes.contains(c)) {
                quoted = true;
            } else if (quoted && !escaped && closeQuotes.contains(c)) {
                quoted = false;
            }

            if (!quoted && breakExcluded.contains(c) && next.length() > 0) {
                head--;
                break;
            }

            if (!quoted && breakIncluded.contains(c)) {
                append(c);
                break;
            }

            append(c);
        }

        if (quoted)
            throw new RuntimeException("Unable to parse unterminated quoted string: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

        if (escaped)
            throw new RuntimeException("Unable to parse hanging escape character: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

        String str = next.toString().trim();
        next = new StringBuilder();

        if (str.length() == 0)
            str = null;

        return str;
    }

    void append(char c) {
        next.append(c);
    }
}
