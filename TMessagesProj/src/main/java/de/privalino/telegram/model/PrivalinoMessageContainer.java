package de.privalino.telegram.model;

/**
 * Created by nico on 16/05/16.
 */
public class PrivalinoMessageContainer {

    private boolean isIncoming;
    private int senderId;
    private int receiverId;
    private int chatId;
    private int channelId;
    private int messageId;
    private String senderUserName;
    private String receiverUserName;
    private String text;
    private String senderFirstName;
    private String senderLastName;
    private String receiverFirstName;
    private String receiverLastName;


    public boolean isIncoming() {
        return isIncoming;
    }
    public void setIncoming(boolean isIncoming) {
        this.isIncoming = isIncoming;
    }
    public int getSenderId() {
        return senderId;
    }
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }
    public int getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }
    public int getChatId() {
        return chatId;
    }
    public void setChatId(int chatId) {
        this.chatId = chatId;
    }
    public int getChannelId() {
        return channelId;
    }
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
    public int getMessageId() {
        return messageId;
    }
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
    public String getSenderUserName() {
        return senderUserName;
    }
    public void setSenderUserName(String senderUserName) {
        this.senderUserName = senderUserName;
    }
    public String getReceiverUserName() {
        return receiverUserName;
    }
    public void setReceiverUserName(String receiverUserName) {
        this.receiverUserName = receiverUserName;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getSenderFirstName() {
        return senderFirstName;
    }
    public void setSenderFirstName(String senderFirstName) {
        this.senderFirstName = senderFirstName;
    }
    public String getSenderLastName() {
        return senderLastName;
    }
    public void setSenderLastName(String senderLastName) {
        this.senderLastName = senderLastName;
    }
    public String getReceiverFirstName() {
        return receiverFirstName;
    }
    public void setReceiverFirstName(String receiverFirstName) {
        this.receiverFirstName = receiverFirstName;
    }
    public String getReceiverLastName() {
        return receiverLastName;
    }
    public void setReceiverLastName(String receiverLastName) {
        this.receiverLastName = receiverLastName;
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(this.getClass().getSimpleName());
        buffer.append("]");

        buffer.append("\tIsIncoming: ");
        buffer.append(isIncoming);

        buffer.append("\tMessage: ");
        buffer.append(getText());

        buffer.append("\tsenderId: ");
        buffer.append(getSenderId());

        buffer.append("\tReceiverId: ");
        buffer.append(getReceiverId());

        buffer.append("\tChatId: ");
        buffer.append(getChatId());

        buffer.append("\tChannel: ");
        buffer.append(getChannelId());

        buffer.append("\tMessageId: ");
        buffer.append(getMessageId());

        if(getSenderUserName() != null){
            buffer.append("\tSenderUserName: " + getSenderUserName());
        }

        if(getReceiverUserName() != null){
            buffer.append("\tReceiverUserName: " + getReceiverUserName());
        }
        if(getSenderFirstName() != null){
            buffer.append("\tSenderFirstName: " + getSenderFirstName());
        }

        if(getSenderLastName() != null){
            buffer.append("\tSenderLastName: " + getSenderLastName());
        }

        if(getReceiverFirstName() != null){
            buffer.append("\tReceiveFirstName: " + getReceiverFirstName());
        }

        if(getReceiverLastName() != null){
            buffer.append("\tReceiveLastName: " + getReceiverLastName());
        }


        return buffer.toString();
    }
}
