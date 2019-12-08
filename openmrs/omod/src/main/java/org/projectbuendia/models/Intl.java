package org.projectbuendia.models;

import org.openmrs.projectbuendia.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A localizable string, made from a string of the form "cat [fr:chat] [es:gato]". */
public class Intl {
    private static final Pattern BRACKETED_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private static final Pattern EXTRA_SPACES = Pattern.compile("^ *| *$");

    public final String base;
    public final Map<String, String> options;

    public Intl(String packed) {
        if (packed == null) packed = "";
        String unpacked = BRACKETED_PATTERN.matcher(packed).replaceAll("");
        base = EXTRA_SPACES.matcher(unpacked).replaceAll("");
        options = new HashMap<>();
        Matcher matcher = BRACKETED_PATTERN.matcher(packed);
        for (int pos = 0; matcher.find(pos); pos = matcher.end(1)) {
            String[] parts = Utils.splitFields(matcher.group(1), ":", 2);
            options.put(parts[0], parts[1]);
        }
    }

    public Intl(String base, Map<String, String> options) {
        this.base = base;
        this.options = options;
    }

    public boolean isEmpty() {
        return base.isEmpty() && options.isEmpty();
    }

    public String loc(Locale locale) {
        if (options == null || options.isEmpty() || locale == null) return base;

        String tag = Utils.toLanguageTag(locale);
        if (options.containsKey(tag)) return options.get(tag);

        String lang = locale.getLanguage();
        String region = locale.getCountry();
        String variant = locale.getVariant();
        tag = Utils.toLanguageTag(new Locale(lang, region, variant));
        if (options.containsKey(tag)) return options.get(tag);
        tag = Utils.toLanguageTag(new Locale(lang, region));
        if (options.containsKey(tag)) return options.get(tag);
        tag = Utils.toLanguageTag(new Locale(lang));
        if (options.containsKey(tag)) return options.get(tag);

        return base;
    }

    public String[] getAll() {
        String[] values = options.values().toArray(new String[0]);
        String[] results = new String[values.length + 1];
        results[0] = base;
        System.arraycopy(values, 0, results, 1, values.length);
        return results;
    }

    public static Intl[] newArray(String... strings) {
        Intl[] intls = new Intl[strings.length];
        for (int i = 0; i < intls.length; i++) {
            intls[i] = new Intl(strings[i]);
        }
        return intls;
    }

    public String toString() {
        return "Intl(" + base + ")";
    }
}
