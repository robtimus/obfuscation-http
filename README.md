# obfuscation-http

Provides functionality for obfuscating HTTP requests and responses. This can be useful for logging such requests/responses, where sensitive content should not be logged as-is.

## Obfuscating request parameters

To create a request parameter obfuscator, simply create a builder, add parameters to it, and let it build the final obfuscator:

    Obfuscator obfuscator = RequestParameterObfuscator.builder()
            .withParameter("password", Obfuscator.fixedLength(3))
            .build();

## Obfuscating headers

To create a header obfuscator, simply create a builder, add headers to it, and let it build the final obfuscator:

    HeaderObfuscator obfuscator = HeaderObfuscator.builder()
            .withHeader("authorization", Obfuscator.fixedLength(3))
            .build();
