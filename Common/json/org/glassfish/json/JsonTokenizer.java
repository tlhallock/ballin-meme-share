/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.json;

import org.glassfish.json.api.BufferPool;

import javax.json.JsonException;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.math.BigDecimal;
import java.util.Arrays;

import javax.json.stream.JsonParser.Event;

/**
 * JSON Tokenizer
 *
 * @author Jitendra Kotamraju
 */
final class JsonTokenizer implements Closeable {
    private final BufferPool bufferPool;

    private final Reader reader;

    // Internal buffer that is used for parsing. It is also used
    // for storing current string and number value token
    private char[] buf;

    // Indexes in buffer
    //
    // XXXssssssssssssXXXXXXXXXXXXXXXXXXXXXXrrrrrrrrrrrrrrXXXXXX
    //    ^           ^                     ^             ^
    //    |           |                     |             |
    //   storeBegin  storeEnd            readBegin      readEnd
    private int readBegin;
    private int readEnd;
    private int storeBegin;
    private int storeEnd;

    // line number of the current pointer of parsing char
    private long lineNo = 1;

    // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    // ^
    // |
    // bufferOffset
    //
    // offset of the last \r\n or \n. will be used to calculate column number
    // of a token or an error. This may be outside of the buffer.
    private long lastLineOffset = 0;
    // offset in the stream for the start of the buffer, will be used in
    // calculating JsonLocation's stream offset, column no.
    private long bufferOffset = 0;

    private boolean minus;
    private boolean fracOrExp;
    private String value;
    private BigDecimal bd;

    enum JsonToken {
        CURLYOPEN(Event.START_OBJECT, false),
        SQUAREOPEN(Event.START_ARRAY, false),
        COLON(null, false),
        COMMA(null, false),
        STRING(Event.VALUE_STRING, true),
        NUMBER(Event.VALUE_NUMBER, true),
        TRUE(Event.VALUE_TRUE, true),
        FALSE(Event.VALUE_FALSE, true),
        NULL(Event.VALUE_NULL, true),
        CURLYCLOSE(Event.END_OBJECT, false),
        SQUARECLOSE(Event.END_ARRAY, false),
        EOF(null, false);

        private final JsonParser.Event event;
        private final boolean value;

        JsonToken(JsonParser.Event event, boolean value) {
            this.event = event;
            this.value = value;
        }

        JsonParser.Event getEvent() {
            return event;
        }

        boolean isValue() {
            return value;
        }
    }

    JsonTokenizer(Reader reader, BufferPool bufferPool) {
        this.reader = reader;
        this.bufferPool = bufferPool;
        buf = bufferPool.take();
    }

    private void readString() {
        // when inPlace is true, no need to copy chars
        boolean inPlace = true;
        storeBegin = storeEnd = readBegin;

        do {
            // Write unescaped char block within the current buffer
            if (inPlace && readBegin < readEnd) {
                do {
                    int ch = buf[readBegin];
                    if (ch >= 0x20 && ch <= 0x10ffff && ch != 0x22 && ch != 0x5c) {
                        readBegin++;        // consume unescaped char
                    } else if (ch == '"') {
                        storeEnd = readBegin;
                        readBegin++;        // consume quote char
                        return;             // Got the entire string
                    } else {
                        break;              // escaped char
                    }
                } while(readBegin < readEnd);
                storeEnd = readBegin;
            }

            // string may be crossing buffer boundaries and may contain
            // escaped characters.
            int ch = read();
            if (ch >= 0x20 && ch <= 0x10ffff && ch != 0x22 && ch != 0x5c) {
                if (!inPlace) {
                    buf[storeEnd] = (char)ch;
                }
                storeEnd++;
                continue;
            }
            switch (ch) {
                case '\\':
                    inPlace = false;        // Now onwards need to copy chars
                    unescape();
                    break;
                case '"':
                    return;
                case -1:
                    throw new JsonException("Unexpected EOF");
                default:
                    throw new JsonParsingException("Unexpected Char="+ch, getLastCharLocation());
            }
        } while (true);
    }

    private void unescape() {
        int ch = read();
        switch (ch) {
            case 'b':
                buf[storeEnd++] = '\b';
                break;
            case 't':
                buf[storeEnd++] = '\t';
                break;
            case 'n':
                buf[storeEnd++] = '\n';
                break;
            case 'f':
                buf[storeEnd++] = '\f';
                break;
            case 'r':
                buf[storeEnd++] = '\r';
                break;
            case '"':
            case '\\':
            case '/':
                buf[storeEnd++] = (char)ch;
                break;
            case 'u': {
                char unicode = 0;
                for (int i = 0; i < 4; i++) {
                    int ch3 = read();
                    unicode <<= 4;
                    if (ch3 >= '0' && ch3 <= '9') {
                        unicode |= ((char) ch3) - '0';
                    } else if (ch3 >= 'a' && ch3 <= 'f') {
                        unicode |= (((char) ch3) - 'a') + 0xA;
                    } else if (ch3 >= 'A' && ch3 <= 'F') {
                        unicode |= (((char) ch3) - 'A') + 0xA;
                    } else {
                        throw new JsonParsingException("Unexpected Char="+ch3, getLastCharLocation());
                    }
                }
                buf[storeEnd++] = (char) (unicode & 0xffff);
                break;
            }
            default:
                throw new JsonParsingException("Unexpected Char="+ch, getLastCharLocation());
        }
    }

    // Reads a number char. If the char is within the buffer, directly
    // reads from the buffer. Otherwise, uses read() which takes care
    // of resizing, filling up the buf, adjusting the pointers
    private int readNumberChar() {
        int ch;
        if (readBegin < readEnd) {
            ch = buf[readBegin++];
        } else {
            storeEnd = readBegin;
            ch = read();
        }
        return ch;
    }

    private void readNumber(int ch)  {
        storeBegin = storeEnd = readBegin-1;
        // sign
        if (ch == '-') {
            this.minus = true;
            ch = readNumberChar();
            if (ch < '0' || ch >'9') {
                throw new JsonParsingException("Unexpected Char="+ch, getLastCharLocation());
            }
        }

        // int
        if (ch == '0') {
            ch = readNumberChar();
        } else {
            do {
                ch = readNumberChar();
            } while (ch >= '0' && ch <= '9');
        }

        // frac
        if (ch == '.') {
            this.fracOrExp = true;
            int count = 0;
            do {
                ch = readNumberChar();
                count++;
            } while (ch >= '0' && ch <= '9');
            if (count == 1) {
                throw new JsonParsingException("Unexpected Char="+ch, getLastCharLocation());
            }
        }

        // exp
        if (ch == 'e' || ch == 'E') {
            this.fracOrExp = true;
            ch = readNumberChar();
            if (ch == '+' || ch == '-') {
                ch = readNumberChar();
            }
            int count;
            for (count = 0; ch >= '0' && ch <= '9'; count++) {
                ch = readNumberChar();
            }
            if (count == 0) {
                throw new JsonParsingException("Unexpected Char="+ch, getLastCharLocation());
            }
        }
        readBegin--;
        storeEnd = readBegin;
    }

    private void readTrue() {
        char ch1 = (char) read();
        if (ch1 != 'r') {
            throw new JsonParsingException("Unexpected Char="+ch1+" expecting 'r'", getLastCharLocation());
        }
        char ch2 = (char) read();
        if (ch2 != 'u') {
            throw new JsonParsingException("Unexpected Char="+ch2+" expecting 'u'", getLastCharLocation());
        }
        char ch3 = (char) read();
        if (ch3 != 'e') {
            throw new JsonParsingException("Unexpected Char="+ch3+" expecting 'e'", getLastCharLocation());
        }
    }

    private void readFalse() {
        char ch1 = (char) read();
        if (ch1 != 'a') {
            throw new JsonParsingException("Unexpected Char="+ch1+" expecting 'a'", getLastCharLocation());
        }
        char ch2 = (char) read();
        if (ch2 != 'l') {
            throw new JsonParsingException("Unexpected Char="+ch2+" expecting 'l'", getLastCharLocation());
        }
        char ch3 = (char) read();
        if (ch3 != 's') {
            throw new JsonParsingException("Unexpected Char="+ch3+" expecting 's'", getLastCharLocation());
        }
        char ch4 = (char) read();
        if (ch4 != 'e') {
            throw new JsonParsingException("Unexpected Char="+ch4+" expecting 'e'", getLastCharLocation());
        }
    }

    private void readNull() {
        char ch1 = (char) read();
        if (ch1 != 'u') {
            throw new JsonParsingException("Unexpected Char="+ch1+" expecting 'u'", getLastCharLocation());
        }
        char ch2 = (char) read();
        if (ch2 != 'l') {
            throw new JsonParsingException("Unexpected Char="+ch2+" expecting 'l'", getLastCharLocation());
        }
        char ch3 = (char) read();
        if (ch3 != 'l') {
            throw new JsonParsingException("Unexpected Char="+ch3+" expecting 'l'", getLastCharLocation());
        }
    }

    JsonToken nextToken() throws IOException {
        reset();
        int ch = read();

        // whitespace
        while (ch == 0x20 || ch == 0x09 || ch == 0x0a || ch == 0x0d) {
            if (ch == '\r') {
                ++lineNo;
                ch = read();
                if (ch == '\n') {
                    lastLineOffset = bufferOffset+readBegin;
                } else {
                    lastLineOffset = bufferOffset+readBegin-1;
                    continue;
                }
            } else if (ch == '\n') {
                ++lineNo;
                lastLineOffset = bufferOffset+readBegin;
            }
            ch = read();
        }

        switch (ch) {
            case '"':
                readString();
                return JsonToken.STRING;
            case '{':
                return JsonToken.CURLYOPEN;
            case '[':
                return JsonToken.SQUAREOPEN;
            case ':':
                return JsonToken.COLON;
            case ',':
                return JsonToken.COMMA;
            case 't':
                readTrue();
                return JsonToken.TRUE;
            case 'f':
                readFalse();
                return JsonToken.FALSE;
            case 'n':
                readNull();
                return JsonToken.NULL;
            case ']':
                return JsonToken.SQUARECLOSE;
            case '}':
                return JsonToken.CURLYCLOSE;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
                readNumber(ch);
                return JsonToken.NUMBER;
            case -1:
                return JsonToken.EOF;
            default:
                throw new JsonParsingException("Unexpected char="+(char)ch, getLastCharLocation());
        }
    }

    // Gives the location of the last char. Used for
    // JsonParsingException.getLocation
    JsonLocation getLastCharLocation() {
        // Already read the char, so subtracting -1
        return new JsonLocationImpl(lineNo, bufferOffset +readBegin-lastLineOffset, bufferOffset +readBegin-1);
    }

    // Gives the parser location. Used for JsonParser.getLocation
    JsonLocation getLocation() {
        return new JsonLocationImpl(lineNo, bufferOffset +readBegin-lastLineOffset+1, bufferOffset +readBegin);
    }

    private int read() {
        try {
            if (readBegin == readEnd) {     // need to fill the buffer
                int len = fillBuf();
                if (len == -1) {
                    return -1;
                }
                assert len != 0;
                readBegin = storeEnd;
                readEnd = readBegin+len;
            }
            return buf[readBegin++];
        } catch (IOException ioe) {
            throw new JsonException("I/O error while tokenizing JSON", ioe);
        }
    }

    private int fillBuf() throws IOException {
        if (storeEnd != 0) {
            int storeLen = storeEnd-storeBegin;
            if (storeLen > 0) {
                // there is some store data
                if (storeLen == buf.length) {
                    // buffer is full, double the capacity
                    char[] doubleBuf = Arrays.copyOf(buf, 2 * buf.length);
                    bufferPool.recycle(buf);
                    buf = doubleBuf;
                } else {
                    // Left shift all the stored data to make space
                    System.arraycopy(buf, storeBegin, buf, 0, storeLen);
                    storeEnd = storeLen;
                    storeBegin = 0;
                    bufferOffset += readBegin-storeEnd;
                }
            } else {
                storeBegin = storeEnd = 0;
                bufferOffset += readBegin;
            }
        } else {
            bufferOffset += readBegin;
        }
        // Fill the rest of the buf
        return reader.read(buf, storeEnd, buf.length-storeEnd);
    }

    // state associated with the current token is no more valid
    private void reset() {
        if (storeEnd != 0) {
            storeBegin = 0;
            storeEnd = 0;
            value = null;
            bd = null;
            minus = false;
            fracOrExp = false;
        }
    }

    String getValue() {
        if (value == null) {
            value = new String(buf, storeBegin, storeEnd-storeBegin);
        }
        return value;
    }

    BigDecimal getBigDecimal() {
        if (bd == null) {
            bd = new BigDecimal(buf, storeBegin, storeEnd-storeBegin);
        }
        return bd;
    }

    int getInt() {
        // no need to create BigDecimal for common integer values (1-9 digits)
        int storeLen = storeEnd-storeBegin;
        if (!fracOrExp && (storeLen <= 9 || (minus && storeLen == 10))) {
            int num = 0;
            int i = minus ? 1 : 0;
            for(; i < storeLen; i++) {
                num = num * 10 + (buf[storeBegin+i] - '0');
            }
            return minus ? -num : num;
        } else {
            return getBigDecimal().intValue();
        }
    }

    boolean isIntegral() {
        return !fracOrExp || getBigDecimal().scale() == 0;
    }

    @Override
    public void close() throws IOException {
        reader.close();
        bufferPool.recycle(buf);
    }
    
}