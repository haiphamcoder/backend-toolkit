# Contributing to backend-toolkit

Thanks for your interest in contributing to **backend-toolkit**! This guide explains how to report bugs, propose features, open pull requests, and meet our technical standards.

> TL;DR: Open a clear issue → branch from `main` → use **Conventional Commits** → ensure build/tests pass → open a focused PR with tests & docs.

---

## Table of Contents

* [Code of Conduct](#code-of-conduct)
* [Questions & Bug Reports](#questions--bug-reports)
* [Scope & Modules](#scope--modules)
* [Environment Requirements](#environment-requirements)
* [Project Structure](#project-structure)
* [Development Workflow](#development-workflow)
* [Commit & Branch Standards](#commit--branch-standards)
* [Code Style](#code-style)
* [Testing & Quality](#testing--quality)
* [API & Backward Compatibility](#api--backward-compatibility)
* [Docs & Examples](#docs--examples)
* [Pull Request Checklist](#pull-request-checklist)
* [License & Copyright](#license--copyright)

---

## Code of Conduct

We expect an inclusive, respectful, and collaborative environment. Be kind, stay constructive, and do not share secrets or sensitive data. (If the repo has a `CODE_OF_CONDUCT.md`, all contributors are expected to follow it.)

---

## Questions & Bug Reports

* **Bug report**: use `.github/ISSUE_TEMPLATE/bug_report.md`. Provide a **Minimal, Complete, Verifiable Example** (MCVE).
* **Feature request**: use `.github/ISSUE_TEMPLATE/feature_request.md`. Describe the **problem**, **proposed API**, and **alternatives**.
* **General discussion**: open a Q&A issue or discuss within a related PR.

> Please search for existing issues before creating a new one.

---

## Scope & Modules

* `backend-toolkit-core`: plain Java utilities (ID generation, response model, exception model, validation helpers, time/collections, etc.).
* `backend-toolkit-spring`: Spring Boot auto-configuration, `@ControllerAdvice`, `ResponseBodyAdvice`, validation adapters, (optional) Jackson integrations.

**Do not** add heavy dependencies to `core`. Framework-specific integrations belong in `spring` or optional modules.

---

## Environment Requirements

* **JDK**: 17 (LTS)
* **Maven**: 3.9.5+
* OS: Linux/macOS/Windows

Quick check:

```bash
java -version
mvn -v
```

---

## Project Structure

```text
backend-toolkit/
├─ pom.xml                      # parent (packaging=pom)
├─ backend-toolkit-bom/         # BOM (version alignment)
├─ backend-toolkit-core/        # Java utilities (no Spring deps)
└─ backend-toolkit-spring/      # Spring Boot autoconfig & web helpers
```

---

## Development Workflow

### 1) Fork & clone

```bash
git clone https://github.com/haiphamcoder/backend-toolkit.git
cd backend-toolkit
```

### 2) Create a branch from `main`

```bash
git checkout -b feat/<short-name>
# e.g., feat/ulid-generator or fix/validation-npe
```

### 3) Build & test locally

```bash
# build all modules
mvn -q -DskipTests=false verify

# force dependency updates if needed
mvn -U -q verify
```

### 4) Selective builds (when needed)

```bash
# build only core + spring (skip examples if present)
mvn -q -pl '!backend-toolkit-examples' -am verify
```

### 5) Push & open a Pull Request

* Keep PRs small and focused on **one** topic.
* Link to related issues and include testing instructions.

---

## Commit & Branch Standards

### Branch naming

* `feat/<summary>`: new feature
* `fix/<summary>`: bug fix
* `chore/<summary>`: tooling/build/scripts
* `docs/<summary>`: documentation
* `test/<summary>`: tests only
* `refactor/<summary>`: internal refactor (no behavior change)

### Conventional Commits (examples)

* `feat(core): add PageResponse with total elements`
* `fix(spring): handle MethodArgumentNotValidException null messages`
* `docs: update README with configuration example`
* `refactor(core): simplify DateTimes truncation`
* `test(spring): add advice wrapping opt-out tests`
* `chore(ci): upload jacoco report artifact`

Multiple small commits in a PR are fine; we can squash on merge.

---

## Code Style

### General Java

* Target **Java 21** (or 17). Use `var` judiciously.
* Prefer **immutability** (`final` classes/fields where appropriate).
* Minimize public API surface (use package-private when possible).
* Avoid catching/throwing overly broad exceptions. Use `BaseException`/`ErrorCode`.

### Nullability & Optional

* Avoid returning `null`. Use `Optional` at boundaries; validate inputs early with clear messages.

### Logging

* Use SLF4J. No `System.out`.
* Don’t log secrets/tokens. Avoid duplicate stack traces.

### Dependencies

* `core`: no heavy/framework dependencies. If JSON support is needed, provide an **SPI**.
* `spring`: depend on `spring-boot-autoconfigure`, `spring-web`, `spring-boot-starter-validation`, etc.

---

## Testing & Quality

### Minimum expectations

* **Unit tests** for new logic / bug fixes.
* Reasonable coverage for core logic. Aim for meaningful tests over % metrics.
* Clear test naming and **AAA** structure (Arrange–Act–Assert).

### Commands

```bash
mvn -q test
# full pipeline:
mvn -q verify
```

### Spring tests

* Prefer **slice** tests (`@WebMvcTest`) when possible for speed and stability; use `@SpringBootTest` only when necessary.

### Static analysis (optional)

* We may adopt Spotless/Checkstyle/ErrorProne later. For now, follow the style rules above.

---

## API & Backward Compatibility

* We follow **Semantic Versioning**:

  * **MAJOR**: breaking changes
  * **MINOR**: new features, backward compatible
  * **PATCH**: bug fixes
* Avoid breaking changes in `0.x` if possible. If unavoidable, document clearly in the PR.
* **Deprecation policy**:

  * Use `@Deprecated` with a clear replacement and removal plan.
  * Do not remove deprecated APIs in the same minor release (unless exceptional).

---

## Docs & Examples

* Provide Javadoc for public APIs (`core` & `spring`).
* Update `README.md` when adding features or changing defaults.
* If default behavior changes (e.g., `wrap-enabled`), update configuration examples (`application.yml`).

---

## Pull Request Checklist

* [ ] Issue created or problem clearly described.
* [ ] PR is small and focused on a single topic.
* [ ] Uses **Conventional Commits**.
* [ ] Tests added/updated and passing locally/CI.
* [ ] Javadoc/docs updated if public API changes.
* [ ] No heavy dependencies added to `core`.
* [ ] Build passes: `mvn -q verify` (CI green).

---

## License & Copyright

By contributing, you agree that your contributions are licensed under the project’s license (e.g., **Apache-2.0** if that’s the repo’s `LICENSE`).

---

### Thank you

Every contribution—whether a small fix, docs improvement, or a larger feature—helps make **backend-toolkit** better 💚.
