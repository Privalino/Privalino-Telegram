package de.privalino.telegram.model;

public class PrivalinoBlockedUser {

	private int user;
	private int blockingUser;
	private Boolean isBlocked;

	public Boolean getIsBlocked() {
		return isBlocked;
	}

	public void setIsBlocked(Boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public int getUser() {
		return user;
	}

	public int getBlockingUser() {
		return blockingUser;
	}

	public void setUser(int user) {
		this.user = user;
	}

	public void setBlockingUser(int blockingUser) {
		this.blockingUser = blockingUser;
	}

	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		buffer.append(this.getClass().getSimpleName());
		buffer.append("]");

		buffer.append("\tBlockingUser: " + getBlockingUser());
		buffer.append("\tUser: " + getUser());
		buffer.append("\tIsBlocked: " + getIsBlocked());

		return buffer.toString();
	}
}