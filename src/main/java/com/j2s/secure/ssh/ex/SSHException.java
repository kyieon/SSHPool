package com.j2s.secure.ssh.ex;

import java.io.IOException;

public class SSHException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1081893379627505895L;

	public SSHException() { 
		super();
	}
	
	public SSHException(String message) {
		super(message);
	}
}
