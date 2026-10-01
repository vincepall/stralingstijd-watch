# RT Stralingstijd & Timer — Google Pixel Watch 3 & Wear OS App

Een speciaal ontwikkelde **Wear OS** applicatie voor de **Google Pixel Watch 3** (41mm en 45mm, 456×456 px) en andere Wear OS smartwatches, ontworpen voor Niet-Destructief Onderzoek (NDO / RT - Radiografisch Onderzoek).

De app berekent nauwkeurig de benodigde stralingstijd voor industriële radiografie (Ir-192 en Se-75) en beschikt over een geïntegreerde timer met slimme batterijbesparing, alarm en een snelle Wear OS tegel (Tile).

---


## ✨ Functionaliteiten

- **☢️ Nauwkeurige Stralingstijdberekening**:
  - Ondersteuning voor **Ir-192** en **Se-75**.
  - Automatische halveringstijd- en vervalberekening op basis van bron-invoerdatum en initiële activiteit (Curie).
  - Invoer voor wanddikte ($mm$), afstand ($cm$), materiaal en filmklasse/filmfactor.
  - Ingebouwde materiaaltabel (Staal, RVS, Koper, Titanium, Aluminium, etc.) met automatische halveringsdikte (HVL) en opbouwfactoren.
  - Lokale bronnendatabase (`bronnen.csv`) voor snelle selectie van actieve bronnen.

- **⏱️ Geïntegreerde Belichtingstimer**:
  - Start direct met één tik na de berekening.
  - Live aftellende timer in minuten en seconden.
  - Pauzeren, hervatten, stoppen en snelle `+30s` correctieknop.

- **🔋 Slimme AOD (Always-On Display) Batterijbesparing**:
  - Eerste **5 seconden** actief helder scherm om de start te controleren.
  - Schakelt tijdens lange belichtingstijden over naar een minimalistische, gedimde energiebesparende stand (OLED-geoptimaliseerd puur zwart).
  - Wordt automatisch weer wakker en helder in de **laatste 30 seconden**.
  - Tik op het scherm om op elk moment de bedieningsknoppen weer tevoorschijn te halen.

- **📳 Alert & Alarm**:
  - Duidelijke trilpatronen en visuele waarschuwing wanneer de belichtingstijd is verstreken.

- **🧩 Wear OS Tegel (Tile)**:
  - Veeg direct vanaf je wijzerplaat naar de Stralingstijd-tegel.
  - Snelle startknop om direct de laatst berekende timer te starten zonder eerst door menu's te hoeven navigeren.

- **⌚ Ergonomisch Horloge UI**:
  - Speciaal ontworpen voor ronde schermen (456×456 px).
  - Ingebouwd numeriek toetsenbord (numpad) met grote aanraakvlakken voor snelle en foutloze invoer met handschoenen/vingers in het veld.

---

## 🚀 Installatie op Google Pixel Watch 3

### Optie 1: Direct via Android Telefoon (Zonder PC) — *Aanbevolen*
1. Download het APK-bestand op je Android-smartphone:
   - **[⬇️ Download `dist/stralingstijd-wear.apk`](https://github.com/vincepall/stralingstijd-watch/releases/latest/download/stralingstijd-wear.apk)** (of vind deze in de repo onder `dist/`).
2. Installeer een Wear OS sideload app op je telefoon vanuit Google Play:
   - **GeminiMan Wear OS Manager** *(Aanbevolen)*
   - **Wear Installer 2**
   - **Bugjaeger Mobile ADB**
3. Koppel de app draadloos met je horloge en installeer direct de APK.

---

### Optie 2: Automatisch via PC/Mac (Wi-Fi ADB)

#### Op Linux / macOS:
```bash
./install-watch.sh
```

#### Op Windows:
Dubbelklik op **`install-watch.bat`** of voer uit in de opdrachtprompt:
```cmd
install-watch.bat
```

Het installatiescript helpt je stap voor stap:
1. Activeer ontwikkelaarsopties op je horloge: **Instellingen** ➔ **Systeem** ➔ **Info** ➔ **Versies** ➔ tik **7 keer** op **Build-nummer**.
2. Schakel onder **Ontwikkelaarsopties** zowel **ADB-foutopsporing** als **Draadloos foutopsporing** in.
3. Voer het IP-adres en de poort in wanneer het script erom vraagt. De app wordt automatisch geïnstalleerd en direct gestart!

---

### Optie 3: Handmatig via ADB CLI
```bash
# 1. Koppelen (indien eerste keer):
adb pair <WATCH_IP>:<PAIR_PORT> <PAIR_CODE>

# 2. Verbinden:
adb connect <WATCH_IP>:<CONNECT_PORT>

# 3. APK installeren:
adb install -r -t dist/stralingstijd-wear.apk

# 4. App starten:
adb shell am start -n com.rt.stralingstijdwatch/.MainActivity
```

---

## 🛠️ Zelf Bouwen vanuit Broncode

Het project vereist geen zware Gradle setup; het kan razendsnel worden gecompileerd met de standalone Android build-tools script:

```bash
# Zorg voor Android SDK (build-tools 34.0.0 & android-34) en JDK 17
./tools/build-apk.sh
```

De gecompileerde en ondertekende APK wordt geplaatst in `dist/stralingstijd-wear.apk`.

---

## 📁 Projectstructuur

```
stralingstijd-watch/
├── android/
│   └── app/src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── bronnen.csv           # RT bronnen configuratie & activiteit
│       ├── java/com/rt/stralingstijdwatch/
│       │   ├── MainActivity.java     # UI, timer, AOD beheer & berekeningen
│       │   ├── MaterialDb.java       # Materialen, HVL en opbouwfactoren
│       │   ├── SourceDb.java         # Bronnenbeheer & vervalberekening
│       │   ├── StralingstijdTileService.java # Wear OS Tile integratie
│       │   └── ImmediateFuture.java  # Async future helper voor Tiles
│       └── res/                      # Drawables, kleuren, strings
├── dist/
│   └── stralingstijd-wear.apk        # Kant-en-klare APK
├── tools/
│   └── build-apk.sh                  # Standalone APK build script
├── install-watch.sh                  # Wi-Fi installer voor Linux/macOS
├── install-watch.bat                 # Wi-Fi installer voor Windows
└── README.md
```

---

## 📄 Licentie

Vrij voor intern en professioneel gebruik binnen Radiografisch Onderzoek (RT / NDO).
