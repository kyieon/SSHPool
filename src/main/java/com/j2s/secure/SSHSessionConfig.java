package com.j2s.secure;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString(exclude = "pwd")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SSHSessionConfig {
	
	String host;
	int port = 22; //Default
	String id;
	String pwd;
	boolean verifyHostKey = false; //SEC: opt-in strict host-key verification (default off = legacy behaviour)
}