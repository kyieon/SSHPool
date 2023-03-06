package com.j2s.secure;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SSHSessionKeyedConfig extends SSHSessionConfig {
	
	List<String> hosts;

	@Deprecated
	@Override
	public String getHost() {
		return super.getHost();
	}

	@Deprecated
	@Override
	public void setHost(String host) {
		super.setHost(host);
	}
}