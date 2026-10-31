package com.ghostlt.geminisync;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_GEMINI = 2;

    public interface MessageActionListener {
        void onSpeak(String text);
        void onCopy(String text);
        void onShare(String text);
        void onGmail(String text);
        void onCanvas(String text);
    }

    private final List<ChatMessage> messages;
    private final MessageActionListener actionListener;

    public ChatAdapter(List<ChatMessage> messages, MessageActionListener actionListener) {
        this.messages = messages;
        this.actionListener = actionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_GEMINI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_gemini, parent, false);
            return new GeminiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setText(message.getText());
            userHolder.tvTime.setText(message.getTimestamp());

            if (message.hasImage()) {
                userHolder.ivAttachment.setVisibility(View.VISIBLE);
                try {
                    byte[] decoded = Base64.decode(message.getImageBase64(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    userHolder.ivAttachment.setImageBitmap(bitmap);
                } catch (Exception e) {
                    userHolder.ivAttachment.setVisibility(View.GONE);
                }
            } else {
                userHolder.ivAttachment.setVisibility(View.GONE);
            }
        } else if (holder instanceof GeminiViewHolder) {
            GeminiViewHolder geminiHolder = (GeminiViewHolder) holder;
            geminiHolder.tvMessage.setText(message.getText());
            geminiHolder.tvTime.setText(message.getTimestamp());

            if (message.getModeTag() != null && !message.getModeTag().isEmpty()) {
                geminiHolder.tvModeTag.setVisibility(View.VISIBLE);
                geminiHolder.tvModeTag.setText(message.getModeTag());
            } else {
                geminiHolder.tvModeTag.setVisibility(View.GONE);
            }

            final String contentText = message.getText();
            geminiHolder.btnSpeak.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onSpeak(contentText);
            });
            geminiHolder.btnCopy.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onCopy(contentText);
            });
            geminiHolder.btnShare.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onShare(contentText);
            });
            geminiHolder.btnGmail.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onGmail(contentText);
            });
            geminiHolder.btnCanvas.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onCanvas(contentText);
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivAttachment;

        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
            tvTime = itemView.findViewById(R.id.tvUserTime);
            ivAttachment = itemView.findViewById(R.id.ivUserAttachment);
        }
    }

    static class GeminiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvModeTag, btnCanvas;
        ImageButton btnSpeak, btnCopy, btnShare, btnGmail;

        GeminiViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvGeminiMessage);
            tvTime = itemView.findViewById(R.id.tvGeminiTime);
            tvModeTag = itemView.findViewById(R.id.tvModeTag);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnGmail = itemView.findViewById(R.id.btnGmail);
            btnCanvas = itemView.findViewById(R.id.btnCanvas);
        }
    }
}
