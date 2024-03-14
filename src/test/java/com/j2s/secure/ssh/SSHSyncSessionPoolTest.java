package com.j2s.secure.ssh;

import com.j2s.secure.SSHSessionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SSHSyncSessionPoolTest {

    private SSHSyncSessionPool pool;

    @BeforeEach
    void before() {
        SSHSessionConfig config = new SSHSessionConfig();
        config.setHost("13.124.86.77");
        config.setPort(22);
        config.setId("ubuntu");
        config.setPwd("VxXbARmNzKwOvKDKV1234ul");

        pool = new SSHSyncSessionPool(config);
    }

    @AfterEach
    void after() {
        pool.disconnectAll();
    }

    @Test
    void execute() throws InterruptedException {
        System.out.println("Start >> " + Thread.activeCount() + "[ " + Thread.currentThread().getThreadGroup() + " ]");

        for (int i = 1; i < 10; i++) {
            String result = executeTest();
            System.out.println(result);
            System.out.println(i + ">> " + Thread.activeCount());

            Thread.sleep(1 * 1000);
        }

        Thread.sleep(1 * 1000);
        System.out.println("End >> " + Thread.activeCount());
    }

    private String executeTest() {
        String result = null;
        try {
            result = this.pool.execute((session) -> {
                try {
                    return session.write("ll");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Test
    void executeOnce() throws Exception {
        String result = this.pool.executeOnce((session) -> {
            try {
                return session.write("ll");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(result);

        Thread.sleep(1 * 1000);
    }
}