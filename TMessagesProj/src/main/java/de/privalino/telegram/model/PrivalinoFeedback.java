package de.privalino.telegram.model;

public class PrivalinoFeedback {

	private String message;
	private PrivalinoPopUp popUp;
	private boolean isBlocked;
	private boolean isFirstMessage;
	private boolean isWhitelisted;

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

	public boolean getIsBlocked() {
		return isBlocked;
	}

	public void setIsBlocked(boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public boolean getIsFirstMessage() {
		return isFirstMessage;
	}

	public void setIsFirstMessage(boolean isFirstMessage) {
		this.isFirstMessage = isFirstMessage;
	}

	public boolean getIsWhitelisted() {
		return isWhitelisted;
	}

	public void setIsWhitelisted(boolean isWhitelisted) {
		this.isWhitelisted = isWhitelisted;
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

        buffer.append("\tIsFirstMessage: " + getIsFirstMessage());

        buffer.append("\tIsBlocked: " + getIsBlocked());

        buffer.append("\tIsWhitelisted: " + getIsWhitelisted());

        return buffer.toString();
	}
}
