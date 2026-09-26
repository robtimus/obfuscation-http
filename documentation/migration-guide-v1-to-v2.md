# Migrating from version 1.x to 2.0

## RequestParameterObfuscator.Builder

`RequestParameterObfuscator.Builder` is no longer an interface but instead a final class. If you are creating mocks or implementing it directly you need to use actual instances created through `RequestParameterObfuscator.builder()`.

### withParameter case sensitivity

`RequestParameterObfuscator.Builder.withParameter` no longer accepts a `CaseSensitivity` argument. You need to provide a `Consumer<ParameterConfigurer>` instead, and call methods `caseSensitive()` and `caseInsensitive()` on the `ParameterConfigurer` instead. For example:

```java
/* old:
RequestParameterObfuscator.builder()
        .withParameter("foo", obfuscator, CaseSensitivity.CASE_INSENSITIVE)
 */
RequestParameterObfuscator.builder()
        .withParameter("foo", obfuscator, ParameterConfigurer::caseInsensitive)
```

### limitTo

`RequestParameterObfuscator.Builder.limitTo` no longer returns a `LimitConfigurer`. Instead it is overloaded to take a `Consumer<LimitConfigurer>`. If you called any `LimitConfigurer` methods you need to provide a lambda instead. For example:

```java
/* old:
RequestParameterObfuscator.builder()
        .limitTo(1024)
                .withTruncatedIndicator("<truncated>")
 */
RequestParameterObfuscator.builder()
        .limitTo(1024, limit -> limit
                .withTruncatedIndicator("<truncated>"))
```

## RequestParameterObfuscator.LimitConfigurer

`RequestParameterObfuscator.LimitConfigurer` is no longer an interface but instead a final class. If you are creating mocks or implementing it directly you need to use actual instances passed to the `Consumer` argument of `RequestParameterObfuscator.Builder.limitTo`.
