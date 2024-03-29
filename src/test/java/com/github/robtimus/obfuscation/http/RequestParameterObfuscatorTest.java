/*
 * RequestParameterObfuscatorTest.java
 * Copyright 2020 Rob Spoor
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

package com.github.robtimus.obfuscation.http;

import static com.github.robtimus.obfuscation.Obfuscator.all;
import static com.github.robtimus.obfuscation.http.RequestParameterObfuscator.builder;
import static com.github.robtimus.obfuscation.support.CaseSensitivity.CASE_SENSITIVE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.http.RequestParameterObfuscator.Builder;

@SuppressWarnings("nls")
@TestInstance(Lifecycle.PER_CLASS)
class RequestParameterObfuscatorTest {

    @Nested
    @DisplayName("unlimited")
    class Unlimited {

        @Test
        @DisplayName("obfuscateText(CharSequence, int, int)")
        void testObfuscateTextCharSequence() {
            String input = "xfoo=bar&hello=world&empty=&no-valuey";
            String expected = "foo=***&hello=world&empty=&no-value";

            Obfuscator obfuscator = createObfuscator();

            assertEquals(expected, obfuscator.obfuscateText(input + "&x=y", 1, input.length() - 1).toString());
            assertEquals("foo=**", obfuscator.obfuscateText(input, 1, 7).toString());
            assertEquals("foo", obfuscator.obfuscateText(input, 1, 4).toString());
        }

        @Test
        @DisplayName("obfuscateText(CharSequence, int, int, Appendable)")
        void testObfuscateTextCharSequenceToAppendable() throws IOException {
            String input = "xfoo=bar&hello=world&empty=&no-valuey";
            String expected = "foo=***&hello=world&empty=&no-value";

            Obfuscator obfuscator = createObfuscator();

            StringBuilder destination = new StringBuilder();
            obfuscator.obfuscateText(input + "&x=y", 1, input.length() - 1, (Appendable) destination);
            assertEquals(expected, destination.toString());
        }

        @Test
        @DisplayName("obfuscateText(Reader, Appendable)")
        void testObfuscateTextReaderToAppendable() throws IOException {
            String input = "foo=bar&hello=world&empty=&no-value";
            String expected = "foo=***&hello=world&empty=&no-value";

            Obfuscator obfuscator = createObfuscator();

            StringBuilder destination = new StringBuilder();
            obfuscator.obfuscateText(new StringReader(input), destination);
            assertEquals(expected, destination.toString());

            destination.delete(0, destination.length());
            obfuscator.obfuscateText(new BufferedReader(new StringReader(input)), destination);
            assertEquals(expected, destination.toString());
        }
    }

    @Nested
    @DisplayName("limited")
    class Limited {

        @Nested
        @DisplayName("with truncated indicator")
        class WithTruncatedIndicator {

            @Test
            @DisplayName("obfuscateText(CharSequence, int, int)")
            void testObfuscateTextCharSequence() {
                String input = "xfoo=bar&hello=world&empty=&no-valuey";
                String expected = "foo=***&hell... (total: 35)";

                Obfuscator obfuscator = createObfuscator(builder().limitTo(12));

                assertEquals(expected, obfuscator.obfuscateText(input + "&x=y", 1, input.length() - 1).toString());
                assertEquals("foo=**", obfuscator.obfuscateText(input, 1, 7).toString());
                assertEquals("foo", obfuscator.obfuscateText(input, 1, 4).toString());
            }

            @Test
            @DisplayName("obfuscateText(CharSequence, int, int, Appendable)")
            void testObfuscateTextCharSequenceToAppendable() throws IOException {
                String input = "xfoo=bar&hello=world&empty=&no-valuey";
                String expected = "foo=***&hell... (total: 35)";

                Obfuscator obfuscator = createObfuscator(builder().limitTo(12));

                StringBuilder destination = new StringBuilder();
                obfuscator.obfuscateText(input + "&x=y", 1, input.length() - 1, (Appendable) destination);
                assertEquals(expected, destination.toString());
            }

            @Test
            @DisplayName("obfuscateText(Reader, Appendable)")
            void testObfuscateTextReaderToAppendable() throws IOException {
                String input = "foo=bar&hello=world&empty=&no-value";
                String expected = "foo=***&hell... (total: 35)";

                Obfuscator obfuscator = createObfuscator(builder().limitTo(12));

                StringBuilder destination = new StringBuilder();
                obfuscator.obfuscateText(new StringReader(input), destination);
                assertEquals(expected, destination.toString());

                destination.delete(0, destination.length());
                obfuscator.obfuscateText(new BufferedReader(new StringReader(input)), destination);
                assertEquals(expected, destination.toString());
            }
        }

        @Nested
        @DisplayName("without truncated indicator")
        class WithoutTruncatedIndicator {

            @Test
            @DisplayName("obfuscateText(CharSequence, int, int)")
            void testObfuscateTextCharSequence() {
                String input = "xfoo=bar&hello=world&empty=&no-valuey";
                String expected = "foo=***&hell";

                Obfuscator obfuscator = createObfuscator(builder().limitTo(12).withTruncatedIndicator(null));

                assertEquals(expected, obfuscator.obfuscateText(input + "&x=y", 1, input.length() - 1).toString());
                assertEquals("foo=**", obfuscator.obfuscateText(input, 1, 7).toString());
                assertEquals("foo", obfuscator.obfuscateText(input, 1, 4).toString());
            }

            @Test
            @DisplayName("obfuscateText(CharSequence, int, int, Appendable)")
            void testObfuscateTextCharSequenceToAppendable() throws IOException {
                String input = "xfoo=bar&hello=world&empty=&no-valuey";
                String expected = "foo=***&hell";

                Obfuscator obfuscator = createObfuscator(builder().limitTo(12).withTruncatedIndicator(null));

                StringBuilder destination = new StringBuilder();
                obfuscator.obfuscateText(input + "&x=y", 1, input.length() - 1, (Appendable) destination);
                assertEquals(expected, destination.toString());
            }

            @Test
            @DisplayName("obfuscateText(Reader, Appendable)")
            void testObfuscateTextReaderToAppendable() throws IOException {
                String input = "foo=bar&hello=world&empty=&no-value";
                String expected = "foo=***&hell";

                Obfuscator obfuscator = createObfuscator(builder().limitTo(12).withTruncatedIndicator(null));

                StringBuilder destination = new StringBuilder();
                obfuscator.obfuscateText(new StringReader(input), destination);
                assertEquals(expected, destination.toString());

                destination.delete(0, destination.length());
                obfuscator.obfuscateText(new BufferedReader(new StringReader(input)), destination);
                assertEquals(expected, destination.toString());
            }
        }
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    @DisplayName("streamTo(Appendable)")
    class StreamTo {

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("write(int)")
        void testWriteInt(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier) throws IOException {
            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                for (int i = 0; i < input.length(); i++) {
                    w.write(input.charAt(i));
                }
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("write(char[])")
        void testWriteCharArray(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier)
                throws IOException {

            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                w.write(input.toCharArray());
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("write(char[], int, int)")
        void testWriteCharArrayRange(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier)
                throws IOException {

            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                char[] content = input.toCharArray();
                int index = 0;
                while (index < input.length()) {
                    int to = Math.min(index + 5, input.length());
                    w.write(content, index, to - index);
                    index = to;
                }

                assertThrows(IndexOutOfBoundsException.class, () -> w.write(content, 0, content.length + 1));
                assertThrows(IndexOutOfBoundsException.class, () -> w.write(content, -1, content.length));
                assertThrows(IndexOutOfBoundsException.class, () -> w.write(content, 1, content.length));
                assertThrows(IndexOutOfBoundsException.class, () -> w.write(content, 0, -1));
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("write(String)")
        void testWriteString(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier) throws IOException {
            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                w.write(input);
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("write(String, int, int)")
        void testWriteStringRange(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier)
                throws IOException {

            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                int index = 0;
                while (index < input.length()) {
                    int to = Math.min(index + 5, input.length());
                    w.write(input, index, to - index);
                    index = to;
                }
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("append(CharSequence)")
        void testAppendCharSequence(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier)
                throws IOException {

            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                w.append(input);
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("append(CharSequence, int, int)")
        void testAppendCharSequenceRange(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier)
                throws IOException {

            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                int length = input.length();

                int index = 0;
                while (index < length) {
                    int to = Math.min(index + 5, length);
                    w.append(input, index, to);
                    index = to;
                }

                assertThrows(IndexOutOfBoundsException.class, () -> w.write(input, 0, length + 1));
                assertThrows(IndexOutOfBoundsException.class, () -> w.write(input, -1, length));
                assertThrows(IndexOutOfBoundsException.class, () -> w.write(input, 1, length));
                assertThrows(IndexOutOfBoundsException.class, () -> w.write(input, 0, -1));
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("append(char)")
        void testAppendChar(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier) throws IOException {
            Obfuscator obfuscator = createObfuscator();

            String input = "foo=bar&hello=world&no-value";
            String expected = "foo=***&hello=world&no-value";

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                for (int i = 0; i < input.length(); i++) {
                    w.append(input.charAt(i));
                }
            }
            assertEquals(expected, destination.toString());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("appendableArguments")
        @DisplayName("flush()")
        void testFlush(@SuppressWarnings("unused") String appendableType, Supplier<Appendable> destinationSupplier) throws IOException {
            Obfuscator obfuscator = createObfuscator();

            Appendable destination = destinationSupplier.get();
            try (Writer w = obfuscator.streamTo(destination)) {
                assertDoesNotThrow(w::flush);
            }
        }

        Arguments[] appendableArguments() {
            return new Arguments[] {
                    arguments(StringBuilder.class.getSimpleName(), (Supplier<Appendable>) StringBuilder::new),
                    arguments(StringBuffer.class.getSimpleName(), (Supplier<Appendable>) StringBuffer::new),
                    arguments(StringWriter.class.getSimpleName(), (Supplier<Appendable>) StringWriter::new),
            };
        }
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    @DisplayName("case sensitive")
    class CaseSensitive {

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String)")
        void testObfuscateParameterCharSequence(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseSensitiveByDefault());
            assertEquals(expected, obfuscator.obfuscateParameter(name, value).toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String, StringBuilder)")
        void testObfuscateParameterCharSequenceToStringBuilder(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseSensitiveByDefault());

            StringBuilder sb = new StringBuilder();
            obfuscator.obfuscateParameter(name, value, sb);
            assertEquals(expected, sb.toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String, StringBuffer)")
        void testObfuscateParameterCharSequenceToStringBuffer(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseSensitiveByDefault());

            StringBuffer sb = new StringBuffer();
            obfuscator.obfuscateParameter(name, value, sb);
            assertEquals(expected, sb.toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String, Appendable)")
        void testObfuscateParameterCharSequenceToAppendable(String name, String value, String expected) throws IOException {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseSensitiveByDefault());

            Writer writer = new StringWriter();
            obfuscator.obfuscateParameter(name, value, writer);
            assertEquals(expected, writer.toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscator(String)")
        void testObfuscateParameterValue(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseSensitiveByDefault());

            Obfuscated<String> obfuscated = obfuscator.obfuscateParameterValue(name, value);
            assertEquals(expected, obfuscated.toString());
            assertSame(value, obfuscated.value());
        }

        Arguments[] testData() {
            return new Arguments[] {
                    arguments("foo", "bar", "***"),
                    arguments("Foo", "bar", "bar"),
                    arguments("hello", "world", "world"),
                    arguments("no-value", "", ""),
            };
        }
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    @DisplayName("case insensitive")
    class CaseInsensitive {

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String)")
        void testObfuscateParameterCharSequence(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseInsensitiveByDefault());
            assertEquals(expected, obfuscator.obfuscateParameter(name, value).toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String, StringBuilder)")
        void testObfuscateParameterCharSequenceToStringBuilder(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseInsensitiveByDefault());

            StringBuilder sb = new StringBuilder();
            obfuscator.obfuscateParameter(name, value, sb);
            assertEquals(expected, sb.toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String, StringBuffer)")
        void testObfuscateParameterCharSequenceToStringBuffer(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseInsensitiveByDefault());

            StringBuffer sb = new StringBuffer();
            obfuscator.obfuscateParameter(name, value, sb);
            assertEquals(expected, sb.toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscateParameter(String, String, Appendable)")
        void testObfuscateParameterCharSequenceToAppendable(String name, String value, String expected) throws IOException {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseInsensitiveByDefault());

            Writer writer = new StringWriter();
            obfuscator.obfuscateParameter(name, value, writer);
            assertEquals(expected, writer.toString());
        }

        @ParameterizedTest(name = "{0}: {1} -> {2}")
        @MethodSource("testData")
        @DisplayName("obfuscator(String)")
        void testObfuscator(String name, String value, String expected) {
            RequestParameterObfuscator obfuscator = createObfuscator(builder().caseInsensitiveByDefault());

            Obfuscated<String> obfuscated = obfuscator.obfuscateParameterValue(name, value);
            assertEquals(expected, obfuscated.toString());
            assertSame(value, obfuscated.value());
        }

        Arguments[] testData() {
            return new Arguments[] {
                    arguments("foo", "bar", "***"),
                    arguments("Foo", "bar", "***"),
                    arguments("hello", "world", "world"),
                    arguments("no-value", "", ""),
            };
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource
    @DisplayName("equals(Object)")
    void testEquals(Obfuscator obfuscator, Object object, boolean expected) {
        assertEquals(expected, obfuscator.equals(object));
    }

    Arguments[] testEquals() {
        Obfuscator obfuscator = createObfuscator();
        return new Arguments[] {
                arguments(obfuscator, obfuscator, true),
                arguments(obfuscator, null, false),
                arguments(obfuscator, createObfuscator(), true),
                arguments(obfuscator, builder().build(), false),
                arguments(obfuscator, createObfuscator(StandardCharsets.US_ASCII), false),
                arguments(obfuscator, createObfuscator(builder().limitTo(Long.MAX_VALUE)), true),
                arguments(obfuscator, createObfuscator(builder().limitTo(1024)), false),
                arguments(obfuscator, createObfuscator(builder().limitTo(Long.MAX_VALUE).withTruncatedIndicator(null)), false),
                arguments(obfuscator, "foo", false),
        };
    }

    @Test
    @DisplayName("hashCode()")
    void testHashCode() {
        Obfuscator obfuscator = createObfuscator();
        assertEquals(obfuscator.hashCode(), obfuscator.hashCode());
        assertEquals(obfuscator.hashCode(), createObfuscator().hashCode());
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Nested
        @DisplayName("limitTo")
        class LimitTo {

            @Test
            @DisplayName("negative limit")
            void testNegativeLimit() {
                Builder builder = builder();
                assertThrows(IllegalArgumentException.class, () -> builder.limitTo(-1));
            }
        }

        @Test
        @DisplayName("transform")
        void testTransform() {
            Builder builder = builder();
            @SuppressWarnings("unchecked")
            Function<Builder, String> f = mock(Function.class);
            when(f.apply(builder)).thenReturn("result");

            assertEquals("result", builder.transform(f));
            verify(f).apply(builder);
            verifyNoMoreInteractions(f);
        }
    }

    private RequestParameterObfuscator createObfuscator() {
        return createObfuscator(builder());
    }

    private RequestParameterObfuscator createObfuscator(Charset encoding) {
        return createObfuscator(builder().withEncoding(encoding));
    }

    private RequestParameterObfuscator createObfuscator(Builder builder) {
        Obfuscator obfuscator = all();
        return builder
                .withParameter("foo", obfuscator)
                .withParameter("no-value", obfuscator, CASE_SENSITIVE)
                .build();
    }
}
