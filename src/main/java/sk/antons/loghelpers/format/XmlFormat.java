/*
 * Copyright 2015 Anton Straka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.antons.loghelpers.format;


/**
 * Helper class for formating xml in string form. It does't parse xml as document so it can porduce some error outputs.
 * But it is usefull for makeing some debug log outputs.
 * <pre>
 * String xml = ...;
 * String oneline = XmlFormat.instance(xml, 10000).forceoneline().cutStringLiterals(400).format());
 * String formatted = XmlFormat.instance(xml, 10000).forceoneline().indent("  ").format());
 * </pre>
 *
 * @author antons
 */
public class XmlFormat {
    private Source source = null;
    private StringBuilder newxml = null;
    private boolean oneline = true;
    private boolean forceoneline = false;
    private String indent = "  ";
    private boolean indentAttrs = false;
    private int length;
    private boolean cut = false;
    private int cutLength = 1;

    /**
     * Create instance of formatter.
     * @param xml xml to be formated
     * @param threshold if size of xml &gt; threshold no temporal copy of xml will be created. Otherwice it creates xml.toCharArray() copy is created.
     */
    public XmlFormat(String xml, int threshold) {
        if(xml != null) {
            this.length = xml.length();
        }
        if(threshold >= length) this.source = CharsSource.instance(xml);
        else this.source = StringSource.instance(xml);
    }

    /**
     * Create instance of formatter.
     * @param xml xml to be formated
     * @param threshold if size of xml &gt; threshold no temporal copy of xml will be created. Otherwice it creates xml.toCharArray() copy is created.
     * @return new XmlFormat instance
     */
    public static XmlFormat instance(String xml, int threshold) { return new XmlFormat(xml, threshold); }
    /**
     * Create instance of formatter. with threshold 0 (no temporal copy to char array)
     * @param xml xml to be formated
     * @return new XmlFormat instance
     */
    public static XmlFormat instance(String xml) { return new XmlFormat(xml, 0); }


    /**
     * Cause indended formatting (default is no formatted)
     * @param indent string to indent (like two spaces "  ")
     * @return this
     */
    public XmlFormat indent(String indent) {
        this.indent = indent;
        this.oneline = false;
        return this;
    }

    /**
     * Cause indended formatting for attributes. ( default is no formatted)
     * It is used together with indent() formattong
     * @param indentAttrs attrs will be indentd too
     * @return this
     */
    public XmlFormat indentAttrs(boolean indentAttrs) {
        this.indentAttrs = indentAttrs;
        return this;
    }

    /**
     * Cause escaping of \r and \n chars.
     * @return this
     */
    public XmlFormat forceoneline() {
        this.forceoneline = true;
        return this;
    }

    /**
     * Cause cutting of text literal to specified length.
     * Useful for some debug logs of xml containng long binary data.
     * @param length length to cut literals
     * @return this
     */
    public XmlFormat cutStringLiterals(int length) {
        if(length < 4) length = 4;
        this.cut = true;
        this.cutLength = length;
        return this;
    }


    /**
     * Produce formated output.
     * @return tormated output.
     */
    public String format() {
        if(source.isEmpty()) return source.original();
        if(oneline) this.newxml = new StringBuilder(this.length + this.length/2);
        else this.newxml = new StringBuilder(this.length);

        int index = 0;
        int depth = 0;
        Token prevprevtoken = null;
        Token prevtoken = null;
        Position position = new Position();
        position.reset();
        next(index, position);
        while(position.token != null) {
            //append(index, position.index);
            //System.out.println(" token " + position.token + " - " + position.index);
            int pos = -1;
            switch(position.token) {
                case LEFT:
                    if(position.nonspacecharacters) {
                        append(index, position.index);
                    } else {
                        if(oneline) {
                        } else {
                            appendIndent(depth);
                        }
                    }
                    append(Token.LEFT);
                    depth++;
                    break;
                case RIGHT:
                    appendTag(index, position.index, depth);
                    append(Token.RIGHT);
                    //if(prevtoken == Token.CDATA_LEFT) depth--;
                    break;
                case LEFT_END:
                    depth--;
                    if(((prevprevtoken == Token.LEFT) && (prevtoken == Token.RIGHT))
                        || (prevtoken == Token.CDATA_RIGHT)) {
                        append(index, position.index);
                    } else {
                        if(position.nonspacecharacters) {
                            append(index, position.index);
                        } else {
                            if(oneline) {
                            } else {
                                appendIndent(depth);
                            }
                        }
                    }
                    append(Token.LEFT_END);
                    break;
                case RIGHT_SINGLE:
                    depth--;
                    appendTag(index, position.index, depth);
                    append(Token.RIGHT_SINGLE);
                    break;
                case CDATA_LEFT:
                    append(index, position.index);
                    pos = find(position.index, Token.CDATA_RIGHT);
                    append(Token.CDATA_LEFT);
                    if(pos > -1) {
                        append(position.index+Token.CDATA_LEFT.size(), pos);
                        append(Token.CDATA_RIGHT);
                        position.index = pos;
                        position.token = Token.CDATA_RIGHT;
                    } else {
                        append(position.index+Token.CDATA_LEFT.size(), length);
                        position.index = length;
                        position.token = Token.CDATA_RIGHT;
                        append(index);
                    }
                    break;
                case CDATA_RIGHT:
                    append(index, position.index);
                    append(Token.CDATA_RIGHT);
                    break;
                case COMMENT_LEFT:
                    append(index, position.index);
                    pos = find(position.index, Token.COMMENT_RIGHT);
                    append(Token.COMMENT_LEFT);
                    if(pos > -1) {
                        append(position.index+Token.COMMENT_LEFT.size(), pos);
                        append(Token.COMMENT_RIGHT);
                        position.index = pos;
                        position.token = Token.COMMENT_RIGHT;
                    } else {
                        append(position.index+Token.COMMENT_LEFT.size(), length);
                        position.index = length;
                        position.token = Token.COMMENT_RIGHT;
                        append(index);
                    }
                    break;
                case COMMENT_RIGHT:
                    append(index, position.index);
                    append(Token.COMMENT_RIGHT);
                    break;
                case PROLOG_LEFT:
                    append(Token.PROLOG_LEFT);
                    break;
                case PROLOG_RIGHT:
                    appendTag(index, position.index, depth);
                    append(Token.PROLOG_RIGHT);
                    break;
                default:
            }

            prevprevtoken = prevtoken;
            prevtoken = position.token;
            index = position.index + prevtoken.size();
            position.reset();
            next(index, position);
        }
        append(index);
        return newxml.toString();
    }


    private int find(int index, Token token) {
        return source.indexOf(token, index);
    }

    private void next(int index, Position position) {
        for(int i = index; i < length; i++) {
            char c = source.charAt(i);
            switch(c) {
                case '<':
                    if(i+1<length) {
                        char cc = source.charAt(i+1);
                        switch(cc) {
                            case '/':
                                position.token = Token.LEFT_END;
                                break;
                            case '!':
                                if(source.match(Token.COMMENT_LEFT, i)) position.token = Token.COMMENT_LEFT;
                                else if(source.match(Token.CDATA_LEFT, i)) position.token = Token.CDATA_LEFT;
                                break;
                            case '?':
                                position.token = Token.PROLOG_LEFT;
                                break;
                            default:
                                position.token = Token.LEFT;
                                break;
                        }
                    } else {
                        position.token = Token.LEFT;
                    }
                    break;
                case '>':
                    position.token = Token.RIGHT;
                    break;
                case '-':
                    if(source.match(Token.COMMENT_RIGHT, i)) position.token = Token.COMMENT_RIGHT;
                    break;
                case '/':
                    if(source.match(Token.RIGHT_SINGLE, i)) position.token = Token.RIGHT_SINGLE;
                    break;
                case ']':
                    if(source.match(Token.CDATA_RIGHT, i)) position.token = Token.CDATA_RIGHT;
                    break;
                case '?':
                    if(source.match(Token.PROLOG_RIGHT, i)) position.token = Token.PROLOG_RIGHT;
                    break;
                default:
            }
            if(position.token != null) {
                position.index = i;
                return;
            }
            position.nonspacecharacters = position.nonspacecharacters
                || (!((c == ' ') || (c == '\n') || (c == '\t') || (c == '\r')));
        }
    }

    private static class Position {
        private int index;
        private Token token;
        private boolean nonspacecharacters;

        public void reset() {
            index = -1;
            token = null;
            nonspacecharacters = false;
        }
        public boolean isValid() { return index > -1; }
    }
    private void appendTag(int from, int to, int depth) {
        char prev = 0;
        char escape = 0;
        int spaces = 0;
        for(int i = from; i < to; i++) {
            char c = source.charAt(i);
            if(escape > 0) {
                newxml.append(c);
                if(escape == c) escape = 0;
            } else if(c == '\'') {
                newxml.append(c);
                escape = c;
            } else if(c == '"') {
                newxml.append(c);
                escape = c;
            } else {
                if((c == '\n') || (c == '\t') || (c == '\r')) c = ' ';
                if((c == ' ')) {
                    if((prev != ' ')) {
                        newxml.append(c);
                        if(indentAttrs && (spaces++ > 0)) appendIndent(depth+2);
                    }
                } else {
                    newxml.append(c);
                }
            }
            prev = c;
        }
    }

    private void appendIndent(int depth) {
        if(newxml.length() > 0) newxml.append('\n');
        for(int i = 0; i < depth; i++) {
            newxml.append(indent);
        }
    }
    private void append(int from, int to) {
        source.append(newxml, from, to, forceoneline, cut, cutLength);
    }

    private void append(int from) {
        source.append(newxml, from, forceoneline, cut, cutLength);
    }
    private void append(Token token) {
        newxml.append(token.pattern);
    }

    private static enum Token {
        LEFT("<")
        , RIGHT(">")
        , LEFT_END("</")
        , RIGHT_SINGLE("/>")
        , CDATA_LEFT("<![CDATA[")
        , CDATA_RIGHT("]]>")
        , COMMENT_LEFT("<!--")
        , COMMENT_RIGHT("-->")
        , PROLOG_LEFT("<?")
        , PROLOG_RIGHT("?>")
        ;

        private String value;
        private char[] pattern;
        private Token(String value) {
            this.value = value;
            this.pattern = value.toCharArray();
        }
        public int size() { return pattern.length; }
    }

    private static interface Source {
        int length();
        char charAt(int index);
        boolean isEmpty();
        String original();
        int indexOf(Token token, int index);
        boolean match(Token token, int index);
        void append(StringBuilder sb, int from, int to, boolean forceoneline, boolean cut, int cutLength);
        void append(StringBuilder sb, int from, boolean forceoneline, boolean cut, int cutLength);
    }

    private static class StringSource implements Source {
        private String xml;
        private int length;

        public StringSource(String xml) {
            this.xml = xml;
            if((xml != null) && (xml.length()>0)) {
                this.length = xml.length();
            }
        }

        public static StringSource instance(String xml) { return new StringSource(xml); }

        @Override public int length() { return length; }
        @Override public char charAt(int index) { return xml.charAt(index); }
        @Override public boolean isEmpty() { return length == 0; }
        @Override public String original() { return xml; }
        @Override public int indexOf(Token token, int index) { return xml.indexOf(token.value, index); }

        @Override
        public boolean match(Token token, int index) {
            if(index+token.pattern.length >= length) return false;
            for(int i = 0; i < token.pattern.length; i++) {
                char c1 = xml.charAt(i+index);
                char c2 = token.pattern[i];
                if(c1 != c2) return false;
            }
            return true;
        }

        @Override
        public void append(StringBuilder sb, int from, int to, boolean forceoneline, boolean cut, int cutLength) {
            String value = xml.substring(from, to);
            boolean cutting = false;
            if(cut && value.length() > cutLength) {
                value = value.substring(0, cutLength - 3);
                cutting = true;
            }
            if(forceoneline) {
                value = value.replace("\r", "&#13;");
                value = value.replace("\n", "&#10;");
            }
            sb.append(value);
            if(cutting) sb.append("...");
        }

        @Override
        public void append(StringBuilder sb, int from, boolean forceoneline, boolean cut, int cutLength) {
            String value = xml.substring(from);
            boolean cutting = false;
            if(cut && value.length() > cutLength) {
                value = value.substring(0, cutLength - 3);
                cutting = true;
            }
            if(forceoneline) {
                value = value.replace("\r", "&#13;");
                value = value.replace("\n", "&#10;");
            }
            sb.append(value);
            if(cutting) sb.append("...");
        }

    }

    private static class CharsSource implements Source {
        private String xml;
        private char[] chars;
        private int length;

        public CharsSource(String xml) {
            this.xml = xml;
            if((xml != null) && (xml.length()>0)) {
                this.chars = xml.toCharArray();
                this.length = xml.length();
            }
        }

        public static CharsSource instance(String xml) { return new CharsSource(xml); }

        @Override public int length() { return length; }
        @Override public char charAt(int index) { return chars[index]; }
        @Override public boolean isEmpty() { return length == 0; }
        @Override public String original() { return xml; }

        @Override
        public int indexOf(Token token, int index) {
            int len = token.pattern.length;
            int len2 = length - len;
            for(int i = index; i < len2; i++) {
                boolean found = true;
                for(int j = 0; j < len; j++) {
                    if(chars[i+j] != token.pattern[j]) {
                        found = false;
                        break;
                    }
                }
                if(found) return i;
            }
            return -1;
        }

        @Override
        public boolean match(Token token, int index) {
            int len = token.pattern.length;
            if(len + index > length) return false;
            for(int j = 0; j < len; j++) {
                if(chars[index+j] != token.pattern[j]) return false;
            }
            return true;
        }

        @Override
        public void append(StringBuilder sb, int from, int to, boolean forceoneline, boolean cut, int cutLength) {
            int toto = to;
            boolean cutting = false;
            if(cut) {
               if(from + cutLength < to) {
                   toto = from + cutLength - 3;
                   cutting = true;
               }
            }
            if(forceoneline) {
                for(int i = from; i < toto; i++) {
                    char c = chars[i];
                    switch(c) {
                        case 13:
                            sb.append("&#13;");
                            break;
                        case 10:
                            sb.append("&#10;");
                            break;
                        default:
                            sb.append(c);
                    }
                }
            } else {
                sb.append(chars, from, toto - from);
            }
            if(cutting) sb.append("...");
        }

        @Override
        public void append(StringBuilder sb, int from, boolean forceoneline, boolean cut, int cutLength) {
            int toto = length;
            boolean cutting = false;
            if(cut) {
               if(from + cutLength < length) {
                   toto = from + cutLength - 3;
                   cutting = true;
               }
            }
            if(forceoneline) {
                for(int i = from; i < toto; i++) {
                    char c = chars[i];
                    switch(c) {
                        case 13:
                            sb.append("&#13;");
                            break;
                        case 10:
                            sb.append("&#10;");
                            break;
                        default:
                            sb.append(c);
                    }
                }
            } else {
                sb.append(chars, from, toto - from);
            }
            if(cutting) sb.append("...");
        }

    }


    public static void main(String[] argv) {
        String xml = "";//TextFile.read("/tmp/aaa/a.xml", "utf-8");
        //String xml = TextFile.read("/home/pdocs/year-2020/2020-06-03 - nksr - rfo/xsd/319-2-2v00-20140324 RFO WS Poskytnutie referenčných údajov zoznamu IFO online/PoskytnutieUdajovIFOOnlineWS-v1.0 - zásielka dáta.xml", "utf-8");
        //String xml = "<start><a    xx=\" ka\"    >\n<name>jab\nlko</name>\nsomarina\n</a></start>";
        int threshold = 1000000;
        long timestart = System.currentTimeMillis();
        System.out.println(" ------------");
        System.out.println(XmlFormat.instance(xml, threshold).forceoneline().cutStringLiterals(5).format());
        System.out.println(" ------------");
        System.out.println(XmlFormat.instance(xml, threshold).indent("  ").cutStringLiterals(5).forceoneline().format());
        System.out.println(" ------------");
        System.out.println(XmlFormat.instance(xml, threshold).indent("  ").indentAttrs(true).cutStringLiterals(5).forceoneline().format());
        System.out.println(" ------------");
        System.out.println(XmlFormat.instance(XmlFormat.instance(xml, threshold).forceoneline().format(), threshold).indent("  ").forceoneline().format());
        System.out.println(" ------------");
        long timeend = System.currentTimeMillis();
        System.out.println(" time: " + (timeend - timestart));

//        int num = 0;
//        long tstart = System.currentTimeMillis();
//        for(int i = 0; i < 100; i++) {
//            System.out.println(" ------------");
//            num = num + XmlFormat.instance(xml, threshold).format().length();
//            num = num + XmlFormat.instance(xml, threshold).indent("  ").format().length();
//            //num = num + Xml.documentToString(Xml.document(xml), "utf-8", false, false).length();
//            //num = num + Xml.documentToString(Xml.document(xml), "utf-8", true, false).length();
//        }
//        long tend = System.currentTimeMillis();
//        System.out.println(" num: " + num);
//        System.out.println(" time: " + (tend - tstart));

    }
}
