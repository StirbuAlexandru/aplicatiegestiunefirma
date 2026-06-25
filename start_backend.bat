@echo off
echo Pornire Backend Gestiune Firma...

:: 1. Verificam daca Python este instalat
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo EROARE: Python nu este instalat sau nu este in PATH!
    pause
    exit /b
)

:: 2. Pornim serverul FastAPI intr-o fereastra noua (cu secretele incarcate din set_secrets.bat)
echo Se porneste serverul FastAPI pe portul 8000...
start cmd /k "call set_secrets.bat && python main.py"

:: 3. Asteptam 3 secunde sa porneasca motorul
timeout /t 3

:: 4. Pornim ngrok
echo Se porneste tunelul ngrok...
ngrok http --domain=registrational-jessenia-sleevelike.ngrok-free.dev 8000

pause
