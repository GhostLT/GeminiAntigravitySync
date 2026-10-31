package com.ghostlt.geminisync;

public class ChatMessage {
    private final String text;
    private final boolean isUser;
    private final String timestamp;
    private final String imageBase64;
    private final String modeTag;

    public ChatMessage(String text, boolean isUser, String timestamp) {
        this(text, isUser, timestamp, null, null);
    }

    public ChatMessage(String text, boolean isUser, String timestamp, String imageBase64, String modeTag) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.imageBase64 = imageBase64;
        this.modeTag = modeTag;
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

    public String getImageBase64() {
        return imageBase64;
    }

    public String getModeTag() {
        return modeTag;
    }

    public boolean hasImage() {
        return imageBase64 != null && !imageBase64.isEmpty();
    }
}
