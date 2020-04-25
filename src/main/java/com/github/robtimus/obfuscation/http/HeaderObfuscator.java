/*
 * HeaderObfuscator.java
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

import static com.github.robtimus.obfuscation.Obfuscator.none;
import static com.github.robtimus.obfuscation.support.CaseSensitivity.CASE_INSENSITIVE;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.github.robtimus.obfuscation.Obfuscated;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.support.MapBuilder;

/**
 * An object that will obfuscate header values.
 *
 * @author Rob Spoor
 */
public final class HeaderObfuscator {

    private final Map<String, Obfuscator> obfuscators;

    private HeaderObfuscator(Builder builder) {
        obfuscators = builder.obfuscators();
    }

    /**
     * Obfuscates the value of a header.
     *
     * @param name The name of the header to obfuscate.
     * @param value The header value to obfuscate.
     * @return The obfuscated header value.
     * @throws NullPointerException If the given name or value is {@code null}.
     */
    public CharSequence obfuscateHeader(String name, String value) {
        return obfuscator(name).obfuscateText(value);
    }

    /**
     * Obfuscates the value of a header.
     *
     * @param name The name of the header to obfuscate.
     * @param value The header value to obfuscate.
     * @param destination The {@code StringBuilder} to append the obfuscated header value to.
     * @throws NullPointerException If the given name, value or {@code StringBuilder} is {@code null}.
     */
    public void obfuscateHeader(String name, String value, StringBuilder destination) {
        obfuscator(name).obfuscateText(value, destination);
    }

    /**
     * Obfuscates the value of a header.
     *
     * @param name The name of the header to obfuscate.
     * @param value The header value to obfuscate.
     * @param destination The {@code StringBuffer} to append the obfuscated header value to.
     * @throws NullPointerException If the given name, value or {@code StringBuffer} is {@code null}.
     */
    public void obfuscateHeader(String name, String value, StringBuffer destination) {
        obfuscator(name).obfuscateText(value, destination);
    }

    /**
     * Obfuscates the value of a header.
     *
     * @param name The name of the header to obfuscate.
     * @param value The header value to obfuscate.
     * @param destination The {@code Appendable} to append the obfuscated header value to.
     * @throws NullPointerException If the given name, value or {@code Appendable} is {@code null}.
     * @throws IOException If an I/O error occurs.
     */
    public void obfuscateHeader(String name, String value, Appendable destination) throws IOException {
        obfuscator(name).obfuscateText(value, destination);
    }

    /**
     * Obfuscates the value of a header.
     *
     * @param name The name of the header to obfuscate.
     * @param value The header value to obfuscate.
     * @return An {@code Obfuscated} wrapper around the given value.
     * @throws NullPointerException If the given name or value is {@code null}.
     */
    public Obfuscated<String> obfuscateHeaderValue(String name, String value) {
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
        HeaderObfuscator other = (HeaderObfuscator) o;
        return obfuscators.equals(other.obfuscators);
    }

    @Override
    public int hashCode() {
        return obfuscators.hashCode();
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getName()
                + "[obfuscators=" + obfuscators
                + "]";
    }

    /**
     * Returns a builder that will create {@code HeaderObfuscators}.
     *
     * @return A builder that will create {@code HeaderObfuscators}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link HeaderObfuscator HeaderObfuscatorss}.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private final MapBuilder<Obfuscator> obfuscators;

        private Builder() {
            obfuscators = new MapBuilder<>();
        }

        /**
         * Adds a header to obfuscate.
         *
         * @param header The name of the header.
         * @param obfuscator The obfuscator to use for obfuscating the header.
         * @return This object.
         * @throws NullPointerException If the given header name or obfuscator is {@code null}.
         * @throws IllegalArgumentException If a header with the same name (case insensitive) was already added.
         */
        public Builder withHeader(String header, Obfuscator obfuscator) {
            obfuscators.withEntry(header, obfuscator, CASE_INSENSITIVE);
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
         * Creates a new {@code HeaderObfuscator} with the obfuscators added to this builder.
         *
         * @return The created {@code HeaderObfuscator}.
         */
        public HeaderObfuscator build() {
            return new HeaderObfuscator(this);
        }
    }
}
