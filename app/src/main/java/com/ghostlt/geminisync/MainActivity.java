package com.ghostlt.geminisync;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ChatAdapter.MessageActionListener {

    private RecyclerView recyclerChat;
    private EditText etMessageInput;
    private MaterialButton btnSend;
    private MaterialButton btnQuickSync;
    private MaterialButton btnModes;
    private ImageButton btnNewBranch;
    private ImageButton btnHistory;
    private ImageButton btnSettings;
    private ImageButton btnAttach;
    private ImageButton btnCamera;
    private ImageButton btnMic;
    private ProgressBar progressBar;
    private TextView tvSyncStatus;

    // Barra de preview de adjunto
    private LinearLayout layoutAttachmentPreview;
    private ImageView ivPreviewThumbnail;
    private TextView tvPreviewName;
    private ImageButton btnRemoveAttachment;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ConfigManager configManager;
    private GeminiApiClient geminiClient;
    private MemoryExtractor memoryExtractor;
    private GitHubSyncManager syncManager;
    private SessionDatabaseHelper dbHelper;

    private String currentSessionId;
    private String currentSessionTitle;

    private TextToSpeech tts;
    private boolean isSyncing = false;
    private String currentAttachedBase64 = null;
    private String currentModeInstruction = null;
    private String currentModeName = "Modo Estándar";

    // Launchers
    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = new ConfigManager(this);
        geminiClient = new GeminiApiClient();
        memoryExtractor = new MemoryExtractor(geminiClient);
        syncManager = new GitHubSyncManager(this, configManager);
        dbHelper = new SessionDatabaseHelper(this);

        initSessionId();
        initViews();
        setupChatRecycler();
        setupLaunchers();
        setupListeners();
        setupTTS();

        // Procesar intents entrantes (Compartir desde otras apps)
        handleIncomingIntent(getIntent());

        if (messageList.isEmpty()) {
            addGeminiMessage("¡Hola! Soy Gemini Sync Pro. Estoy listo para ayudarte con código, arquitectura, capturas de pantalla, generación multimedia y análisis profundo. Todo se unificará automáticamente con tu PC.");
        }
    }

    private void initSessionId() {
        currentSessionId = "session_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentSessionTitle = null;
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        btnQuickSync = findViewById(R.id.btnQuickSync);
        btnModes = findViewById(R.id.btnModes);
        btnNewBranch = findViewById(R.id.btnNewBranch);
        btnHistory = findViewById(R.id.btnHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnAttach = findViewById(R.id.btnAttach);
        btnCamera = findViewById(R.id.btnCamera);
        btnMic = findViewById(R.id.btnMic);
        progressBar = findViewById(R.id.progressBar);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);

        layoutAttachmentPreview = findViewById(R.id.layoutAttachmentPreview);
        ivPreviewThumbnail = findViewById(R.id.ivPreviewThumbnail);
        tvPreviewName = findViewById(R.id.tvPreviewName);
        btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);
    }

    private void setupChatRecycler() {
        chatAdapter = new ChatAdapter(messageList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("es", "ES"));
            }
        });
    }

    private void setupLaunchers() {
        // Selector de galería / archivos / capturas
        galleryPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                processSelectedImageUri(uri);
            }
        });

        // Captura directa con cámara
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                processCapturedBitmap(bitmap);
            }
        });

        // Dictado por voz (Speech to Text)
        speechLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    String current = etMessageInput.getText().toString();
                    if (!current.isEmpty()) {
                        etMessageInput.setText(current + " " + spokenText);
                    } else {
                        etMessageInput.setText(spokenText);
                    }
                    etMessageInput.setSelection(etMessageInput.getText().length());
                }
            }
        });
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnQuickSync.setOnClickListener(v -> triggerAutoSync(false));
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnNewBranch.setOnClickListener(v -> confirmNewBranch());
        btnHistory.setOnClickListener(v -> showHistoryDialog());
        btnModes.setOnClickListener(v -> showModesMenu(v));

        btnAttach.setOnClickListener(v -> galleryPickerLauncher.launch("image/*"));
        btnCamera.setOnClickListener(v -> cameraLauncher.launch(null));
        btnMic.setOnClickListener(v -> startVoiceRecognition());

        btnRemoveAttachment.setOnClickListener(v -> clearAttachment());
    }

    // --- Manejo de Dictado por Voz (STT) ---
    private void startVoiceRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla tu prompt técnico...");
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Dictado por voz no disponible en este dispositivo.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Manejo de Capturas / Imágenes ---
    private void processSelectedImageUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                processCapturedBitmap(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al leer imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processCapturedBitmap(Bitmap bitmap) {
        // Redimensionar para optimizar ancho de banda de API
        int maxDim = 1024;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxDim || height > maxDim) {
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxDim;
                height = (int) (maxDim / ratio);
            } else {
                height = maxDim;
                width = (int) (maxDim * ratio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        currentAttachedBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

        ivPreviewThumbnail.setImageBitmap(bitmap);
        tvPreviewName.setText("Captura adjunta (" + (bytes.length / 1024) + " KB) lista para análisis");
        layoutAttachmentPreview.setVisibility(View.VISIBLE);
    }

    private void clearAttachment() {
        currentAttachedBase64 = null;
        layoutAttachmentPreview.setVisibility(View.GONE);
    }

    // --- Enviar Mensaje ---
    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty() && currentAttachedBase64 == null) return;

        etMessageInput.setText("");
        String attachedImage = currentAttachedBase64;
        clearAttachment();

        addUserMessage(text, attachedImage);

        progressBar.setVisibility(View.VISIBLE);
        tvSyncStatus.setText("● Gemini pensando (" + currentModeName + ")...");

        String apiKey = configManager.getApiKey();
        if (apiKey.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            tvSyncStatus.setText("● Falta API Key");
            showSettingsDialog();
            return;
        }

        geminiClient.sendMessage(messageList, text, attachedImage, currentModeInstruction, apiKey, new GeminiApiClient.GeminiCallback() {
            @Override
            public void onSuccess(String reply) {
                progressBar.setVisibility(View.GONE);
                tvSyncStatus.setText("● " + currentModeName);
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

    private void addUserMessage(String text, String imageBase64) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messageList.add(new ChatMessage(text, true, time, imageBase64, null));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);

        if (currentSessionTitle == null && text != null && !text.isEmpty()) {
            currentSessionTitle = text.length() > 45 ? text.substring(0, 45) + "..." : text;
        }
        saveCurrentSessionToDb();
    }

    private void addGeminiMessage(String text) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        messageList.add(new ChatMessage(text, false, time, null, currentModeName));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);

        saveCurrentSessionToDb();
    }

    private void saveCurrentSessionToDb() {
        if (messageList.size() > 1 && currentSessionId != null) {
            String title = currentSessionTitle != null ? currentSessionTitle : "Sesión " + new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
            dbHelper.saveOrUpdateSession(currentSessionId, title, messageList, currentModeName);
        }
    }

    // --- Historial de Sesiones ---
    private void showHistoryDialog() {
        saveCurrentSessionToDb();
        List<SessionDatabaseHelper.SessionItem> sessions = dbHelper.getAllSessions();

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_session_history, null);

        RecyclerView recyclerSessions = view.findViewById(R.id.recyclerSessions);
        TextView tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);
        ImageButton btnClose = view.findViewById(R.id.btnCloseHistory);
        com.google.android.material.button.MaterialButton btnClear = view.findViewById(R.id.btnClearAllSessions);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view)
                .create();

        if (sessions.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            recyclerSessions.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            recyclerSessions.setVisibility(View.VISIBLE);
            recyclerSessions.setLayoutManager(new LinearLayoutManager(this));
            SessionAdapter adapter = new SessionAdapter(sessions, new SessionAdapter.SessionClickListener() {
                @Override
                public void onSessionClick(SessionDatabaseHelper.SessionItem session) {
                    List<ChatMessage> restoredMsgs = dbHelper.getSessionMessages(session.sessionId);
                    if (restoredMsgs != null && !restoredMsgs.isEmpty()) {
                        currentSessionId = session.sessionId;
                        currentSessionTitle = session.title;
                        currentModeName = session.modeTag != null ? session.modeTag : "Modo Estándar";
                        messageList.clear();
                        messageList.addAll(restoredMsgs);
                        chatAdapter.notifyDataSetChanged();
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                        tvSyncStatus.setText("● " + currentModeName);
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Reanudada: " + session.title, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No se encontraron mensajes en esta sesión.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onSessionDelete(SessionDatabaseHelper.SessionItem session) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Eliminar sesión")
                            .setMessage("¿Deseas eliminar '" + session.title + "' del historial?")
                            .setPositiveButton("Eliminar", (d, w) -> {
                                dbHelper.deleteSession(session.sessionId);
                                sessions.remove(session);
                                if (sessions.isEmpty()) {
                                    tvEmptyHistory.setVisibility(View.VISIBLE);
                                    recyclerSessions.setVisibility(View.GONE);
                                }
                                recyclerSessions.getAdapter().notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, "Sesión eliminada", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
            });
            recyclerSessions.setAdapter(adapter);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnClear.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Vaciar Historial")
                    .setMessage("¿Estás seguro de que deseas eliminar todas las sesiones guardadas?")
                    .setPositiveButton("Vaciar", (d, w) -> {
                        dbHelper.clearAllSessions();
                        dialog.dismiss();
                        Toast.makeText(this, "Historial vaciado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        dialog.show();
    }

    // --- Crear Rama / Nuevo Chat ---
    private void confirmNewBranch() {
        if (messageList.size() <= 1) {
            Toast.makeText(this, "El chat actual ya está vacío.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("🌱 Crear Nueva Rama de Chat")
                .setMessage("¿Deseas iniciar una nueva rama? Se sincronizará automáticamente el conocimiento de esta sesión y se abrirá un chat limpio.")
                .setPositiveButton("Sí, Nueva Rama", (dialog, which) -> {
                    saveCurrentSessionToDb();
                    triggerAutoSync(true);
                    messageList.clear();
                    initSessionId();
                    addGeminiMessage("🌱 Nueva rama iniciada. El contexto de la sesión anterior fue respaldado. ¿En qué trabajamos ahora?");
                    chatAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- Selector de Modos Especiales ---
    private void showModesMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "⚡ Modo Estándar");
        popup.getMenu().add(0, 2, 0, "🔍 Deep Research");
        popup.getMenu().add(0, 3, 0, "📝 Canvas de Código/Docs");
        popup.getMenu().add(0, 4, 0, "🎓 Aprendizaje Guiado (Tutor)");
        popup.getMenu().add(0, 5, 0, "🎨 Crear Imágenes (Imagen 3)");
        popup.getMenu().add(0, 6, 0, "🎬 Video & Música Studio");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    currentModeInstruction = null;
                    currentModeName = "Modo Estándar";
                    break;
                case 2:
                    currentModeInstruction = "MODO DEEP RESEARCH: Realiza una investigación exhaustiva, rigurosa y multidimensional del tema propuesto. Desglosa arquitectura técnica, pros, contras, mejores prácticas, casos borde y referencias comprobadas.";
                    currentModeName = "🔍 Deep Research";
                    break;
                case 3:
                    currentModeInstruction = "MODO CANVAS: Genera código modular completo de nivel de producción o documentos técnicos detallados listos para copiar y ejecutar, sin omitir bloques ni usar comentarios de relleno.";
                    currentModeName = "📝 Canvas";
                    break;
                case 4:
                    currentModeInstruction = "MODO APRENDIZAJE GUIADO (TUTOR): Actúa como un mentor socrático experto. Explica de forma progresiva desde los fundamentos, realiza preguntas de comprobación y guía paso a paso con analogías y código práctico.";
                    currentModeName = "🎓 Aprendizaje Guiado";
                    break;
                case 5:
                    currentModeInstruction = "MODO GENERADOR DE IMÁGENES: Describe la composición visual fotográfica o artística con precisión extrema (iluminación, lente, ángulo, paleta de color y directivas de estilo) optimizada para modelos de difusión como Imagen 3.";
                    currentModeName = "🎨 Crear Imágenes";
                    break;
                case 6:
                    currentModeInstruction = "MODO ESTUDIO AUDIOVISUAL (VIDEO/MÚSICA): Proporciona guiones cinemáticos segundo a segundo, prompts de generación de video generativo (Sora/Runway/Omni) y estructuras líricas y métricas de audio (Suno/Udio).";
                    currentModeName = "🎬 Video & Música";
                    break;
            }
            tvSyncStatus.setText("● " + currentModeName);
            Toast.makeText(this, "Activado: " + currentModeName, Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    // --- Implementación de MessageActionListener (Bocina, Compartir, Gmail, Canvas) ---
    @Override
    public void onSpeak(String text) {
        if (tts == null) return;
        // Limpiar markdown simple para lectura fluida
        String cleanText = text.replaceAll("[#*`_-]", "")
                .replaceAll("```[a-zA-Z]*", "")
                .trim();
        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "GEMINI_SPEECH");
    }

    @Override
    public void onCopy(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Gemini Respuesta", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShare(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Compartir con..."));
    }

    @Override
    public void onGmail(String text) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Síntesis GeminiSync - " + dateStr);
        emailIntent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(emailIntent, "Crear borrador en Gmail..."));
        } catch (Exception e) {
            Toast.makeText(this, "No se encontró cliente de correo instalado.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCanvas(String text) {
        showCanvasDialog(text);
    }

    private void showCanvasDialog(String content) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_canvas, null);

        TextView tvContent = view.findViewById(R.id.tvCanvasContent);
        tvContent.setText(content);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view)
                .create();

        view.findViewById(R.id.btnCanvasClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnCanvasCopy).setOnClickListener(v -> {
            onCopy(content);
        });
        view.findViewById(R.id.btnCanvasShare).setOnClickListener(v -> {
            onShare(content);
        });
        view.findViewById(R.id.btnCanvasGmail).setOnClickListener(v -> {
            onGmail(content);
        });

        dialog.show();
    }

    // --- Intent entrante (Recibir contenido compartido) ---
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    etMessageInput.setText(sharedText);
                }
            } else if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    processSelectedImageUri(imageUri);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    // --- Sincronización Automática con Antigravity ---
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
                        tvSyncStatus.setText("● " + currentModeName);
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
                        tvSyncStatus.setText("● " + currentModeName);
                        if (!silent) {
                            Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                isSyncing = false;
                progressBar.setVisibility(View.GONE);
                tvSyncStatus.setText("● " + currentModeName);
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
        com.google.android.material.button.MaterialButton btnTestKey = dialogView.findViewById(R.id.btnTestKey);
        TextView tvTestStatus = dialogView.findViewById(R.id.tvTestStatus);

        etApiKey.setText(configManager.getApiKey());
        etRepoName.setText(configManager.getGithubRepo());
        etGithubToken.setText(configManager.getGithubToken());
        cbAutoSync.setChecked(configManager.isAutoSyncEnabled());

        btnTestKey.setOnClickListener(v -> {
            String testKey = etApiKey.getText().toString().trim();
            if (testKey.isEmpty()) {
                tvTestStatus.setVisibility(View.VISIBLE);
                tvTestStatus.setText("❌ Ingresa una clave antes de probar.");
                tvTestStatus.setTextColor(android.graphics.Color.parseColor("#FF5252"));
                return;
            }
            tvTestStatus.setVisibility(View.VISIBLE);
            tvTestStatus.setText("⏳ Probando conexión con Gemini...");
            tvTestStatus.setTextColor(android.graphics.Color.parseColor("#8AB4F8"));

            geminiClient.testConnection(testKey, new GeminiApiClient.GeminiCallback() {
                @Override
                public void onSuccess(String reply) {
                    tvTestStatus.setText("✅ ¡Conexión exitosa! Modelos activos y listos.");
                    tvTestStatus.setTextColor(android.graphics.Color.parseColor("#81C995"));
                }

                @Override
                public void onError(String error) {
                    tvTestStatus.setText("❌ " + error);
                    tvTestStatus.setTextColor(android.graphics.Color.parseColor("#FF5252"));
                }
            });
        });

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
        saveCurrentSessionToDb();
        if (configManager.isAutoSyncEnabled() && messageList.size() >= 3) {
            triggerAutoSync(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
