package com.j2s.secure.ssh;

import com.jcraft.jsch.JSchException;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;

interface SSHSession extends Closeable {

	/**
	 * @param command
	 */
	void writeVoid(String command);

	/**
	 * ssh connect
	 * 
	 * @param host  - Server host
	 * @param port  - Server Port
	 * @param id	- Server Id
	 * @param pwd	- Server Pwd
	 */
	void connect(String host, int port, String id, String pwd) throws JSchException, IOException;

	/**
	 * ssh connect - Local Port Forwarding
	 * 
	 * @param tHost - Ternel Server host
	 * @param tPort - Ternel Server port
	 * @param tId   - Ternel Server Id
	 * @param tPwd  - Ternel server Pwd
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
	 * @param name
	 */
	void setName(String name);

	/**
	 * @return name
	 */
	String getName();

	/**
	 * @return session key (uuid)
	 */
	String getSessionKey();

	/**
	 * @return create session time
	 */
	LocalDateTime getCreateDate();
}
