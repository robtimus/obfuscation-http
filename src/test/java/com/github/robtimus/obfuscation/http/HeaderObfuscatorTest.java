/*
 * HeaderObfuscatorTest.java
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
import static com.github.robtimus.obfuscation.http.HeaderObfuscator.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.Function;
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
import com.github.robtimus.obfuscation.http.HeaderObfuscator.Builder;

@SuppressWarnings({ "javadoc", "nls" })
@TestInstance(Lifecycle.PER_CLASS)
public class HeaderObfuscatorTest {

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("testData")
    @DisplayName("obfuscateHeader(String, String)")
    public void testObfuscateHeaderCharSequence(String name, String value, String expected) {
        HeaderObfuscator obfuscator = createObfuscator();
        assertEquals(expected, obfuscator.obfuscateHeader(name, value).toString());
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("testData")
    @DisplayName("obfuscateHeader(String, String, StringBuilder)")
    public void testObfuscateHeaderCharSequenceToStringBuilder(String name, String value, String expected) {
        HeaderObfuscator obfuscator = createObfuscator();

        StringBuilder sb = new StringBuilder();
        obfuscator.obfuscateHeader(name, value, sb);
        assertEquals(expected, sb.toString());
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("testData")
    @DisplayName("obfuscateHeader(String, String, StringBuffer)")
    public void testObfuscateHeaderCharSequenceToStringBuffer(String name, String value, String expected) {
        HeaderObfuscator obfuscator = createObfuscator();

        StringBuffer sb = new StringBuffer();
        obfuscator.obfuscateHeader(name, value, sb);
        assertEquals(expected, sb.toString());
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("testData")
    @DisplayName("obfuscateHeader(String, String, Appendable)")
    public void testObfuscateHeaderCharSequenceToAppendable(String name, String value, String expected) throws IOException {
        HeaderObfuscator obfuscator = createObfuscator();

        Writer writer = new StringWriter();
        obfuscator.obfuscateHeader(name, value, writer);
        assertEquals(expected, writer.toString());
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("testData")
    @DisplayName("obfuscator(String)")
    public void testObfuscator(String name, String value, String expected) {
        HeaderObfuscator obfuscator = createObfuscator();

        Obfuscated<String> obfuscated = obfuscator.obfuscator(name).obfuscateObject(value);
        assertEquals(expected, obfuscated.toString());
        assertSame(value, obfuscated.value());
    }

    @Nested
    @DisplayName("Builder")
    public class BuilderTest {

        @Test
        @DisplayName("transform")
        public void testTransform() {
            Builder builder = builder();
            @SuppressWarnings("unchecked")
            Function<Builder, String> f = mock(Function.class);
            when(f.apply(builder)).thenReturn("result");

            assertEquals("result", builder.transform(f));
            verify(f).apply(builder);
            verifyNoMoreInteractions(f);
        }
    }

    Arguments[] testData() {
        return new Arguments[] {
                arguments("authorization", "value", "*****"),
                arguments("Authorization", "value", "*****"),
                arguments("other", "value", "value"),
                arguments("Other", "value", "value"),
        };
    }

    private HeaderObfuscator createObfuscator() {
        return createObfuscator(builder());
    }

    private HeaderObfuscator createObfuscator(Builder builder) {
        Obfuscator obfuscator = all();
        return builder
                .withHeader("authorization", obfuscator)
                .build();
    }
}
