package com.ghostlt.geminisync;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GitHubSyncManager {

    public interface SyncCallback {
        void onSuccess(String detail);
        void onError(String error);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Context context;
    private final ConfigManager config;
    private final OkHttpClient client;
    private final Handler mainHandler;

    public GitHubSyncManager(Context context, ConfigManager config) {
        this.context = context;
        this.config = config;
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void syncSession(String markdownContent, SyncCallback callback) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "sesion_" + timestamp + ".md";

        // 1. Guardar siempre localmente en carpeta de descargas (accesible por Termux y usuario)
        File localSavedFile = saveToLocalStorage(fileName, markdownContent);

        // 2. Si hay token de GitHub configurado, sincronizar directamente via GitHub REST API
        String token = config.getGithubToken();
        String repo = config.getGithubRepo();

        if (token != null && !token.trim().isEmpty() && repo != null && !repo.trim().isEmpty()) {
            pushToGitHub(repo, fileName, markdownContent, token, callback);
        } else {
            String pathMsg = localSavedFile != null ? localSavedFile.getAbsolutePath() : "Almacenamiento local";
            mainHandler.post(() -> callback.onSuccess("Guardado automáticamente en móvil: " + pathMsg));
        }
    }

    private File saveToLocalStorage(String fileName, String content) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File inboxDir = new File(downloadsDir, "inbox_historial");
            if (!inboxDir.exists()) {
                inboxDir.mkdirs();
            }
            File targetFile = new File(inboxDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return targetFile;
        } catch (Exception e) {
            // Fallback a almacenamiento interno de la app
            try {
                File internalDir = new File(context.getFilesDir(), "inbox_historial");
                if (!internalDir.exists()) internalDir.mkdirs();
                File targetFile = new File(internalDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    fos.write(content.getBytes(StandardCharsets.UTF_8));
                }
                return targetFile;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private void pushToGitHub(String repo, String fileName, String content, String token, SyncCallback callback) {
        String url = "https://api.github.com/repos/" + repo + "/contents/.agent/memory/inbox_historial/" + fileName;

        String base64Content = Base64.encodeToString(content.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("message", "docs(memory): sync automatica sesion movil " + fileName);
        bodyJson.addProperty("content", base64Content);

        RequestBody body = RequestBody.create(bodyJson.toString(), JSON);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token.trim())
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("User-Agent", "GeminiAntigravitySync-Android")
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onSuccess("Guardado localmente. (Aviso: GitHub sin conexión: " + e.getMessage() + ")"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess("¡Pushed directamente a GitHub (" + repo + ") y guardado local!"));
                } else {
                    String err = response.body() != null ? response.body().string() : response.message();
                    mainHandler.post(() -> callback.onSuccess("Guardado localmente. (Aviso GitHub " + response.code() + ": " + err + ")"));
                }
            }
        });
    }
}
