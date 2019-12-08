package org.projectbuendia.openmrs.web.controller;

import org.projectbuendia.models.Intl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HtmlOutput {
    public interface Writable {
        void writeHtmlTo(LocalizedWriter writer) throws IOException;
        boolean isEmpty();
    }

    public static Sequence seq(Object... objects) {
        Sequence sequence = new Sequence();
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            sequence.add(
                obj instanceof Object[] ? seq((Object[]) obj)
                    : obj instanceof Writable ? (Writable) obj
                        : text(obj)
            );
        }
        return sequence;
    }

    public static Element el(String tag, Object... objects) {
        return new Element(tag, seq(objects));
    }

    public static Html html(String html) {
        return new Html(html);
    }

    public static Writable text(Object object) {
        return object instanceof Intl ? new IntlText((Intl) object) : new Text("" + object);
    }

    public static Writable intl(Object object) {
        return object instanceof Intl ? new IntlText((Intl) object) : new IntlText("" + object);
    }

    public static Writable format(String intl, Object... args) {
        return new Format(new Intl(intl), args);
    }

    public static Writable format(Intl intl, Object... args) {
        return new Format(intl, args);
    }

    public static class Sequence implements Writable {
        private final List<Writable> children = new ArrayList<>();

        public Sequence(Writable... children) {
            for (Writable child : children) {
                this.children.add(child);
            }
        }

        public Sequence add(Writable... children) {
            for (Writable child : children) {
                this.children.add(child);
            }
            return this;
        }

        public void writeHtmlTo(LocalizedWriter writer) throws IOException {
            for (Writable child : children) {
                child.writeHtmlTo(writer);
            }
        }

        public boolean isEmpty() {
            for (Writable child : children) {
                if (!child.isEmpty()) return false;
            }
            return true;
        }
    }

    public static class Element implements Writable {
        private final String tag;
        private final Sequence content;

        public Element(String tag, Sequence content) {
            this.tag = tag;
            this.content = content;
        }

        public void writeHtmlTo(LocalizedWriter writer) throws IOException {
            writer.write("<" + tag + "\n>");
            content.writeHtmlTo(writer);
            writer.write("\n</" + tag.split(" ")[0] + ">");
        }

        public boolean isEmpty() {
            return false;
        }
    }

    public static class Html implements Writable {
        private final String html;

        public Html(String html) {
            this.html = html;
        }

        public void writeHtmlTo(LocalizedWriter writer) throws IOException {
            writer.write(html);
        }

        public boolean isEmpty() {
            return html.isEmpty();
        }
    }

    public static class IntlText implements Writable {
        private Intl intl;

        public IntlText(String text) {
            this(new Intl(text));
        }

        public IntlText(Intl intl) {
            this.intl = intl;
        }

        public void writeHtmlTo(LocalizedWriter writer) throws IOException {
            writer.writeEscaped(intl);
        }

        public boolean isEmpty() {
            return intl.isEmpty();
        }
    }

    public static class Text implements Writable {
        private String text;

        public Text(String text) {
            this.text = text;
        }

        public void writeHtmlTo(LocalizedWriter writer) throws IOException {
            writer.writeEscaped(text);
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }
    }

    public static class Format implements Writable {
        private Intl intl;
        private Object[] args;

        public Format(Intl intl, Object... args) {
            this.intl = intl;
            this.args = args;
        }

        public void writeHtmlTo(LocalizedWriter writer) throws IOException {
            writer.writeEscaped(intl, args);
        }

        public boolean isEmpty() {
            return intl.isEmpty();
        }
    }

    interface Renderer {
        Writable render(Object obj);
    }

    public static Sequence formatItems(Renderer renderer, List<?> objects) {
        Sequence seq = new Sequence();
        for (Object obj : objects) {
            seq.add(renderer.render(obj));
        }
        return seq;
    }

    public static Sequence formatItems(Renderer renderer, Object[] objects) {
        Sequence seq = new Sequence();
        for (Object obj : objects) {
            seq.add(renderer.render(obj));
        }
        return seq;
    }

    public static class LocalizedWriter {
        private final Writer writer;
        private final Locale locale;

        public LocalizedWriter(Writer writer, Locale locale) {
            this.writer = writer;
            this.locale = locale;
        }

        public void write(String str) throws IOException {
            writer.write(str);
        }

        public void writeEscaped(String text) throws IOException {
            writer.write(esc(text));
        }

        public void writeEscaped(Intl intl, Object... args) throws IOException {
            String localized = intl.loc(locale);
            writer.write(esc(String.format(Locale.US, localized, args)));
        }
    }

    private static String esc(String x) {
        return x.replace("&", "&amp;").replace("<", "&lt;");
    }
}