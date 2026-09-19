package com.j2s.secure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two security invariants of {@link SSHSessionConfig}:
 * the credential must never reach a log file, and the opt-in host-key
 * verification flag must default to the legacy (off) behaviour.
 */
class SSHSessionConfigTest {

    private static final String SECRET = "p@ssw0rd-leak-canary";

    @Test
    void toStringMustNotExposeThePassword() {
        SSHSessionConfig config = new SSHSessionConfig();
        config.setHost("example.com");
        config.setPort(2222);
        config.setId("user");
        config.setPwd(SECRET);

        String str = config.toString();

        assertFalse(str.contains(SECRET), "toString() leaks the password: " + str);
        // non-sensitive fields must still be there, so the exclusion is targeted, not a blanket wipe
        assertTrue(str.contains("example.com"), "host missing from toString(): " + str);
        assertTrue(str.contains("user"), "id missing from toString(): " + str);
    }

    @Test
    void verifyHostKeyDefaultsToFalse() {
        SSHSessionConfig config = new SSHSessionConfig();
        assertFalse(config.isVerifyHostKey(), "host-key verification must be opt-in (legacy default)");

        config.setVerifyHostKey(true);
        assertTrue(config.isVerifyHostKey(), "setter/getter round-trip failed");
    }
}
