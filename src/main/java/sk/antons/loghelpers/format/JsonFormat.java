/*
 * Copyright 2018 Anton Straka
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Convert json in unparsed form. It requires correct json to produce
 * correct output.
 * @author antons
 */
public class JsonFormat {

    private Reader reader = null;
    private Writer writer = null;
    private boolean noindent = false;
    private boolean indent = false;
    private boolean cut = false;
    private String indenttext = null;
    private int cutLength = 1;
    private int expectedLength = 4096;


    private JsonFormat(Reader reader) { this.reader = reader; }

    public static JsonFormat from(String json) {
        return new JsonFormat(new StringReader(json));
    }

    public static JsonFormat from(java.io.Reader json) {
        return new JsonFormat(new ReaderReader(json));
    }

    public JsonFormat noindent() {
        if(indent) throw new IllegalStateException("Indendation was setup already.");
        noindent = true;
        return this;
    }

    public JsonFormat indent(int num, char c) {
        if(noindent) throw new IllegalStateException("No indendation was setup already.");
        indent = true;
        indenttext = "";
        for(int i = 0; i < num; i++) indenttext = indenttext + c;
        return this;
    }
    public JsonFormat indent(String textToIndent) {
        if(noindent) throw new IllegalStateException("No indendation was setup already.");
        indent = true;
        if(textToIndent == null) textToIndent = " ";
        this.indenttext = textToIndent;
        return this;
    }

    public JsonFormat cutStringLiterals(int num) {
        cut = true;
        cutLength = num;
        return this;
    }

    public JsonFormat expectedLength(int num) {
        expectedLength = num;
        return this;
    }

    public String toText() {
        int len = reader.length();
        if(len <=0) len = expectedLength;
        if(indent) len = len*2;
        this.writer = new StringWriter(len);
        process();
        this.writer.flush();
        return this.writer.result();
    }

    public void toWriter(java.io.Writer w) {
        this.writer = new WriterWriter(w);
        process();
        this.writer.flush();
    }

    private void process() {
        int ind = 0;
        int strlen = 0;
        char prev = 0;
        char prevNoSpace = 0;
        char prevprevNoSpace = 0;
        boolean notInJson = true;
        boolean inStringLiteral = false;
        while(reader.something()) {
            char c = reader.next();
//            if((notInJson) && ((c == '{') || (c == '['))) notInJson = false;
//            if(notInJson) {
//                writer.write(c);
//            } else {
                if(inStringLiteral) {
                    if((c == '"') && (prev != '\\')) {
                        inStringLiteral = false;
                        strlen = 0;
                        writer.write(c);
                    } else {
                        if(cut) {
                            if(cutLength > strlen) {
                                writer.write(c);
                            } else if(cutLength < strlen) {
                            } else if(cutLength == strlen) {
                                if(prev == '\\') writer.write("t...");
                                else writer.write(" ...");
                            } else {
                                //writer.write(c);
                            }
                            strlen++;
                        } else {
                            writer.write(c);
                        }
                    }
                } else {
                    switch (c) {
                        case '"':
                            if((prevNoSpace == ',') && (prevprevNoSpace == '}')) {
                                if(indent) {
                                    indent(ind);
                                }
                            }
                            inStringLiteral = true;
                            writer.write(c);
                            break;
                        case ':':
                            if(indent) writer.write(" : ");
                            else writer.write(c);
                            break;
                        case ',':
                            //if(indent && (prevNoSpace == '}')) writer.write(' ');
                            writer.write(c);
                            //if(indent && (prevNoSpace == '}')) writer.write(' ');
                            if(indent) {
                                if((prevNoSpace == '}')) {
                                } else {
                                    indent(ind);
                                }
                            }
                            break;
                        case '{':
                        case '[':
                            writer.write(c);
                            if(indent) {
                                ind++;
                                indent(ind);
                            }
                            break;
                        case '}':
                        case ']':
                            if(indent) {
                                ind--;
                                indent(ind);
                            }
                            writer.write(c);
                            break;
                        case ' ':
                        case '\n':
                        case '\t':
                        case '\r':
                            if(noindent) {
                            } else if(indent) {
                            } else {
                                writer.write(c);
                            }
                            break;
                        default:
                            writer.write(c);
                    }
                }
//            }
            prev = c;
            if(!isSpace(c)) {
                prevprevNoSpace = prevNoSpace;
                prevNoSpace = c;
            }
        }
    }


    private List<String> indents = new ArrayList<String>();

    private void indent(int ind) {
        if(indents.size() <= ind ) {
            StringBuilder sb = new StringBuilder();
            sb.append('\n');
            for(int i = 0; i <= ind; i++) {
                if(i >= indents.size()) indents.add(sb.toString());
                sb.append(indenttext);
            }
        }
        writer.write(indents.get(ind));
        //int num = ind * indentNum;
        //for(int i = 0; i < num; i++) {
        //    writer.write(indentChar);
        //}
    }

    private static boolean isSpace(char c) {
        switch (c) {
            case ' ': return true;
            case '\r': return true;
            case '\n': return true;
            case '\t': return true;
            default: return false;
        }
    }



    private static interface Reader {
        boolean something();
        char next();
        int length();
    }

    private static class StringReader implements Reader {

        private char[] chars = null;
        private int index = 0;
        private int length = 0;

        public StringReader(String s) {
            if(s != null) {
                this.chars = s.toCharArray();
                this.length = chars.length;
                this.index = 0;
            }
        }

        @Override
        public boolean something() {
            return index < length;
        }

        @Override
        public char next() {
            //if(index >= length) throw new IllegalStateException("Unable to read next char");
            return chars[index++];
        }

        @Override
        public int length() {
            return length;
        }



    }

    private static class ReaderReader implements Reader {

        private java.io.Reader reader = null;
        private boolean something = true;
        private char next = 0;

        public ReaderReader(java.io.Reader r) {
            if(!(r instanceof BufferedReader)) r = new BufferedReader(r);
            this.reader = r;
            read();
        }

        @Override
        public boolean something() {
            return something;
        }

        @Override
        public char next() {
            if(something) {
                char c = next;
                read();
                return c;
            } else {
                throw new IllegalStateException("Unable to read next char");
            }
        }

        private void read() {
            try {
                int c = reader.read();
                if(c < 0) {
                    something = false;
                } else {
                    next = (char)c;
                }
            } catch (Exception e) {
                throw new IllegalStateException("Unable to read next character", e);
            }
        }

        @Override
        public int length() {
            return -1;
        }



    }

    private static interface Writer {
        void write(char c);
        void write(String str);
        String result();
        void flush();
    }

    private static class StringWriter implements Writer {

        private StringBuilder sb = null;

        public StringWriter(int size) {
            sb = new StringBuilder(size);
        }

        @Override
        public void write(char c) {
            sb.append(c);
        }

        @Override
        public void write(String str) {
            sb.append(str);
        }


        @Override
        public String result() {
            return sb.toString();
        }

        @Override
        public void flush() {
        }


    }

    private static class WriterWriter implements Writer {

        private java.io.Writer w = null;

        public WriterWriter(java.io.Writer w) {
            if(!(w instanceof BufferedWriter)) w = new BufferedWriter(w);
            this.w = w;
        }

        @Override
        public void write(char c) {
            try {
                w.write(c);
            } catch(Throwable e) {
                throw new IllegalStateException("Unable to write", e);
            }
        }

        @Override
        public void write(String str) {
            try {
                w.write(str);
            } catch(Exception e) {
                throw new IllegalStateException("Unable to write", e);
            }
        }

        @Override
        public void flush() {
            try {
                w.flush();
            } catch(Exception e) {
                throw new IllegalStateException("Unable to flush", e);
            }
        }

        @Override
        public String result() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }



    }

}
