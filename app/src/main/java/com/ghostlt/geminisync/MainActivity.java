package com.ghostlt.geminisync;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etMessageInput;
    private MaterialButton btnSend;
    private MaterialButton btnQuickSync;
    private ImageButton btnSettings;
    private ProgressBar progressBar;
    private TextView tvSyncStatus;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ConfigManager configManager;
    private GeminiApiClient geminiClient;
    private MemoryExtractor memoryExtractor;
    private GitHubSyncManager syncManager;

    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = new ConfigManager(this);
        geminiClient = new GeminiApiClient();
        memoryExtractor = new MemoryExtractor(geminiClient);
        syncManager = new GitHubSyncManager(this, configManager);

        initViews();
        setupChatRecycler();
        setupListeners();

        // Mensaje de bienvenida inicial
        addGeminiMessage("¡Hola! Soy tu asistente Gemini conectado al ecosistema Antigravity. Pregúntame sobre código, arquitectura o depuración. Cada sesión técnica se unificará automáticamente con tu PC.");
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        btnQuickSync = findViewById(R.id.btnQuickSync);
        btnSettings = findViewById(R.id.btnSettings);
        progressBar = findViewById(R.id.progressBar);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
    }

    private void setupChatRecycler() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnQuickSync.setOnClickListener(v -> triggerAutoSync(false));
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        etMessageInput.setText("");
        addUserMessage(text);

        progressBar.setVisibility(View.VISIBLE);
        tvSyncStatus.setText("● Gemini pensando...");

        String apiKey = configManager.getApiKey();
        if (apiKey.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            tvSyncStatus.setText("● Falta API Key");
            showSettingsDialog();
            return;
        }

        geminiClient.sendMessage(messageList, text, apiKey, new GeminiApiClient.GeminiCallback() {
            @Override
            public void onSuccess(String reply) {
                progressBar.setVisibility(View.GONE);
                tvSyncStatus.setText("● Listo (Auto-Sync activo)");
                addGeminiMessage(reply);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                tvSyncStatus.setText("● Error de conexión");
                addGeminiMessage("❌ Error: " + error);
            }
        });
    }

    private void addUserMessage(String text) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messageList.add(new ChatMessage(text, true, time));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }

    private void addGeminiMessage(String text) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messageList.add(new ChatMessage(text, false, time));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }

    private void triggerAutoSync(boolean silent) {
        if (isSyncing || messageList.size() < 2) {
            if (!silent) {
                Toast.makeText(this, "No hay suficiente contenido técnico para sincronizar.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        isSyncing = true;
        tvSyncStatus.setText("● Extrayendo memoria técnica...");
        if (!silent) progressBar.setVisibility(View.VISIBLE);

        String apiKey = configManager.getApiKey();
        memoryExtractor.extractMemory(messageList, apiKey, new MemoryExtractor.ExtractionCallback() {
            @Override
            public void onExtracted(String markdownSummary) {
                syncManager.syncSession(markdownSummary, new GitHubSyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String detail) {
                        isSyncing = false;
                        progressBar.setVisibility(View.GONE);
                        tvSyncStatus.setText("● Sincronizado");
                        if (!silent) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("⚡ Memoria Sincronizada")
                                    .setMessage(detail)
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        isSyncing = false;
                        progressBar.setVisibility(View.GONE);
                        tvSyncStatus.setText("● Error al sincronizar");
                        if (!silent) {
                            Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                isSyncing = false;
                progressBar.setVisibility(View.GONE);
                tvSyncStatus.setText("● Error en extracción");
                if (!silent) {
                    Toast.makeText(MainActivity.this, "Error al extraer: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showSettingsDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);

        EditText etApiKey = dialogView.findViewById(R.id.etApiKey);
        EditText etRepoName = dialogView.findViewById(R.id.etRepoName);
        EditText etGithubToken = dialogView.findViewById(R.id.etGithubToken);
        CheckBox cbAutoSync = dialogView.findViewById(R.id.cbAutoSync);

        etApiKey.setText(configManager.getApiKey());
        etRepoName.setText(configManager.getGithubRepo());
        etGithubToken.setText(configManager.getGithubToken());
        cbAutoSync.setChecked(configManager.isAutoSyncEnabled());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    configManager.setApiKey(etApiKey.getText().toString().trim());
                    configManager.setGithubRepo(etRepoName.getText().toString().trim());
                    configManager.setGithubToken(etGithubToken.getText().toString().trim());
                    configManager.setAutoSyncEnabled(cbAutoSync.isChecked());
                    Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (configManager.isAutoSyncEnabled() && messageList.size() >= 3) {
            triggerAutoSync(true);
        }
    }
}
