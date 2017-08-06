package de.privalino.telegram.model;

import java.util.Date;

public class PrivalinoBlockedUser {

	private long user;
	private long blockingUser;
	private Boolean blocked;
	private Date date;
	
	public PrivalinoBlockedUser(){
		date = new Date();
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

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
