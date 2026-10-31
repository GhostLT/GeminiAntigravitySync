# 🚀 GeminiSync Android (Unificador Automático Antigravity)

Aplicación Android nativa desarrollada en Java y Material Components 3 para chatear con la API oficial de **Google Gemini** y **sincronizar automáticamente la memoria técnica** con tu entorno local de **Antigravity en PC**.

---

## 📱 ¿Qué hace esta aplicación?

1. **Chat de Inteligencia Artificial con Gemini**:
   - Conexión directa y fluida a los modelos más veloces de Google: `gemini-2.5-flash`, `gemini-2.0-flash` y `gemini-1.5-flash` con conmutación por error (auto-fallback).
   - Historial conversacional interactivo con diseño oscuro moderno.

2. **Extracción y Depuración Automatizada (Fase 1)**:
   - Al finalizar un tema o salir de la aplicación, el motor de extracción destila quirúrgicamente el núcleo técnico de la conversación eliminando saludos, cortesías y charla.
   - Clasifica la información en las 4 secciones estándar del ecosistema Antigravity:
     - `system_profile` (directrices y entorno)
     - `decisions` (decisiones de arquitectura ADR)
     - `technical_solutions` (código, scripts y snippets funcionales)
     - `lessons_learned` (errores resueltos e incompatibilidades)

3. **Sincronización de Cero Clics (Fase 2 y 3)**:
   - **Local / Termux**: Guarda automáticamente el archivo `.md` generado en `/sdcard/Download/inbox_historial/sesion_YYYYMMDD_HHMMSS.md` para que Termux o tu sincronización de nube lo tenga sin tocar nada.
   - **Push Directo a GitHub (Zero-Touch)**: Si configuras un GitHub Personal Access Token (PAT), la app realiza un `PUT` a la API de GitHub y sube el archivo directamente a tu repositorio (`.agent/memory/inbox_historial/`).
   - La automatización de las 9:00 PM de Antigravity en tu PC lo toma, clasifica y unifica en tu base de conocimiento.

---

## 🛠️ Requisitos Técnicos

- **Android 8.0+** (API 26 mínima, SDK 34 compatible).
- **Google Gemini API Key** (Gratis en [Google AI Studio](https://aistudio.google.com/)).
- **GitHub Token** (Opcional, solo si deseas push remoto directo sin cables).

---

## ⚙️ Configuración en la App

1. Abre la aplicación en tu teléfono Android.
2. Toca el ícono de engranaje (⚙️) en la barra superior.
3. Ingresa tu **Gemini API Key**.
4. *(Opcional)* Ingresa tu **GitHub Personal Access Token** y el nombre de tu repositorio (`GhostLT/cool-newton`).
5. Mantén marcado **"Auto-sincronizar automáticamente al salir del chat"**.

A partir de ahí, solo consulta tus dudas de código y arquitectura en la app. La memoria de tu PC se mantendrá unificada automáticamente.

---

## 📦 Compilación y APK

El ejecutable listo para instalar se encuentra disponible en:
- `app/build/outputs/apk/debug/app-debug.apk`
- Copia directa en el Escritorio: `GeminiSync.apk`

---

## 👤 Autor
- **Desarrollador**: GhostLT (`viverosjavier86@gmail.com`)
- **Ecosistema**: Google Antigravity & Termux
