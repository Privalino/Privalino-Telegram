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

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		buffer.append(this.getClass().getSimpleName());
		buffer.append("]");

		if(getMessage() != null){
			buffer.append("\tMessage: " + getMessage());
		}

		if(getPopUp() != null){
			buffer.append("\tPopUp: " + getPopUp().toString());
		}

		buffer.append("\tIsBlocked: " + isBlocked());

		return buffer.toString();
	}
}
