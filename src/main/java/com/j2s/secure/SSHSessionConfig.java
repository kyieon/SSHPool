package com.j2s.secure;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SSHSessionConfig {
	
	String host;
	int port = 22; //Default
	String id;
	String pwd;
}