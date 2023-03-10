package com.j2s.secure.sftp;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public interface SFTPSession extends Closeable {

    /**
     * sftp connect
     *
     * @param host  - Server host
     * @param port  - Server Port
     * @param id	- Server Id
     * @param pwd	- Server Pwd
     */
    void connect(String host, int port, String id, String pwd) throws JSchException, IOException;

    /**
     * sftp connect - Local Port Forwarding
     *
     * @param tHost - Turnel Server host
     * @param tPort - Turnel Server port
     * @param tId   - Turnel Server Id
     * @param tPwd  - Turnel server Pwd
     * @param host  - Server host
     * @param port  - Server Port
     * @param id	- Server Id
     * @param pwd	- Server Pwd
     */
    void connectTunnel(String tHost, int tPort, String tId, String tPwd, String host, int port, String id, String pwd) throws JSchException, IOException;

    /**
     * @return Boolean (true: connect, false: not connect)
     */
    boolean isConnected();

    /**
     * @return session key (uuid)
     */
    String getSessionKey();

    /**
     * @return create session time
     */
    LocalDateTime getCreateDate();

    /** Method **/
    void cd(String path) throws SftpException;

    String pwd() throws SftpException;

    List<ChannelSftp.LsEntry> ls() throws SftpException;

    List<ChannelSftp.LsEntry> ls(String path) throws SftpException;

    InputStream get(String name) throws SftpException;

    /** Use Tmp File **/
    File getFile(String name) throws SftpException, IOException;

    String cat(String name) throws SftpException, IOException;

    void put(File file) throws SftpException;

    void put(File file, String name) throws SftpException;

    void rm(String name) throws SftpException;

    void rename(String srcName, String dstName) throws SftpException;

}
