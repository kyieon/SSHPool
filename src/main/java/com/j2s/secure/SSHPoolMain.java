package com.j2s.secure;

import com.j2s.secure.ssh.SSHSessionFactory;
import com.j2s.secure.ssh.SSHSyncSession;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;

import java.util.UUID;

public class SSHPoolMain {

    public static void main(String[] args) {
        String host = args[0];
        String id = args[1];
        String pwd = args[2];

        JSch.setLogger(new Logger() {
            @Override
            public boolean isEnabled(int i) {
                return true;
            }

            @Override
            public void log(int i, String s) {
                System.out.println(s);
            }
        });

        try (SSHSyncSession session = SSHSessionFactory.openSyncSession(UUID.randomUUID().toString(), host, 22, id, pwd);) {
            String result = session.write("ll");
            System.out.println(result);
            Thread.sleep(5 * 1000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
