package de.privalino.telegram.model;

public class PrivalinoFeedback {

	private String message;
	private PrivalinoPopUp popUp;
	private boolean blocked;
	private boolean firstMessage;

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

	public boolean isFirstMessage() {
		return firstMessage;
	}

	public void setFirstMessage(boolean firstMessage) {
		this.firstMessage = firstMessage;
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

        if(isFirstMessage() != true){
            buffer.append("\tFirstMessage: true");
        }

		buffer.append("\tIsBlocked: " + isBlocked());

		return buffer.toString();
	}
}
