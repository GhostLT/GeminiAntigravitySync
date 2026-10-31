package com.ghostlt.geminisync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public interface SessionClickListener {
        void onSessionClick(SessionDatabaseHelper.SessionItem session);
        void onSessionDelete(SessionDatabaseHelper.SessionItem session);
    }

    private final List<SessionDatabaseHelper.SessionItem> sessions;
    private final SessionClickListener listener;

    public SessionAdapter(List<SessionDatabaseHelper.SessionItem> sessions, SessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_history, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        SessionDatabaseHelper.SessionItem item = sessions.get(position);
        holder.tvTitle.setText(item.title);

        String meta = item.updatedAt + " • " + item.messageCount + " mensajes • " + item.modeTag;
        holder.tvMeta.setText(meta);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onSessionDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta;
        ImageButton btnDelete;

        SessionViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvMeta = itemView.findViewById(R.id.tvSessionMeta);
            btnDelete = itemView.findViewById(R.id.btnDeleteSession);
        }
    }
}
