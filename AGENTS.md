# AGENTS.md — secure-client (SSHPool)

- Java 8 library (`com.j2s:secure-client`), single Maven module. Build: `mvn package`.
- Tests disabled by default (`maven.test.skip=true` in pom.xml). To run: `mvn test -Dmaven.test.skip=false`.
- Do NOT run `src/test/*` normally: they dial hardcoded internal SSH host (`10.180.92.250`) with sleeps; no assertions, will hang/fail off-network.
- Single-test (when on target network): `mvn test -Dmaven.test.skip=false -Dtest=SSHSyncSessionPoolTest`.
- JSch = `com.github.mwiede:jsch` (not `com.jcraft`). SFTP/SSH impls wrap shell channels with prompt detection (`custom_end_prompts`); see `SSHSessionFactory.openSyncSession(..., String... custom_end_prompts)`.
- Entry: `SSHSessionFactory` / `SFTPSessionFactory`; sessions registered in `SSHSessionManager`/`SFTPSessionManager` singletons by `sessionKey`. Sync (`write()->String`) vs Async (`onTrigger`+`write`). Pooling via commons-pool2 (`SSHSyncSessionPool`, `SSHSyncSessionKeyedPool`).
- Lombok `provided` scope; keep source Java 8 compatible.
- Publish is file-based (`distributionManagement` → `file:./target/deploy`).
