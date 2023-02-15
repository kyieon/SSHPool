package com.j2s.secure;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SSHAsyncMessage {

	String sessionKey;
	String result;
}
