package de.privalino.telegram.model;

import java.util.Date;

public class PrivalinoBlockUser {

	private long user;
	private long blockingUser;
	Boolean blocked;

	public Boolean getBlocked() {
		return blocked;
	}

	public void setBlocked(Boolean blocked) {
		this.blocked = blocked;
	}

	public long getUser() {
		return user;
	}

	public long getBlockingUser() {
		return blockingUser;
	}

	public void setUser(long user) {
		this.user = user;
	}

	public void setBlockingUser(long blockingUser) {
		this.blockingUser = blockingUser;
	}
}
