package com.j2s.secure.ssh;

import com.j2s.secure.SSHAsyncMessage;
import com.j2s.secure.ssh.ex.SSHTriggerAlreadyExistException;

import java.io.IOException;
import java.util.function.Consumer;


public interface SSHAsyncSession extends SSHSession {

	/**
	 * @param command
	 */
	void write(String command) throws IOException;
	
	void onTrigger(Consumer<SSHAsyncMessage> consumer) throws SSHTriggerAlreadyExistException;
}
