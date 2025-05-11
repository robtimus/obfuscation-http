# obfuscation-http
[![Maven Central](https://img.shields.io/maven-central/v/com.github.robtimus/obfuscation-http)](https://search.maven.org/artifact/com.github.robtimus/obfuscation-http)
[![Build Status](https://github.com/robtimus/obfuscation-http/actions/workflows/build.yml/badge.svg)](https://github.com/robtimus/obfuscation-http/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Aobfuscation-http&metric=alert_status)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Aobfuscation-http)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Aobfuscation-http&metric=coverage)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Aobfuscation-http)
[![Known Vulnerabilities](https://snyk.io/test/github/robtimus/obfuscation-http/badge.svg)](https://snyk.io/test/github/robtimus/obfuscation-http)

Provides functionality for obfuscating HTTP requests and responses. This can be useful for logging such requests/responses, where sensitive content should not be logged as-is.

## Obfuscating request parameters

To create a request parameter obfuscator, simply create a builder, add parameters to it, and let it build the final obfuscator:

```java
RequestParameterObfuscator obfuscator = RequestParameterObfuscator.builder()
        .withParameter("password", Obfuscator.fixedLength(3))
        .build();
```

## Obfuscating headers

To create a header obfuscator, simply create a builder, add headers to it, and let it build the final obfuscator:

```java
HeaderObfuscator obfuscator = HeaderObfuscator.builder()
        .withHeader("authorization", Obfuscator.fixedLength(3))
        .build();
```
