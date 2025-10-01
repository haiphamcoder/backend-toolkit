---
name: "🐞 Bug report"
about: Report a problem with backend-toolkit (core/spring)
title: "[Bug]: <short summary>"
labels: ["bug", "needs-triage"]
assignees: ""
---

<!--
Thanks for taking the time to file a bug! Please fill out *all* required sections.
Do not include secrets, access tokens, or proprietary data. Redact anything sensitive.
-->

## ✅ Pre-checklist

- [ ] I searched existing issues and didn’t find a duplicate.
- [ ] I’m using the latest released version of `backend-toolkit` (or I can reproduce the bug there).
- [ ] I can provide a **minimal reproducible example** (MCVE), ideally a small repo or gist.

---

## 🧩 Affected module(s)

- [ ] `backend-toolkit-core`
- [ ] `backend-toolkit-spring`
- [ ] Other (please specify):

---

## 🐛 Bug description

A clear and concise description of what the bug is.

**What were you trying to do? What happened instead?**

---

## 🔁 Steps to reproduce

1. …
2. …
3. …

> Minimal code/config that reproduces the issue:

```java
// Java snippet (short, self-contained)
````

```yaml
# application.yml (if relevant)
```

```xml
<!-- pom.xml (relevant parts only) -->
```

> If possible, link to a tiny public repo that reproduces the problem:

- Repro repo: <link>

---

## 🤔 Expected behavior

What you expected to happen.

---

## 😵 Actual behavior

What actually happened (include exact error messages).

---

## 🧾 Logs / Stack trace

<details>
<summary>Click to expand</summary>

```text
<copy the full stack trace here>
```

</details>

---

## 💻 Environment

- `backend-toolkit` version: `x.y.z`
- Java version / vendor: `e.g. 21 (Temurin)`
- Spring Boot version: `e.g. 3.3.4`
- Build tool: `Maven x.y.z`
- OS / Arch: `e.g. Ubuntu 24.04 / x86_64`
- Using Lombok: `Yes/No`
- Runtime: `JVM / Docker / Kubernetes / GraalVM native-image (version?)`

---

## ⏪ Regression?

- Did this work in a previous version? **Yes/No**
- If yes, the last known good version: `x.y.z`

---

## 🛠️ Workarounds tried

- List any temporary fixes or mitigations you discovered.

---

## 📎 Additional context / Screenshots

- Add any other context, screenshots, or references that might help.

---

## 🙋 Willing to contribute?

- [ ] I’m interested in submitting a PR to fix this.
- [ ] I can help test candidate fixes.

---

### Notes for maintainers (optional)

Anything else we should know (e.g., suspected root cause, related issues/PRs).
