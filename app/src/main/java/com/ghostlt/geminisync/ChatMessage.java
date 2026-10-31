package com.ghostlt.geminisync;

public class ChatMessage {
    private final String text;
    private final boolean isUser;
    private final String timestamp;

    public ChatMessage(String text, boolean isUser, String timestamp) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
