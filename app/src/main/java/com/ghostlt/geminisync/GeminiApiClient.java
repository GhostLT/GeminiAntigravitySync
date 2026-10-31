package com.ghostlt.geminisync;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiClient {
    public interface GeminiCallback {
        void onSuccess(String reply);
        void onError(String error);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    public GeminiApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void sendMessage(List<ChatMessage> history, String userPrompt, String imageBase64, String modeInstruction, String apiKey, GeminiCallback callback) {
        String[] models = new String[]{"gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash"};
        tryModel(history, userPrompt, imageBase64, modeInstruction, apiKey, models, 0, callback);
    }

    private void tryModel(List<ChatMessage> history, String userPrompt, String imageBase64, String modeInstruction, String apiKey, String[] models, int modelIndex, GeminiCallback callback) {
        if (modelIndex >= models.length) {
            postError(callback, "Error: No se pudo conectar con los modelos de Gemini. Verifica tu API Key y conexión.");
            return;
        }

        String model = models[modelIndex];
        String url = BASE_URL + model + ":generateContent?key=" + apiKey;

        JsonObject root = new JsonObject();

        // System Instruction si hay un modo especial activo
        if (modeInstruction != null && !modeInstruction.isEmpty()) {
            JsonObject sysInst = new JsonObject();
            JsonArray sysParts = new JsonArray();
            JsonObject p = new JsonObject();
            p.addProperty("text", modeInstruction);
            sysParts.add(p);
            sysInst.add("parts", sysParts);
            root.add("systemInstruction", sysInst);
        }

        JsonArray contents = new JsonArray();

        // Agregar historial
        if (history != null) {
            for (ChatMessage msg : history) {
                JsonObject entry = new JsonObject();
                entry.addProperty("role", msg.isUser() ? "user" : "model");
                JsonArray parts = new JsonArray();

                // Si tenía imagen previa
                if (msg.hasImage()) {
                    JsonObject imgPart = new JsonObject();
                    JsonObject inlineData = new JsonObject();
                    inlineData.addProperty("mime_type", "image/jpeg");
                    inlineData.addProperty("data", msg.getImageBase64());
                    imgPart.add("inline_data", inlineData);
                    parts.add(imgPart);
                }

                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", msg.getText());
                parts.add(textPart);

                entry.add("parts", parts);
                contents.add(entry);
            }
        }

        // Agregar mensaje actual
        if (userPrompt != null && !userPrompt.isEmpty()) {
            JsonObject currentEntry = new JsonObject();
            currentEntry.addProperty("role", "user");
            JsonArray parts = new JsonArray();

            // Adjuntar imagen actual si existe
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                JsonObject imgPart = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mime_type", "image/jpeg");
                inlineData.addProperty("data", imageBase64);
                imgPart.add("inline_data", inlineData);
                parts.add(imgPart);
            }

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", userPrompt);
            parts.add(textPart);

            currentEntry.add("parts", parts);
            contents.add(currentEntry);
        }

        root.add("contents", contents);

        RequestBody body = RequestBody.create(gson.toJson(root), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                tryModel(history, userPrompt, imageBase64, modeInstruction, apiKey, models, modelIndex + 1, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    tryModel(history, userPrompt, imageBase64, modeInstruction, apiKey, models, modelIndex + 1, callback);
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    if (json.has("candidates")) {
                        JsonArray candidates = json.getAsJsonArray("candidates");
                        if (candidates.size() > 0) {
                            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                            JsonObject contentObj = firstCandidate.getAsJsonObject("content");
                            JsonArray parts = contentObj.getAsJsonArray("parts");
                            StringBuilder replyBuilder = new StringBuilder();
                            for (JsonElement p : parts) {
                                if (p.getAsJsonObject().has("text")) {
                                    replyBuilder.append(p.getAsJsonObject().get("text").getAsString());
                                }
                            }
                            postSuccess(callback, replyBuilder.toString().trim());
                            return;
                        }
                    }
                    tryModel(history, userPrompt, imageBase64, modeInstruction, apiKey, models, modelIndex + 1, callback);
                } catch (Exception e) {
                    tryModel(history, userPrompt, imageBase64, modeInstruction, apiKey, models, modelIndex + 1, callback);
                }
            }
        });
    }

    private void postSuccess(GeminiCallback callback, String reply) {
        mainHandler.post(() -> callback.onSuccess(reply));
    }

    private void postError(GeminiCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
