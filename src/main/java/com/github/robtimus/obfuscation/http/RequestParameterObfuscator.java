/*
 * RequestParameterObfuscator.java
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

import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.checkStartAndEnd;
import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.indexOf;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.support.CachingObfuscatingWriter;
import com.github.robtimus.obfuscation.support.CaseSensitivity;
import com.github.robtimus.obfuscation.support.MapBuilder;

/**
 * An obfuscator that obfuscates request parameters in {@link CharSequence CharSequences} or the contents of {@link Reader Readers}.
 * It can be used for both query strings and form data strings.
 * <p>
 * In addition to obfuscating request parameters in text, it can also obfuscate values from already-parsed request parameters.
 *
 * @author Rob Spoor
 */
public final class RequestParameterObfuscator extends Obfuscator {

    private final Map<String, Obfuscator> obfuscators;
    private final Charset encoding;

    private RequestParameterObfuscator(Builder builder) {
        obfuscators = builder.obfuscators();
        encoding = builder.encoding;
    }

    @Override
    public CharSequence obfuscateText(CharSequence s, int start, int end) {
        checkStartAndEnd(s, start, end);
        StringBuilder sb = new StringBuilder(end - start);
        obfuscateText(s, start, end, sb);
        return sb.toString();
    }

    @Override
    public void obfuscateText(CharSequence s, int start, int end, Appendable destination) throws IOException {
        checkStartAndEnd(s, start, end);

        int index;
        while ((index = indexOf(s, '&', start, end)) != -1) {
            maskKeyValue(s, start, index, destination);
            destination.append('&');
            start = index + 1;
        }
        // remainder
        maskKeyValue(s, start, end, destination);
    }

    @Override
    public void obfuscateText(Reader input, Appendable destination) throws IOException {
        BufferedReader br = input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input);
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = br.read()) != -1) {
            if (c == '&') {
                maskKeyValue(sb, 0, sb.length(), destination);
                sb.delete(0, sb.length());
                destination.append('&');
            } else {
                sb.append((char) c);
            }
        }
        // remainder
        maskKeyValue(sb, 0, sb.length(), destination);
    }

    private void maskKeyValue(CharSequence s, int start, int end, Appendable destination) throws IOException {
        int index = indexOf(s, '=', start, end);
        if (index == -1) {
            // no value so nothing to mask
            destination.append(s, start, end);
        } else {
            String name = URLDecoder.decode(s.subSequence(start, index).toString(), encoding.name());
            Obfuscator obfuscator = obfuscators.get(name);
            if (obfuscator == null) {
                destination.append(s, start, end);
            } else {
                String value = URLDecoder.decode(s.subSequence(index + 1, end).toString(), encoding.name());
                destination.append(s, start, index + 1);
                CharSequence obfuscated = obfuscator.obfuscateText(value);
                destination.append(URLEncoder.encode(obfuscated.toString(), encoding.name()));
            }
        }
    }

    @Override
    public Writer streamTo(Appendable destination) {
        return new CachingObfuscatingWriter(this, destination);
    }

    /**
     * Obfuscates the value of a parameter.
     *
     * @param name The name of the parameter to obfuscate.
     * @param value The parameter value to obfuscate.
     * @return The obfuscated parameter value.
     * @throws NullPointerException If the given name or value is {@code null}.
     */
    public CharSequence obfuscateParameter(String name, String value) {
        return obfuscator(name).obfuscateText(value);
    }

    /**
     * Obfuscates the value of a parameter.
     *
     * @param name The name of the parameter to obfuscate.
     * @param value The parameter value to obfuscate.
     * @param destination The {@code StringBuilder} to append the obfuscated parameter value to.
     * @throws NullPointerException If the given name, value or {@code StringBuilder} is {@code null}.
     */
    public void obfuscateParameter(String name, String value, StringBuilder destination) {
        obfuscator(name).obfuscateText(value, destination);
    }

    /**
     * Obfuscates the value of a parameter.
     *
     * @param name The name of the parameter to obfuscate.
     * @param value The parameter value to obfuscate.
     * @param destination The {@code StringBuffer} to append the obfuscated parameter value to.
     * @throws NullPointerException If the given name, value or {@code StringBuffer} is {@code null}.
     */
    public void obfuscateParameter(String name, String value, StringBuffer destination) {
        obfuscator(name).obfuscateText(value, destination);
    }

    /**
     * Obfuscates the value of a parameter.
     *
     * @param name The name of the parameter to obfuscate.
     * @param value The parameter value to obfuscate.
     * @param destination The {@code Appendable} to append the obfuscated parameter value to.
     * @throws NullPointerException If the given name, value or {@code Appendable} is {@code null}.
     * @throws IOException If an I/O error occurs.
     */
    public void obfuscateParameter(String name, String value, Appendable destination) throws IOException {
        obfuscator(name).obfuscateText(value, destination);
    }

    /**
     * Obfuscates the value of a parameter.
     *
     * @param name The name of the parameter to obfuscate.
     * @param value The parameter value to obfuscate.
     * @return An {@code Obfuscated} wrapper around the given value.
     * @throws NullPointerException If the given name or value is {@code null}.
     */
    public Obfuscated<String> obfuscateParameterValue(String name, String value) {
        return obfuscator(name).obfuscateObject(value);
    }

    private Obfuscator obfuscator(String name) {
        Objects.requireNonNull(name);
        return obfuscators.getOrDefault(name, none());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        RequestParameterObfuscator other = (RequestParameterObfuscator) o;
        return obfuscators.equals(other.obfuscators)
                && encoding.equals(other.encoding);
    }

    @Override
    public int hashCode() {
        return obfuscators.hashCode() ^ encoding.hashCode();
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getName()
                + "[obfuscators=" + obfuscators
                + ",encoding=" + encoding
                + "]";
    }

    /**
     * Returns a builder that will create {@code RequestParameterObfuscators}.
     *
     * @return A builder that will create {@code RequestParameterObfuscators}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link RequestParameterObfuscator RequestParameterObfuscators}.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private final MapBuilder<Obfuscator> obfuscators;

        private Charset encoding;

        private Builder() {
            obfuscators = new MapBuilder<>();
            encoding = StandardCharsets.UTF_8;
        }

        /**
         * Adds a parameter to obfuscate.
         * This method is an alias for {@link #withParameter(String, Obfuscator, CaseSensitivity)} with the last specified default case sensitivity
         * using {@link #caseSensitiveByDefault()} or {@link #caseInsensitiveByDefault()}. The default is {@link CaseSensitivity#CASE_SENSITIVE}.
         *
         * @param parameter The name of the parameter. It will be treated case sensitively.
         * @param obfuscator The obfuscator to use for obfuscating the parameter.
         * @return This object.
         * @throws NullPointerException If the given parameter name or obfuscator is {@code null}.
         * @throws IllegalArgumentException If a parameter with the same name and the same case sensitivity was already added.
         */
        public Builder withParameter(String parameter, Obfuscator obfuscator) {
            obfuscators.withEntry(parameter, obfuscator);
            return this;
        }

        /**
         * Adds a parameter to obfuscate.
         *
         * @param parameter The name of the parameter.
         * @param obfuscator The obfuscator to use for obfuscating the parameter.
         * @param caseSensitivity The case sensitivity for the parameter name.
         * @return This object.
         * @throws NullPointerException If the given parameter name, obfuscator or case sensitivity is {@code null}.
         * @throws IllegalArgumentException If a parameter with the same name and the same case sensitivity was already added.
         */
        public Builder withParameter(String parameter, Obfuscator obfuscator, CaseSensitivity caseSensitivity) {
            obfuscators.withEntry(parameter, obfuscator, caseSensitivity);
            return this;
        }

        /**
         * Sets the default case sensitivity for new parameters to {@link CaseSensitivity#CASE_SENSITIVE}. This is the default setting.
         * <p>
         * Note that this will not change the case sensitivity of any parameter that was already added.
         *
         * @return This object.
         */
        public Builder caseSensitiveByDefault() {
            obfuscators.caseSensitiveByDefault();
            return this;
        }

        /**
         * Sets the default case sensitivity for new parameters to {@link CaseSensitivity#CASE_INSENSITIVE}.
         * <p>
         * Note that this will not change the case sensitivity of any parameter that was already added.
         *
         * @return This object.
         */
        public Builder caseInsensitiveByDefault() {
            obfuscators.caseInsensitiveByDefault();
            return this;
        }

        /**
         * Sets the encoding to use. The default is {@link StandardCharsets#UTF_8}.
         *
         * @param encoding The encoding.
         * @return This object.
         * @throws NullPointerException If the given encoding mode is {@code null}.
         */
        public Builder withEncoding(Charset encoding) {
            this.encoding = Objects.requireNonNull(encoding);
            return this;
        }

        /**
         * This method allows the application of a function to this builder.
         * <p>
         * Any exception thrown by the function will be propagated to the caller.
         *
         * @param <R> The type of the result of the function.
         * @param f The function to apply.
         * @return The result of applying the function to this builder.
         */
        public <R> R transform(Function<? super Builder, ? extends R> f) {
            return f.apply(this);
        }

        private Map<String, Obfuscator> obfuscators() {
            return obfuscators.build();
        }

        /**
         * Creates a new {@code RequestParameterObfuscator} with the properties and obfuscators added to this builder.
         *
         * @return The created {@code RequestParameterObfuscator}.
         */
        public RequestParameterObfuscator build() {
            return new RequestParameterObfuscator(this);
        }
    }
}
