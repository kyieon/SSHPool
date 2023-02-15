package com.j2s.secure.ssh.ex;

public class SSHSessionAlreadyExistException extends SSHSessionException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6281500408983465122L;

	public SSHSessionAlreadyExistException(String message) {
		super(message);
	}
}
