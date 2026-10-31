# 🚀 GeminiSync Pro (Asistente Multimodal y Sincronizador Antigravity)

Aplicación Android nativa de última generación desarrollada en Java y Material Components 3 para potenciar tu flujo de trabajo con **Google Gemini**, integrando visión computacional, dictado por voz, síntesis auditiva, modos avanzados y **sincronización automática de memoria técnica** con tu entorno local de **Antigravity en PC**.

---

## ✨ Nuevas Características Implementadas

### 1. 📎 Multimodalidad Total (Screenshots, Archivos y Cámara)
- **Adjuntar Capturas de Pantalla / Archivos**: Botón de clip (📎) para seleccionar cualquier imagen o screenshot desde tu galería. Se envía directamente a Gemini mediante `inline_data` Base64.
- **Cámara Instantánea**: Botón de cámara (📷) para tomar fotos en tiempo real y analizarlas con Gemini al instante.
- **Recepción desde otras apps**: La app aparece en el menú de "Compartir" de Android para recibir textos e imágenes de cualquier otra aplicación.

### 2. 🎙️ Dictado por Voz & 🔊 Texto a Voz (TTS)
- **Icono de Micrófono (Speech-to-Text)**: Toca el micrófono para dictar prompts largos sin escribir una sola letra.
- **Icono de Bocina (Text-to-Speech)**: Cada respuesta de Gemini cuenta con un botón de altavoz para escuchar la explicación en voz alta en español.

### 3. 🌱 Ramas de Conversación (Branching)
- **Botón Nueva Rama (+)**: Inicia una nueva sesión limpia. Automáticamente extrae y respalda el conocimiento de la sesión anterior en la memoria de tu PC antes de limpiar la pantalla.

### 4. 📤 Compartir & ✉️ Borrador en Gmail
- **Compartir con otras apps**: Envía cualquier solución o respuesta a WhatsApp, Telegram, Slack o notas con un solo toque.
- **Borrador en Gmail**: Genera al instante un correo en Gmail con el asunto y la solución técnica estructurada lista para enviar.

### 5. 💡 Modos Especiales de Inteligencia Artificial
Accede al menú **Modos** en la barra superior para alternar entre:
- 🔍 **Deep Research**: Modo de investigación exhaustiva y multidimensional. Desglosa arquitectura técnica, pros, contras, casos límite y estado del arte.
- 📝 **Canvas Mode**: Visor de pantalla completa para código fuente y documentación técnica sin omisiones, con botones directos para copiar y compartir.
- 🎓 **Aprendizaje Guiado (Tutor)**: Mentor socrático que enseña de forma progresiva, hace preguntas de comprobación y explica paso a paso.
- 🎨 **Crear Imágenes (Imagen 3)**: Generador de directivas y composiciones fotográficas hiperdetalladas optimizadas para modelos de difusión visual.
- 🎬 **Video & Música Studio**: Guiones cinematográficos para video generativo (Sora / Runway / Omni) y métricas líricas para composición musical (Suno / Udio).

### 6. ⚡ Sincronización Automática con tu PC (Zero-Touch)
- **Fase 1 (Extracción quirúrgica)**: Al salir de la app o tocar `⚡ Sync`, el motor destila las 4 secciones estándar (`system_profile`, `decisions`, `technical_solutions`, `lessons_learned`).
- **Fase 2 & 3 (Respaldo)**:
  - Guarda automáticamente en `/sdcard/Download/inbox_historial/sesion_YYYYMMDD_HHMMSS.md`.
  - Si configuras tu GitHub Token en Ajustes (⚙️), realiza un push directo vía GitHub API a tu repositorio.
  - Tu PC unifica la memoria cada noche a las 9:00 PM sin que muevas un dedo.

---

## 📦 Descarga e Instalación

El ejecutable compilado listo para instalar en tu teléfono se encuentra en tu Escritorio:
* **Ruta**: `C:\Users\PC\Desktop\GeminiSyncPro.apk` (o `GeminiSync.apk`)

---

## 🚀 Subir Cambios a GitHub (GhostLT)

Para subir todas estas nuevas implementaciones a tu GitHub:
1. Haz doble clic en el script:
   [`C:\proyectos\GeminiAntigravitySync\subir_a_github.bat`](file:///c:/proyectos/GeminiAntigravitySync/subir_a_github.bat)
2. El script empaqueta todos los cambios, realiza el commit semántico y ejecuta el push vía SSH a `git@github.com:GhostLT/GeminiAntigravitySync.git`.

---

## 👤 Autor
- **Desarrollador**: GhostLT (`viverosjavier86@gmail.com`)
- **Ecosistema**: Google Antigravity & Android Nativo
