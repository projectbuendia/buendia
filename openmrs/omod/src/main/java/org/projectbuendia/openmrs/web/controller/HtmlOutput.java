package org.projectbuendia.openmrs.web.controller;

import org.openmrs.projectbuendia.Utils;
import org.projectbuendia.models.Intl;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HtmlOutput {
    public interface Doc {
        void writeTo(Writer writer, Locale locale) throws IOException;
        boolean isEmpty();
    }

    public static String render(Doc doc, Locale locale) throws IOException {
        StringWriter buffer = new StringWriter();
        doc.writeTo(buffer, locale);
        return buffer.toString();
    }

    public static Sequence seq(Object... objects) {
        Sequence sequence = new Sequence();
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            sequence.add(
                obj instanceof Object[] ? seq((Object[]) obj)
                    : obj instanceof Doc ? (Doc) obj
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

    public static Doc text(Object object) {
        return object instanceof Intl ? (Intl) object : new Text("" + object);
    }

    public static Doc intl(Object object) {
        return object instanceof Intl ? (Intl) object : new Intl("" + object);
    }

    public static Doc format(String intl, Object... args) {
        return new Format(new Intl(intl), args);
    }

    public static Doc format(Intl intl, Object... args) {
        return new Format(intl, args);
    }

    public static class Sequence implements Doc {
        private final List<Doc> children = new ArrayList<>();

        public Sequence(Doc... children) {
            for (Doc child : children) {
                this.children.add(child);
            }
        }

        public Sequence add(Doc... children) {
            for (Doc child : children) {
                this.children.add(child);
            }
            return this;
        }

        public void writeTo(Writer writer, Locale locale) throws IOException {
            for (Doc child : children) {
                child.writeTo(writer, locale);
            }
        }

        public boolean isEmpty() {
            for (Doc child : children) {
                if (!child.isEmpty()) return false;
            }
            return true;
        }
    }

    public static class Element implements Doc {
        private final String tag;
        private final Sequence content;

        public Element(String tag, Sequence content) {
            this.tag = tag;
            this.content = content;
        }

        public void writeTo(Writer writer, Locale locale) throws IOException {
            writer.write("<" + tag + "\n>");
            content.writeTo(writer, locale);
            writer.write("</" + tag.split(" ")[0] + "\n>");
        }

        public boolean isEmpty() {
            return false;
        }
    }

    public static class Text implements Doc {
        private String text;

        public Text(String text) {
            this.text = text;
        }

        public void writeTo(Writer writer, Locale locale) throws IOException {
            writer.write(esc(text));
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }
    }

    public static class Html implements Doc {
        private String html;

        public Html(String html) {
            this.html = html;
        }

        public void writeTo(Writer writer, Locale locale) throws IOException {
            writer.write(html);
        }

        public boolean isEmpty() {
            return html.isEmpty();
        }
    }

    public static class Format implements Doc {
        private Intl template;
        private Object[] args;

        public Format(Intl template, Object... args) {
            this.template = template;
            this.args = args;
        }

        public void writeTo(Writer writer, Locale locale) throws IOException {
            String locTemplate = template.loc(locale);
            Object[] locArgs = new Object[args.length];
            int i = 0;
            for (Object arg : args) {
                locArgs[i++] = arg instanceof Doc ? render((Doc) arg, locale) : arg;
            }
            writer.write(String.format(
                Utils.orDefault(locale, Locale.US), locTemplate, locArgs));
        }

        public boolean isEmpty() {
            return template.isEmpty();
        }
    }

    interface Renderer {
        Doc render(Object obj);
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

        public void writeEscaped(Intl template, Object... args) throws IOException {
            String locTemplate = template.loc(locale);
            Object[] locArgs = new Object[args.length];
            int i = 0;
            for (Object arg : args) {
                locArgs[i++] = arg instanceof Intl ? ((Intl) arg).loc(locale) : arg;
            }
            writer.write(esc(String.format(Locale.US, locTemplate, locArgs)));
        }
    }

    private static String esc(String x) {
        return x.replace("&", "&amp;").replace("<", "&lt;");
    }
}