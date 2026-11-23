# Changelog

## [3.0.0](https://github.com/emirhannaneli/service-utils/compare/service-utils-v2.1.0...service-utils-v3.0.0) (2026-11-23)


### ⚠ BREAKING CHANGES

* RequestTool header accessors (userAgent, referer, accept, ...) return String? instead of throwing when the header is absent. snakeyaml, joda-time, aspectjrt, jackson-module-afterburner, kotlinx-coroutines-core and kotlin-script-runtime are no longer transitive dependencies. HTTP 500 responses carry no details unless lubble.expose-exception-details is enabled. page sizes above lubble.pagination.max-size (default 1000) are reduced to it. Elasticsearch criteria queries without a field list return only id, pk, sk, createdAt and updatedAt. search terms are matched literally (LIKE wildcards and regex metacharacters are no longer interpreted), and toCacheKey() produces different keys.

### Features

* upgrade toolchain and dependencies, harden security and fix correctness bugs ([651b2a2](https://github.com/emirhannaneli/service-utils/commit/651b2a201cd357f61588c26c8cfde6f8a2c9c764))


### Documentation

* document installation, configuration and the release flow ([48ebdf8](https://github.com/emirhannaneli/service-utils/commit/48ebdf80789bb9f24daab507e1beb63fdeb9566c))
