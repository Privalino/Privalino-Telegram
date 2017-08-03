package de.privalino.telegram.model;

import java.util.Date;

public class PrivalinoPopUp {
	
	private long id;
	private String question;
	private String[] answerOptions;
	
	public PrivalinoPopUp(){
		this.id = new Date().getTime();
	}
	
	public long getId() {
		return id;
	}
	public String getQuestion() {
		return question;
	}
	public void setQuestion(String question) {
		this.question = question;
	}
	public String[] getAnswerOptions() {
		return answerOptions;
	}
	public void setAnswerOptions(String... answerOptions) {
		this.answerOptions = answerOptions;
	}

}
