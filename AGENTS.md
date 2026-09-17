# Repository Guidelines

Java library providing pooled SSH/SFTP session management, built on the mwiede JSch fork and Apache commons-pool2.

## Project Structure & Module Organization

```
src/main/java/com/j2s/secure/     Library source
  ssh/                            Sync/async SSH sessions, session pool, factories
  sftp/                           SFTP sessions, manager, factory
  executors/                      Thread pool helpers (SecureExecutors)
  ex/ (ssh/ex, sftp/ex)           Exception hierarchy per protocol
  SSHPoolMain.java                CLI entry point (manual smoke test)
  SSHSessionConfig.java, SSHSessionKeyedConfig.java, SessionTimer.java, SSHAsyncMessage.java
src/test/java/com/j2s/secure/     JUnit 5 tests
META-INF/MANIFEST.MF              JAR manifest (Main-Class)
uml.md, uml2.md                   Mermaid state/class diagrams of the pool lifecycle
pom.xml                           Maven build (groupId com.j2s, artifactId secure-client)
```

## Build Environment

- Requires JDK 8+ and Maven 3.x. Code targets Java 8 (`maven.compiler.source/target`) — no `var`, no `List.of`, UTF-8, 4-space indentation, no tabs.
- Do not commit `target/` or `*.jar` (gitignored).

## Build, Test, and Development Commands

- `mvn clean package` — compile and build the JAR into `target/`.
- `mvn deploy` — publish to the internal repository (`file:./target/deploy`, local path, not Maven Central).
- `mvn test` — run tests. **Note:** `maven.test.skip` is `true` by default in `pom.xml`; override with `mvn test -Dmaven.test.skip=false`.
- `java -jar target/secure-client-<version>.jar <host> <id> <pwd>` — run `SSHPoolMain` against a live host (version follows `pom.xml <version>`).

Tests are integration-style: several connect to a real SSH server (host/id/pwd are set inline in `@BeforeEach`), so they fail without reachable credentials.

## Coding Style & Naming Conventions

- Java 8 target (`maven.compiler.source/target`), UTF-8, 4-space indentation, no tabs.
- Package classes by feature: `com.j2s.secure.<protocol>` with a nested `ex/` package for exceptions.
- Use Lombok (`@Slf4j(topic = "ssh")` for SSH, `@Slf4j(topic = "sftp")` for SFTP; bare `@Slf4j` only in `SessionTimer`) and SLF4J for logging; never `System.out` in library code (`SSHPoolMain` and tests only).
- Interfaces without an `I` prefix; implementations suffixed `Impl` (e.g. `SSHSyncSessionImpl`). Exceptions follow `SSH<Something>Exception` / `SFTP<Something>Exception`. Pool-related classes take the `...Pool` / `...Factory` suffix.

## Architecture Notes

- One-shot sessions: `SSHSessionFactory.openSyncSession` / `openAsyncSession` (use with try-with-resources).
- Reuse: `SSHSyncSessionPool` (single host) vs `SSHSyncSessionKeyedPool` (multi-host) plus `SSHSessionManager`.
- SFTP: `SFTPSessionFactory` + `SFTPSessionManager` (no commons-pool2 pool unlike SSH).
- Idle expiry via `SessionTimer` (ExpiringMap); threading via `SecureExecutors`.

## Testing Guidelines

- Framework: JUnit 5 (`junit-jupiter-api`), `slf4j-simple` as the test runtime.
- `pom.xml` declares only `junit-jupiter-api` (no engine/surefire) — verify test discovery before trusting `mvn test -Dmaven.test.skip=false`.
- Name test classes `<ClassUnderTest>Test` and methods after the scenario (`execute`, `before`, `after`).
- Do not add new `Thread.sleep`-based live-host tests; prefer mocks or mark as manual/integration.
- Always free resources in `@AfterEach` (e.g. `pool.disconnectAll()`) to avoid leaking sessions; prefer try-with-resources for sessions.

## Security

- Never commit real host/id/password. Tests historically contain hardcoded credentials — replace with env vars or placeholders before committing.
- Sanitize `@BeforeEach` test constants and `SSHPoolMain` CLI args from logs and diffs.
- `StrictHostKeyChecking=no` is set in `SSHAbstractSession` / `SFTPAbstractSession` — do not change without discussion.

## Commit & Pull Request Guidelines

History uses release-tagged commits matching the README Change Log:

```
v2.1.3  - build setting  - upgrade lib
v2.1.2  - add log (connection ip)
```

- Prefix every commit with the target version (`vX.Y.Z`), followed by `-` bullet points describing each change.
- Add the same entry to the **Change Log** section of `README.md`.
- Bump `pom.xml <version>` in lockstep — commit prefix, README entry, and pom version must all match.
- Keep changes within a single release scope; update `uml.md`/`uml2.md` when pool or session lifecycles change.
