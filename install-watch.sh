#!/usr/bin/env bash
# ==============================================================================
# Google Pixel Watch 3 - RT Stralingstijd App Installer
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK="$DIR/dist/stralingstijd-wear.apk"
PARENT_ROOT="$(cd "$DIR/.." && pwd)"

# Find or setup ADB
if command -v adb >/dev/null 2>&1; then
    ADB="adb"
elif [ -x "$PARENT_ROOT/focus-watch/tools/platform-tools/adb" ]; then
    ADB="$PARENT_ROOT/focus-watch/tools/platform-tools/adb"
elif [ -x "$DIR/tools/platform-tools/adb" ]; then
    ADB="$DIR/tools/platform-tools/adb"
else
    echo "⚠️ ADB (Android Debug Bridge) is niet gevonden op je systeem."
    exit 1
fi

echo "===================================================================="
echo "    RT STRALINGSTIJD & TIMER - GOOGLE PIXEL WATCH 3 INSTALLER"
echo "===================================================================="
echo ""
echo "Volg deze stappen op je Google Pixel Watch 3:"
echo " 1. Ga naar Instellingen (tandwiel) op je horloge."
echo " 2. Scroll naar: Systeem -> Info -> Versies."
echo " 3. Tik 7 keer achter elkaar op 'Build-nummer' totdat er staat:"
echo "    'U bent nu ontwikkelaar!'."
echo " 4. Ga terug naar: Instellingen -> Ontwikkelaarsopties (onderaan)."
echo " 5. Schakel 'ADB-foutopsporing' IN."
echo " 6. Schakel 'Draadloos foutopsporing' IN (bevestig eventueel met 'Altijd toestaan')."
echo " 7. Zorg dat je Pixel Watch 3 met hetzelfde Wi-Fi netwerk verbonden is."
echo "===================================================================="
echo ""

# Build APK if not present
if [ ! -f "$APK" ]; then
    echo "APK niet gevonden, bouwen..."
    "$DIR/tools/build-apk.sh"
fi

if [ -n "$1" ]; then
    CONNECT_ADDR="$1"
    echo "Verbinden met opgegeven adres $CONNECT_ADDR..."
    "$ADB" connect "$CONNECT_ADDR"
else
    echo "Kies je verbindingsmethode:"
    echo " 1) Mijn horloge vraagt een koppelingscode (Eerste keer / 'Nieuw apparaat koppelen')"
    echo " 2) Direct verbinden met IP en Poort (Horloge is al eerder gekoppeld)"
    echo ""

    METHOD=""
    while [ "$METHOD" != "1" ] && [ "$METHOD" != "2" ]; do
        read -p "Keuze [1 of 2]: " METHOD
    done

    if [ "$METHOD" = "1" ]; then
        echo ""
        echo "--------------------------------------------------------------------"
        echo " STAP 1: KOPPELEN (Pairing)"
        echo " Tik op je horloge op: 'Nieuw apparaat koppelen' (Pair new device)."
        echo " LET OP: Laat dit venster OPEN staan op je horloge tijdens het invoeren!"
        echo " Je ziet twee regels:"
        echo "   A) Wi-Fi-koppelingscode (6 cijfers, bijv. 729104)"
        echo "   B) IP-adres en poort (bijv. 192.168.2.17:33973)"
        echo "--------------------------------------------------------------------"
        
        PAIR_SUCCESS=0
        while [ $PAIR_SUCCESS -eq 0 ]; do
            PAIR_ADDR=""
            while [ -z "$PAIR_ADDR" ]; do
                read -p "Voer IP en Koppel-poort in (bijv. 192.168.2.17:33973): " PAIR_ADDR
            done

            PAIR_CODE=""
            while [ -z "$PAIR_CODE" ]; do
                read -p "Voer de 6-cijferige Koppelcode in (bijv. 729104): " PAIR_CODE
            done

            if [[ "$PAIR_CODE" == *":"* ]] && [[ "$PAIR_ADDR" != *":"* ]]; then
                TMP="$PAIR_ADDR"
                PAIR_ADDR="$PAIR_CODE"
                PAIR_CODE="$TMP"
            fi

            echo ""
            echo "Koppelen met $PAIR_ADDR met code $PAIR_CODE..."
            set +e
            PAIR_OUTPUT=$("$ADB" pair "$PAIR_ADDR" "$PAIR_CODE" 2>&1)
            PAIR_EXIT=$?
            set -e
            echo "$PAIR_OUTPUT"

            if [ $PAIR_EXIT -eq 0 ] && [[ "$PAIR_OUTPUT" == *"Successfully paired"* ]]; then
                PAIR_SUCCESS=1
                echo "✓ Succesvol gekoppeld!"
            else
                echo ""
                echo "⚠️ Koppelen mislukt. Zorg dat het koppelvenstertje open blijft op je watch."
                read -p "Opnieuw proberen? (j/n): " RETRY
                if [ "$RETRY" != "j" ] && [ "$RETRY" != "J" ]; then
                    exit 1
                fi
            fi
        done
        
        echo ""
        echo "--------------------------------------------------------------------"
        echo " STAP 2: VERBINDEN (Connect)"
        echo " Sluit het koppelvenstertje op je horloge (tik op OK / vinkje)."
        echo " Kijk naar het 'IP-adres en poort' op het hoofdscherm van Draadloos foutopsporing."
        echo "--------------------------------------------------------------------"
        
        CONNECT_ADDR=""
        while [ -z "$CONNECT_ADDR" ]; do
            read -p "Voer IP-adres en Verbindings-poort in (bijv. 192.168.2.17:41235): " CONNECT_ADDR
        done
        
        echo "Verbinden met $CONNECT_ADDR..."
        "$ADB" connect "$CONNECT_ADDR"
    else
        CONNECT_ADDR=""
        while [ -z "$CONNECT_ADDR" ]; do
            read -p "Voer IP-adres en Poort in (bijv. 192.168.2.17:41235): " CONNECT_ADDR
        done
        echo "Verbinden met $CONNECT_ADDR..."
        "$ADB" connect "$CONNECT_ADDR"
    fi
fi

echo ""
echo "Apparatenlijst controleren..."
"$ADB" devices -l

TARGET_DEVICE="$CONNECT_ADDR"
if [ -z "$TARGET_DEVICE" ]; then
    TARGET_DEVICE=$("$ADB" devices | grep -E "\s+device$" | head -n 1 | awk '{print $1}')
fi

echo ""
echo "Bezig met installeren van RT Stralingstijd op je Google Pixel Watch 3 ($TARGET_DEVICE)..."
"$ADB" -s "$TARGET_DEVICE" install -r -t "$APK"

echo ""
echo "App starten op het horlogescherm..."
"$ADB" -s "$TARGET_DEVICE" shell am start -n com.rt.stralingstijdwatch/.MainActivity

echo ""
echo "===================================================================="
echo " GEFELICITEERD! Stralingstijd is succesvol geïnstalleerd op je Pixel Watch 3!"
echo "===================================================================="
