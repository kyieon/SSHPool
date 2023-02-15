package com.j2s.secure.sftp.ex;

import java.io.IOException;

public class SFTPException extends IOException {

	private static final long serialVersionUID = 6205282296228734639L;

	public SFTPException() {
		super();
	}

	public SFTPException(String message) {
		super(message);
	}
}
