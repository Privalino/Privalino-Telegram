package de.privalino.telegram.model;

public class PrivalinoFeedback {
	
	private String message;
	private PrivalinoPopUp popUp;
	private boolean blocked;
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public PrivalinoPopUp getPopUp() {
		return popUp;
	}

	public void setPopUp(PrivalinoPopUp popUp) {
		this.popUp = popUp;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
}
