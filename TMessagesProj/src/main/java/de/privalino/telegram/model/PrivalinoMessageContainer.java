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
    private String senderUser;
    private String receiverUser;
    private String text;


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
    public String getSenderUser() {
        return senderUser;
    }
    public void setSenderUser(String senderUser) {
        this.senderUser = senderUser;
    }
    public String getReceiverUser() {
        return receiverUser;
    }
    public void setReceiverUser(String receiverUser) {
        this.receiverUser = receiverUser;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(this.getClass().getSimpleName());
        buffer.append("]");

        buffer.append("\tMessage: ");
        buffer.append(text.toString());
//
//        buffer.append("\tCorrect?: ");
//        buffer.append(isCorrect);
//
//        buffer.append("\tRejected?: ");
//        buffer.append(isRejected);
//
//        buffer.append("\tDeviceId: ");
//        buffer.append(deviceId);

        return buffer.toString();
    }
}
