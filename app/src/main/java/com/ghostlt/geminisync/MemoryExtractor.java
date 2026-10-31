package com.ghostlt.geminisync;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryExtractor {

    public interface ExtractionCallback {
        void onExtracted(String markdownSummary);
        void onError(String error);
    }

    private final GeminiApiClient apiClient;

    public MemoryExtractor(GeminiApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void extractMemory(List<ChatMessage> history, String apiKey, ExtractionCallback callback) {
        if (history == null || history.isEmpty()) {
            callback.onError("No hay conversación suficiente para extraer memoria técnica.");
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String extractionInstruction = "Actúa como un extractor técnico estricto para el sistema de memoria Antigravity. " +
                "Revisa todo el historial anterior de esta conversación y genera una síntesis técnica estructurada.\n\n" +
                "REGLAS:\n" +
                "1. Elimina por completo saludos, preámbulos, despedidas y charla conversacional.\n" +
                "2. Extrae únicamente el núcleo funcional.\n\n" +
                "FORMATO OBLIGATORIO (en Markdown):\n" +
                "# SÍNTESIS DE SESIÓN GEMINI MÓVIL\n" +
                "- Fecha: " + today + "\n" +
                "- Tema Principal: [Tema técnico central resuelto]\n" +
                "- Entorno: Android / Termux / Windows\n\n" +
                "## 1. Directrices y Perfil Operativo (system_profile)\n" +
                "- [Directrices, restricciones o convenciones acordadas]\n\n" +
                "## 2. Decisiones de Arquitectura (decisions)\n" +
                "- **[Decisión]**: [Justificación breve]\n\n" +
                "## 3. Registro de Soluciones Técnicas (technical_solutions)\n" +
                "### [Nombre de la solución/script]\n" +
                "- Descripción: [Breve resumen]\n" +
                "- Código / Comandos:\n```\n[Código o snippets exactos]\n```\n\n" +
                "## 4. Lecciones Aprendidas (lessons_learned)\n" +
                "- **Error**: [Problema resuelto]\n" +
                "- **Solución**: [Cómo se arregló]\n\n" +
                "Devuelve ÚNICAMENTE este formato Markdown, sin texto adicional.";

        apiClient.sendMessage(history, extractionInstruction, null, null, apiKey, new GeminiApiClient.GeminiCallback() {
            @Override
            public void onSuccess(String reply) {
                callback.onExtracted(reply);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
