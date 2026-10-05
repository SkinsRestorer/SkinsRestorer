# Contribute to SkinsRestorer

Contributions can fix behavior, improve documentation, or add focused tests.
Use the [installation guide](https://skinsrestorer.net/docs/installation) and [support Discord](https://skinsrestorer.net/discord) for setup questions.

## Before you start

Read [the support guide](SUPPORT.md) for questions and issue routing.
Search existing issues and pull requests. Discuss larger API, architecture, or dependency changes before implementation.

Work from `dev` and target that branch in your pull request.
Keep each change focused. Avoid unrelated formatting and dependency updates.

## Prepare a checkout

Install JDK 25. Use the Gradle wrapper. Docker is required for tests that use Testcontainers. The default development branch is `dev`.

Run the commands below from the repository root unless a command names another directory.
On Windows, use `gradlew.bat` in place of `./gradlew` for Gradle commands.

## Repository layout

- `api/`, `shared/`: public API and shared behavior.
- `bukkit/`, `bungee/`, `velocity/`, `mod/`: platform integrations.
- `test/`: shared test infrastructure.
- `universal/`: combined plugin artifact.
- `buildSrc/`: compiler, formatting, and analysis conventions.

## Verify your change

```bash
./gradlew test spotlessCheck
./gradlew build
```

Read [AGENTS.md](AGENTS.md) for Java, Lombok, and test conventions. Preserve skin storage, proxy/backend behavior, permissions, and public API compatibility. Use the existing test extension for focused tests. For runtime fixes, verify skin apply, clear, reconnect, and backend switching on the affected platforms. Update release documentation when behavior changes.

Run the relevant checks before review. State the command and result in the pull request.
If a check cannot run, explain the missing dependency or service. Do not claim it passed.
Keep generated artifacts consistent with their source and review their diff.

## Style and documentation

Follow the existing code conventions and repository formatter. Keep commit hooks enabled.
Add focused tests for changed logic when practical. Avoid tests that only assert source strings.
Update documentation when commands, APIs, configuration, or expected behavior change.
Keep examples small and reproducible. Preserve exact identifiers, commands, and error messages.

## Open a pull request

Explain the problem and resulting behavior. Link related issues without a placeholder issue number.
Target `dev`. Identify affected platforms and tests. Explain any public API or storage migration.
Include commands and results. State any runtime checks that remain necessary.
Respond to review with a correction or concrete evidence.

Use Conventional Commits: `type(scope): description`, for example `docs(contributing): explain local validation`.
Use a meaningful scope, or omit it. Keep the subject concise and imperative.
Add a body when the reason or compatibility impact is not obvious.

For vulnerabilities, follow [the security reporting instructions](SECURITY.md).
Remove credentials and private data from examples, logs, and screenshots.
