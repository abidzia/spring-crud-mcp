# CI Foundation Plan

Goal: establish a **CI safety net and branch protection** before feature work, so every
later change (especially the security phases in `SECURITY.md`) lands gated and reproducible.
This is **Phase 0** work from the security roadmap.

> Branch: `chore/ci`. Scope is CI/config only — no application code changes.

## Why this first

Every subsequent branch (security phases, tests, features) is safer once the build, a
vulnerability scanner, dependency updates, and secret scanning all run automatically on
each PR. It also removes the "needs Java + Maven locally" barrier for contributors.

## What we're adding

| Component | Tool | File | Runs on |
|-----------|------|------|---------|
| Build & test | GitHub Actions + `mvn verify` (reactor) | `.github/workflows/ci.yml` | push to `main`, all PRs |
| Static analysis (SAST) | Semgrep (community rules) | `.github/workflows/semgrep.yml` | push to `main`, all PRs |
| Static analysis (SAST) | CodeQL (java-kotlin) | `.github/workflows/codeql.yml` | push to `main`, PRs, weekly |
| Dependency updates (SCA) | Dependabot (maven + github-actions) | `.github/dependabot.yml` | weekly |
| Secret scanning | gitleaks | `.github/workflows/gitleaks.yml` | push, PRs |

> **Two SAST layers:** the repo is public, so **CodeQL** (GitHub-native, deep dataflow,
> results in Security → Code scanning) runs alongside **Semgrep** (fast, pattern-based, gates
> the build). They complement each other. CodeQL code scanning is free on public repos; on a
> private repo it would need GitHub Advanced Security, in which case drop `codeql.yml` and
> keep Semgrep. Dependabot and the gitleaks action are free either way.

## Design notes

- **Build:** `mvn -B -ntp verify` at the repo root builds all three modules via the reactor
  and runs whatever tests exist (none yet — the job still validates compile + package).
  `actions/setup-java` with `cache: maven` keeps CI builds fast.
- **Semgrep:** runs in the official `semgrep/semgrep` container with community rulesets
  (`p/java`, `p/security-audit`) and `--error`, so any finding fails the check. No login or
  GitHub Advanced Security required. (A Java-native alternative for later: SpotBugs +
  FindSecBugs via a Maven plugin, which would move SAST into `mvn verify`.)
- **Dependabot:** two ecosystems — `maven` (root reactor) and `github-actions` (keeps the
  workflow actions themselves current). Weekly PRs.
- **gitleaks:** full-history scan (`fetch-depth: 0`) on push/PR. Free for public repos.

## Branch protection (manual — GitHub setting, not a file)

After these workflows have run once on `main`, enable in
**Settings → Branches → Branch protection rules** for `main`:

- Require a pull request before merging.
- Require status checks to pass: **CI**, **Semgrep**, **CodeQL**, **gitleaks**.
- Require branches to be up to date before merging.
- (Recommended) Require conversation resolution; restrict force-pushes.

*This step can't be committed as a file — it's configured in the repo settings by an admin
(or via `gh api`).* 

## Definition of done

- [ ] `ci.yml`, `semgrep.yml`, `codeql.yml`, `gitleaks.yml`, `dependabot.yml` committed and green on a PR.
- [ ] Branch protection on `main` requires the three checks + PR review.
- [ ] `SECURITY.md` Phase 0 CI items checked off.

## Out of scope (later)

- Actual unit/slice tests (`@WebMvcTest`, MCP tool slice) — a separate `feature/tests` branch;
  CI will start enforcing them automatically once they exist.
- SBOM generation (CycloneDX), container image scanning (Trivy), DAST — later security phases.
