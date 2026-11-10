# 7xqer — Minecraft 1.21.11 / Fabric

Dies ist eine eigenständige Mod ohne mitgelieferte Resourcepack-Texturen. Die Oberfläche zeigt **PvP**, **Blöcke**, **Gear**, **HUD**, **Misc** und **Effekte**. Drücke im Spiel **O**, wähle pro Kategorie ein Pack und exportiere mit **Pack erstellen** ein zusammengesetztes Ressourcenpaket.

## Eigene Packs hinzufügen

- Öffne den Editor mit **O** und klicke **Pack hochladen**, um eine oder mehrere Resourcepack-ZIPs auszuwählen. Alternativ ziehe ZIP-Packs aus dem Explorer auf das Spiel-Fenster oder kopiere ZIPs beziehungsweise Resourcepack-Ordner in `resourcepacks`.
- Wähle in jeder Kategorie das Pack aus, dessen Texturen du dafür übernehmen möchtest.
- **Pack erstellen** legt `7xqer-style.zip` im `resourcepacks`-Ordner ab. Aktiviere das erstellte Pack danach unter **Optionen → Ressourcenpakete**.

Die Kategorien werden anhand der Resourcepack-Pfade und Dateinamen zugeordnet. Für gemischte Packs gilt: Wenn zwei ausgewählte Kategorien dieselbe Datei enthalten, überschreibt die zuletzt exportierte Auswahl diese Datei.

## JAR bauen

Doppelklicke auf `build.bat`. Beim ersten Start lädt das Skript JDK 25 und Gradle herunter; der Build braucht Internet. Die JAR liegt anschließend unter `build/libs/7xqer-1.0.0.jar`. Kopiere sie in den `mods`-Ordner deiner Fabric-Installation für Minecraft 1.21.11.

Der Quellcode enthält weder die Texturen noch den Programmcode aus der angehängten Fremdmod. Die angehängte ZIP enthielt nur eine fertige JAR.
