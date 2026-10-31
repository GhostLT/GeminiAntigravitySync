@echo off
chcp 65001 >nul
title Subir GeminiAntigravitySync a GitHub
color 0A

echo ===================================================================
echo     Subiendo GeminiAntigravitySync a GitHub (GhostLT)
echo ===================================================================
echo.

cd /d "%~dp0"

echo [+] Preparando archivos para commit...
git add .
git commit -m "feat: Aplicacion nativa Android GeminiSync con auto-sincronizacion de memoria Antigravity" >nul 2>&1

echo [+] Subiendo cambios a GitHub (git@github.com:GhostLT/GeminiAntigravitySync.git)...
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================================
    echo  ✅ ¡Proyecto subido exitosamente a GitHub!
    echo  🔗 URL: https://github.com/GhostLT/GeminiAntigravitySync
    echo ===================================================================
) else (
    echo.
    echo [!] Si el repositorio no existe en GitHub:
    echo     1. Entra a https://github.com/new
    echo     2. Nombre del repositorio: GeminiAntigravitySync
    echo     3. Deja desmarcado "Initialize with README" y dale Crear.
    echo     4. Vuelve a ejecutar este script.
)

echo.
pause
