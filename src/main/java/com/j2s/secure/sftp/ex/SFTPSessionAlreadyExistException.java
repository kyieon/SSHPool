package com.j2s.secure.sftp.ex;

public class SFTPSessionAlreadyExistException extends SFTPSessionException {


	private static final long serialVersionUID = 356404334035628776L;

	public SFTPSessionAlreadyExistException(String message) {
		super(message);
	}
}
