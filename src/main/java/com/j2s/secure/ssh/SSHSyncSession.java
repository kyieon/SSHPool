package com.j2s.secure.ssh;

import java.io.IOException;

public interface SSHSyncSession extends SSHSession {
	
	/**
	 * @param command
	 */
	void writeVoid(String command);

	/**
	 * @param command
	 */
	String write(String command) throws IOException;

	/**
	 * @param command
	 * @param readTimeOut (Second)
	 * @return result
	 */
	String write(String command, int readTimeOut) throws IOException;

	/**
	 * @param command
	 * @param prompt
	 * @return result
	 */
	String write(String command, String prompt) throws IOException;

	/**
	 * @param command
	 * @param prompt
	 * @param readTimeOut (Second)
	 * @return result
	 */
	String write(String command, String prompt, int readTimeOut) throws IOException;

	long getQueueCount();

	long getErrorCount();

}
