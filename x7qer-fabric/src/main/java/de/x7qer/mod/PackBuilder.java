package de.x7qer.mod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Combines selected folders/ZIPs from the player's resourcepacks directory. */
final class PackBuilder {
    private static final Map<String, String> CHOICES = new LinkedHashMap<>();
    private static final Map<String, Path> FOUND = new LinkedHashMap<>();
    private PackBuilder() { }

    static List<String> findPackNames(Path root) {
        FOUND.clear();
        if (!Files.isDirectory(root)) return List.of();
        try (var items = Files.list(root)) {
            for (Path item : items.sorted().toList()) {
                if (item.getFileName().toString().equalsIgnoreCase("7xqer-style.zip")) continue;
                if (Files.isDirectory(item) && findResourceRoot(item) != null) {
                    putUnique(displayName(item) + " (Ordner)", item);
                } else if (Files.isRegularFile(item) && item.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip") && isPackZip(item)) {
                    putUnique(displayName(item) + " (ZIP)", item);
                }
            }
        } catch (IOException ignored) { }
        return List.copyOf(FOUND.keySet());
    }

    private static void putUnique(String base, Path path) {
        String name = base;
        int n = 2;
        while (FOUND.containsKey(name)) name = base + " " + n++;
        FOUND.put(name, path);
    }

    private static boolean isPackZip(Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> it = zip.entries();
            while (it.hasMoreElements()) {
                String name = it.nextElement().getName().replace('\\', '/');
                if (name.equals("pack.mcmeta") || name.endsWith("/pack.mcmeta") || name.startsWith("assets/")) return true;
            }
        } catch (IOException ignored) { }
        return false;
    }

    private static String displayName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static Path findResourceRoot(Path folder) {
        if (Files.isDirectory(folder.resolve("assets")) || Files.isRegularFile(folder.resolve("pack.mcmeta"))) return folder;
        try (var children = Files.list(folder)) {
            return children.filter(Files::isDirectory)
                    .filter(p -> Files.isDirectory(p.resolve("assets")) || Files.isRegularFile(p.resolve("pack.mcmeta")))
                    .findFirst().orElse(null);
        } catch (IOException ignored) { return null; }
    }

    static void setChoice(String category, String pack) { CHOICES.put(category, pack); }
    static String getChoice(String category) { return CHOICES.get(category); }

    static int importPacks(List<Path> dropped, Path resourcePackDirectory) throws IOException {
        Files.createDirectories(resourcePackDirectory);
        int imported = 0;
        for (Path source : dropped) {
            if (source == null || !Files.exists(source)) continue;
            String fileName = source.getFileName().toString();
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (Files.isRegularFile(source) && lower.endsWith(".zip") && isPackZip(source)) {
                Path target = uniqueTarget(resourcePackDirectory, fileName);
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                imported++;
            } else if (Files.isDirectory(source) && findResourceRoot(source) != null) {
                Path target = uniqueTarget(resourcePackDirectory, fileName);
                copyDirectory(source, target);
                imported++;
            }
        }
        return imported;
    }

    private static Path uniqueTarget(Path root, String name) {
        Path target = root.resolve(name);
        int n = 2;
        String base = name;
        String extension = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0 && !Files.isDirectory(root.resolve(name))) {
            base = name.substring(0, dot);
            extension = name.substring(dot);
        }
        while (Files.exists(target)) target = root.resolve(base + " (" + n++ + ")" + extension);
        return target;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path item : paths.toList()) {
                Path relative = source.relativize(item);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(item)) Files.createDirectories(destination);
                else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(item, destination);
                }
            }
        }
    }

    static Path build(Path root) throws IOException {
        findPackNames(root);
        if (FOUND.isEmpty()) throw new IOException("Füge zuerst Packs hinzu (ZIP auf das Editorfenster ziehen).");
        Map<String, byte[]> outputFiles = new TreeMap<>();
        for (var selection : CHOICES.entrySet()) {
            Path pack = FOUND.get(selection.getValue());
            if (pack == null) continue;
            if (Files.isDirectory(pack)) readFolder(pack, selection.getKey(), outputFiles);
            else readZip(pack, selection.getKey(), outputFiles);
        }
        if (outputFiles.isEmpty()) throw new IOException("Wähle zuerst in mindestens einer Kategorie ein Pack aus.");

        String metadata = "{\n  \"pack\": {\n    \"pack_format\": 75,\n    \"description\": \"7xqer - eigenes Ressourcenpaket\"\n  }\n}\n";
        outputFiles.put("pack.mcmeta", metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Path destination = root.resolve("7xqer-style.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination))) {
            for (var file : outputFiles.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return destination;
    }

    private static void readFolder(Path folder, String category, Map<String, byte[]> output) throws IOException {
        Path root = findResourceRoot(folder);
        if (root == null) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                if (isCategoryFile(relative, category)) output.put(relative, Files.readAllBytes(path));
            }
        }
    }

    private static void readZip(Path source, String category, Map<String, byte[]> output) throws IOException {
        try (ZipFile zip = new ZipFile(source.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String relative = toPackRelative(entry.getName());
                if (relative != null && isCategoryFile(relative, category)) {
                    try (InputStream in = zip.getInputStream(entry)) { output.put(relative, in.readAllBytes()); }
                }
            }
        }
    }

    private static String toPackRelative(String entry) {
        String normalized = entry.replace('\\', '/');
        if (normalized.startsWith("assets/")) return normalized;
        int assets = normalized.indexOf("/assets/");
        if (assets >= 0) return normalized.substring(assets + 1);
        return null;
    }

    private static boolean isCategoryFile(String path, String category) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("assets/")) return false;
        String[] parts = lower.split("/", 4);
        if (parts.length < 3) return false;
        String area = lower.substring(("assets/" + parts[1] + "/").length());
        String file = lower.substring(lower.lastIndexOf('/') + 1);
        boolean block = area.startsWith("textures/block/") || area.startsWith("blockstates/") || area.startsWith("models/block/");
        boolean gui = area.startsWith("textures/gui/") || area.startsWith("textures/font/") || area.startsWith("font/");
        boolean effect = area.startsWith("textures/particle/") || area.startsWith("particles/") || area.contains("explosion");
        boolean pvp = containsAny(file, "crystal", "obsidian", "totem", "anchor", "ender_pearl", "pearl", "golden_apple", "enchanted_golden_apple", "experience_bottle", "arrow");
        boolean gear = containsAny(file, "sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots", "shield", "trident", "bow", "crossbow", "mace", "spear");
        boolean itemArea = area.startsWith("textures/item/") || area.startsWith("items/") || area.startsWith("models/item/");

        return switch (category) {
            case "Blöcke" -> block;
            case "HUD" -> gui;
            case "Effekte" -> effect;
            case "PvP" -> itemArea && pvp;
            case "Gear" -> itemArea && gear;
            case "Misc" -> !block && !gui && !effect && !(itemArea && pvp) && !(itemArea && gear);
            default -> false;
        };
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }
}
