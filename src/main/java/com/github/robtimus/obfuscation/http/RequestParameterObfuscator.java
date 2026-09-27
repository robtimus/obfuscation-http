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

import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.appendAtMost;
import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.checkStartAndEnd;
import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.counting;
import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.discardAll;
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
import java.util.function.Consumer;
import java.util.function.Function;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.support.CachingObfuscatingWriter;
import com.github.robtimus.obfuscation.support.CaseSensitivity;
import com.github.robtimus.obfuscation.support.CountingReader;
import com.github.robtimus.obfuscation.support.LimitAppendable;
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

    private final Map<String, Obfuscator> parameters;
    private final String parametersRepresentation;
    private final Charset encoding;

    private final long limit;
    private final String truncatedIndicator;

    private RequestParameterObfuscator(Builder builder) {
        parameters = builder.parameters();
        parametersRepresentation = builder.parametersRepresentation();
        encoding = builder.encoding;

        limit = builder.limit;
        truncatedIndicator = builder.truncatedIndicator;
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

        LimitAppendable appendable = appendAtMost(destination, limit);
        int count = end - start;

        int index;
        while ((index = indexOf(s, '&', start, end)) != -1 && !appendable.limitExceeded()) {
            maskKeyValue(s, start, index, appendable);
            appendable.append('&');
            start = index + 1;
        }
        if (!appendable.limitExceeded()) {
            // remainder
            maskKeyValue(s, start, end, appendable);
        }
        if (appendable.limitExceeded() && truncatedIndicator != null) {
            destination.append(String.format(truncatedIndicator, count));
        }
    }

    @Override
    public void obfuscateText(Reader input, Appendable destination) throws IOException {
        CountingReader reader = counting(input);
        @SuppressWarnings("resource")
        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        LimitAppendable appendable = appendAtMost(destination, limit);
        int c;
        while ((c = br.read()) != -1 && !appendable.limitExceeded()) {
            if (c == '&') {
                maskKeyValue(sb, 0, sb.length(), appendable);
                sb.delete(0, sb.length());
                appendable.append('&');
            } else {
                sb.append((char) c);
            }
        }
        if (appendable.limitExceeded()) {
            discardAll(br);
        } else {
            // remainder
            maskKeyValue(sb, 0, sb.length(), appendable);
        }
        if (appendable.limitExceeded() && truncatedIndicator != null) {
            destination.append(String.format(truncatedIndicator, reader.count()));
        }
    }

    private void maskKeyValue(CharSequence s, int start, int end, Appendable destination) throws IOException {
        int index = indexOf(s, '=', start, end);
        if (index == -1) {
            // no value so nothing to mask
            destination.append(s, start, end);
        } else {
            String name = URLDecoder.decode(s.subSequence(start, index).toString(), encoding.name());
            Obfuscator obfuscator = parameters.get(name);
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
        return parameters.getOrDefault(name, none());
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
        return parameters.equals(other.parameters)
                && encoding.equals(other.encoding)
                && limit == other.limit
                && Objects.equals(truncatedIndicator, other.truncatedIndicator);
    }

    @Override
    public int hashCode() {
        return parameters.hashCode() ^ encoding.hashCode() ^ Long.hashCode(limit) ^ Objects.hashCode(truncatedIndicator);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getName()
                + "[parameters=" + parametersRepresentation
                + ",encoding=" + encoding
                + ",limit=" + limit
                + ",truncatedIndicator=" + truncatedIndicator
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

        private final MapBuilder<Obfuscator> parameters;
        private final StringBuilder parametersRepresentation;

        private CaseSensitivity defaultCaseSensitivity;

        private Charset encoding;

        private long limit;
        private String truncatedIndicator;

        private final ParameterConfigurer parameterConfigurer;
        private final LimitConfigurer limitConfigurer;

        private Builder() {
            parameters = new MapBuilder<>();
            parametersRepresentation = new StringBuilder().append('{');

            defaultCaseSensitivity = CaseSensitivity.CASE_SENSITIVE;

            encoding = StandardCharsets.UTF_8;

            limit = Long.MAX_VALUE;
            truncatedIndicator = "... (total: %d)"; //$NON-NLS-1$

            parameterConfigurer = new ParameterConfigurer();
            limitConfigurer = new LimitConfigurer();
        }

        /**
         * Adds a parameter to obfuscate.
         * This method is equivalent to calling for {@link #withParameter(String, Obfuscator, Consumer)} with a {@link Consumer} that does nothing.
         *
         * @param parameter The name of the parameter.
         * @param obfuscator The obfuscator to use for obfuscating the parameter.
         * @return This object.
         * @throws NullPointerException If the given parameter name or obfuscator is {@code null}.
         * @throws IllegalArgumentException If a parameter with the same name and the same case sensitivity was already added.
         */
        public Builder withParameter(String parameter, Obfuscator obfuscator) {
            addParameter(parameter, obfuscator, null);
            return this;
        }

        /**
         * Adds a parameter to obfuscate.
         * This parameter will use the defaults set using {@link #caseSensitiveByDefault()} and {@link #caseInsensitiveByDefault()},
         * unless explicitly replaced by the given {@link Consumer}.
         *
         * @param parameter The name of the parameter.
         * @param obfuscator The obfuscator to use for obfuscating the parameter.
         * @param configurer A {@link Consumer} that can be used to update its argument, to override any setting for the parameter.
         * @return This object.
         * @throws NullPointerException If the given parameter name, obfuscator or {@link Consumer} is {@code null}.
         * @throws IllegalArgumentException If a parameter with the same name and the same case sensitivity was already added.
         * @since 2.0
         */
        public Builder withParameter(String parameter, Obfuscator obfuscator, Consumer<ParameterConfigurer> configurer) {
            Objects.requireNonNull(configurer);
            addParameter(parameter, obfuscator, configurer);
            return this;
        }

        private void addParameter(String parameter, Obfuscator obfuscator, Consumer<ParameterConfigurer> configurer) {
            Objects.requireNonNull(parameter);
            Objects.requireNonNull(obfuscator);
            try {
                parameterConfigurer.caseSensitivity = defaultCaseSensitivity;
                if (configurer != null) {
                    configurer.accept(parameterConfigurer);
                }

                parameters.withEntry(parameter, obfuscator, parameterConfigurer.caseSensitivity);

                addParameterRepresenation(parameter, obfuscator);
            } finally {
                parameterConfigurer.reset();
            }
        }

        @SuppressWarnings("nls")
        private void addParameterRepresenation(String parameter, Obfuscator obfuscator) {
            if (parametersRepresentation.length() > 1) {
                parametersRepresentation.append(", ");
            }
            parametersRepresentation.append(parameter).append("=[");
            if (parameterConfigurer.caseSensitivity == CaseSensitivity.CASE_INSENSITIVE) {
                parametersRepresentation.append("caseInsensitive,");
            }
            parametersRepresentation.append("obfuscator=").append(obfuscator);
            parametersRepresentation.append("]");
        }

        /**
         * Sets the default case sensitivity for new parameters to {@link CaseSensitivity#CASE_SENSITIVE}. This is the default setting.
         * <p>
         * Note that this will not change the case sensitivity of any parameter that was already added.
         *
         * @return This object.
         */
        public Builder caseSensitiveByDefault() {
            defaultCaseSensitivity = CaseSensitivity.CASE_SENSITIVE;
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
            defaultCaseSensitivity = CaseSensitivity.CASE_INSENSITIVE;
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
         * Sets the limit for the obfuscated result.
         * Note that this limit only applies when obfuscating full request parameter texts, not when obfuscating single parameters using one of the
         * {@code obfuscateParameter} methods.
         *
         * @param limit The limit to use.
         * @return This object.
         * @throws IllegalArgumentException If the given limit is negative.
         * @since 1.1
         */
        public Builder limitTo(long limit) {
            setLimit(limit, null);
            return this;
        }

        /**
         * Sets the limit for the obfuscated result.
         * Note that this limit only applies when obfuscating full request parameter texts, not when obfuscating single parameters using one of the
         * {@code obfuscateParameter} methods.
         *
         * @param limit The limit to use.
         * @param configurer A {@link Consumer} that can be used to update its argument, to set any limit-specific properties.
         * @return This object.
         * @throws IllegalArgumentException If the given limit is negative.
         * @since 2.0
         */
        public Builder limitTo(long limit, Consumer<LimitConfigurer> configurer) {
            Objects.requireNonNull(configurer);
            setLimit(limit, configurer);
            return this;
        }

        private void setLimit(long limit, Consumer<LimitConfigurer> configurer) {
            if (limit < 0) {
                throw new IllegalArgumentException(limit + " < 0"); //$NON-NLS-1$
            }
            try {
                limitConfigurer.truncatedIndicator = truncatedIndicator;
                if (configurer != null) {
                    configurer.accept(limitConfigurer);
                }

                this.limit = limit;
                this.truncatedIndicator = limitConfigurer.truncatedIndicator;
            } finally {
                limitConfigurer.reset();
            }
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

        private Map<String, Obfuscator> parameters() {
            return parameters.build();
        }

        private String parametersRepresentation() {
            parametersRepresentation.append('}');
            String result = parametersRepresentation.toString();
            parametersRepresentation.deleteCharAt(parametersRepresentation.length() - 1);
            return result;
        }

        /**
         * Creates a new {@code RequestParameterObfuscator} with the parameters and obfuscators added to this builder.
         *
         * @return The created {@code RequestParameterObfuscator}.
         */
        public RequestParameterObfuscator build() {
            return new RequestParameterObfuscator(this);
        }
    }

    /**
     * An object that can be used to configure a parameter that should be obfuscated.
     *
     * @author Rob Spoor
     * @since 2.0
     */
    public static final class ParameterConfigurer {

        private CaseSensitivity caseSensitivity;

        private ParameterConfigurer() {
        }

        /**
         * Sets the case sensitivity for the parameter to {@link CaseSensitivity#CASE_SENSITIVE}.
         *
         * @return This object.
         */
        public ParameterConfigurer caseSensitive() {
            caseSensitivity = CaseSensitivity.CASE_SENSITIVE;
            return this;
        }

        /**
         * Sets the case sensitivity for the parameter to {@link CaseSensitivity#CASE_INSENSITIVE}.
         *
         * @return This object.
         */
        public ParameterConfigurer caseInsensitive() {
            caseSensitivity = CaseSensitivity.CASE_INSENSITIVE;
            return this;
        }

        private void reset() {
            caseSensitivity = null;
        }
    }

    /**
     * An object that can be used to configure handling when the obfuscated result exceeds a pre-defined limit.
     *
     * @author Rob Spoor
     * @since 1.1
     */
    public static final class LimitConfigurer {

        private String truncatedIndicator;

        private LimitConfigurer() {
        }

        /**
         * Sets the indicator to use when the obfuscated result is truncated due to the limit being exceeded.
         * There can be one place holder for the total number of characters. Defaults to {@code ... (total: %d)}.
         * Use {@code null} to omit the indicator.
         *
         * @param pattern The pattern to use as indicator.
         * @return This object.
         */
        public LimitConfigurer withTruncatedIndicator(String pattern) {
            this.truncatedIndicator = pattern;
            return this;
        }

        private void reset() {
            this.truncatedIndicator = null;
        }
    }
}
