@echo off
setlocal enabledelayedexpansion
title RT Stralingstijd - Pixel Watch 3 Installer

set "DIR=%~dp0"
set "APK=%DIR%dist\stralingstijd-wear.apk"

echo ====================================================================
echo     RT STRALINGSTIJD & TIMER - GOOGLE PIXEL WATCH 3 INSTALLER
echo ====================================================================
echo.
echo Volg deze stappen op je Google Pixel Watch 3:
echo  1. Ga naar Instellingen (tandwiel) op je horloge.
echo  2. Scroll naar: Systeem -^> Info -^> Versies.
echo  3. Tik 7 keer achter elkaar op 'Build-nummer' tot je ontwikkelaar bent.
echo  4. Ga naar: Instellingen -^> Ontwikkelaarsopties.
echo  5. Schakel 'ADB-foutopsporing' IN.
echo  6. Schakel 'Draadloos foutopsporing' IN.
echo  7. Zorg dat je Pixel Watch 3 op hetzelfde Wi-Fi netwerk zit als deze pc.
echo ====================================================================
echo.

where adb >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%DIR%tools\platform-tools\adb.exe" (
        set "ADB=%DIR%tools\platform-tools\adb.exe"
    ) else (
        echo [!] ADB is niet gevonden op je computer.
        echo Installeer ADB via: winget install Google.PlatformTools
        echo of download Android SDK Platform-Tools:
        echo https://developer.android.com/tools/releases/platform-tools
        pause
        exit /b 1
    )
) else (
    set "ADB=adb"
)

if not exist "%APK%" (
    echo [!] APK bestand niet gevonden in: %APK%
    pause
    exit /b 1
)

pause
echo.
echo Kies je verbindingsmethode:
echo  1) Horloge vraagt een koppelingscode (Nieuw apparaat koppelen)
echo  2) Direct verbinden met IP en Poort (eerder al gekoppeld)
echo.
set /p METHOD="Keuze [1 of 2]: "

if "%METHOD%"=="1" (
    echo.
    echo STAP 1: KOPPELEN
    echo Tik op je horloge op 'Nieuw apparaat koppelen' en laat dat venster open!
    set /p PAIR_ADDR="Voer IP en Koppel-poort in (bijv. 192.168.1.50:33973): "
    set /p PAIR_CODE="Voer de 6-cijferige Koppelcode in: "
    "%ADB%" pair !PAIR_ADDR! !PAIR_CODE!
    echo.
    echo STAP 2: VERBINDEN
    echo Sluit het koppel-venstertje op je horloge en kijk naar het IP:poort op het hoofdscherm.
    set /p CONNECT_ADDR="Voer IP-adres en Verbindings-poort in (bijv. 192.168.1.50:41235): "
    "%ADB%" connect !CONNECT_ADDR!
) else (
    set /p CONNECT_ADDR="Voer IP-adres en Poort in (bijv. 192.168.1.50:5555): "
    "%ADB%" connect !CONNECT_ADDR!
)

echo.
echo Controleren van verbonden apparaten...
"%ADB%" devices -l

echo.
echo Installeren van RT Stralingstijd op je horloge...
"%ADB%" install -r -t "%APK%"

echo.
echo App starten op het horloge...
"%ADB%" shell am start -n com.rt.stralingstijdwatch/.MainActivity

echo.
echo ====================================================================
echo  GEFELICITEERD! De app is succesvol geinstalleerd op je Pixel Watch 3!
echo ====================================================================
pause
