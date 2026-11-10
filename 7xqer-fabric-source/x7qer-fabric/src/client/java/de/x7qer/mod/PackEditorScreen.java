package de.x7qer.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/** Clean, client-only pack editor: no texture assets are bundled in this mod. */
public final class PackEditorScreen extends Screen {
    private record Category(String name, Item icon) { }
    private static final List<Category> CATEGORIES = List.of(
            new Category("PvP", Items.END_CRYSTAL),
            new Category("Blöcke", Items.OBSIDIAN),
            new Category("Gear", Items.IRON_CHESTPLATE),
            new Category("HUD", Items.TARGET),
            new Category("Misc", Items.ENCHANTED_BOOK),
            new Category("Effekte", Items.FIREWORK_ROCKET)
    );

    private final int page; // 0 = home, 1..6 = category, 7 = imported packs
    private String status = "Ziehe eigene ZIP-Packs auf dieses Fenster.";

    public PackEditorScreen() { this(0); }
    private PackEditorScreen(int page) {
        super(Component.literal("7xqer Pack Editor"));
        this.page = page;
    }

    @Override protected void init() {
        clearWidgets();
        if (page == 0) {
            int gap = 10;
            int cardWidth = Math.min(190, Math.max(96, (width - 48 - gap * 2) / 3));
            int total = cardWidth * 3 + gap * 2;
            int left = (width - total) / 2;
            int top = Math.max(66, height / 2 - 118);
            int bottomY = height - 38;
            int buttonLeft = width / 2 - 239;
            addRenderableWidget(Button.builder(Component.literal("Meine Packs"), b -> minecraft.setScreen(new PackEditorScreen(7)))
                    .bounds(buttonLeft, bottomY, 92, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Pack hochladen"), b -> choosePacks())
                    .bounds(buttonLeft + 97, bottomY, 105, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Pack erstellen"), b -> exportPack())
                    .bounds(buttonLeft + 207, bottomY, 105, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Ordner"), b -> openPackFolder())
                    .bounds(buttonLeft + 317, bottomY, 68, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                    .bounds(buttonLeft + 390, bottomY, 62, 24).build());
        } else if (page >= 1 && page <= CATEGORIES.size()) {
            List<String> packs = PackBuilder.findPackNames(Minecraft.getInstance().getResourcePackDirectory());
            int show = Math.min(packs.size(), Math.max(1, Math.min(9, (height - 140) / 32)));
            for (int i = 0; i < show; i++) {
                String pack = packs.get(i);
                addRenderableWidget(Button.builder(Component.literal(pack), b -> {
                    PackBuilder.setChoice(CATEGORIES.get(page - 1).name(), pack);
                    status = CATEGORIES.get(page - 1).name() + " ← " + pack;
                    minecraft.setScreen(new PackEditorScreen());
                }).bounds(width / 2 - 150, 86 + i * 32, 300, 27).build());
            }
            if (packs.isEmpty()) status = "Keine Packs gefunden. Ziehe eine Pack-ZIP hier hinein.";
            addRenderableWidget(Button.builder(Component.literal("Packs-Ordner öffnen"), b -> openPackFolder())
                    .bounds(width / 2 - 106, height - 40, 130, 26).build());
            addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> minecraft.setScreen(new PackEditorScreen()))
                    .bounds(width / 2 + 30, height - 40, 76, 26).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Packs-Ordner öffnen"), b -> openPackFolder())
                    .bounds(width / 2 - 112, height - 40, 132, 26).build());
            addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> minecraft.setScreen(new PackEditorScreen()))
                    .bounds(width / 2 + 26, height - 40, 76, 26).build());
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        g.fill(12, 12, width - 12, height - 12, 0x77303030);
        g.drawCenteredString(font, "7xqer", width / 2, 22, 0xFFFFFFFF);
        g.drawCenteredString(font, page == 0 ? "Pack Editor" : page <= CATEGORIES.size()
                ? "Kategorie: " + CATEGORIES.get(page - 1).name() : "Meine Packs", width / 2, 40, 0xFFE4E4E4);

        if (page == 0) renderCategories(g, mouseX, mouseY);
        else if (page == 7) renderPacks(g);
        g.drawCenteredString(font, status, width / 2, height - 58, 0xFFFFFFFF);
        super.render(g, mouseX, mouseY, delta);
    }

    private void renderCategories(GuiGraphics g, int mouseX, int mouseY) {
        int gap = 10;
        int cardWidth = Math.min(190, Math.max(96, (width - 48 - gap * 2) / 3));
        int cardHeight = 76;
        int total = cardWidth * 3 + gap * 2;
        int left = (width - total) / 2;
        int top = Math.max(66, height / 2 - 118);

        for (int i = 0; i < CATEGORIES.size(); i++) {
            int x = left + (i % 3) * (cardWidth + gap);
            int y = top + (i / 3) * (cardHeight + gap);
            boolean hover = mouseX >= x && mouseX < x + cardWidth && mouseY >= y && mouseY < y + cardHeight;
            g.fill(x, y, x + cardWidth, y + cardHeight, hover ? 0xB04B4353 : 0x90404040);
            int edge = hover ? 0xFFEBD6FF : 0xFFB7B7B7;
            g.fill(x, y, x + cardWidth, y + 1, edge);
            g.fill(x, y + cardHeight - 1, x + cardWidth, y + cardHeight, edge);
            g.fill(x, y, x + 1, y + cardHeight, edge);
            g.fill(x + cardWidth - 1, y, x + cardWidth, y + cardHeight, edge);
            g.renderItem(new ItemStack(CATEGORIES.get(i).icon()), x + cardWidth / 2 - 8, y + 7);
            g.drawCenteredString(font, CATEGORIES.get(i).name(), x + cardWidth / 2, y + 31, 0xFFFFFFFF);
            String selected = PackBuilder.getChoice(CATEGORIES.get(i).name());
            String label = selected == null ? "Pack auswählen" : trim(selected, Math.max(8, cardWidth / 6));
            g.drawCenteredString(font, label, x + cardWidth / 2, y + 49, 0xFFD5D5D5);
        }
        g.drawCenteredString(font, "Klicke eine Kategorie oder ziehe ZIP-Packs hier hinein.", width / 2, top + 2 * (cardHeight + gap) + 2, 0xFFDDDDDD);
    }

    private void renderPacks(GuiGraphics g) {
        List<String> packs = PackBuilder.findPackNames(Minecraft.getInstance().getResourcePackDirectory());
        if (packs.isEmpty()) {
            g.drawCenteredString(font, "Noch keine eigenen Packs hinzugefügt.", width / 2, height / 2 - 8, 0xFFFFFFFF);
            g.drawCenteredString(font, "Ziehe eine ZIP auf den Pack Editor oder kopiere sie in resourcepacks.", width / 2, height / 2 + 10, 0xFFDDDDDD);
            return;
        }
        int y = 76;
        for (String pack : packs.stream().limit(12).toList()) {
            g.drawCenteredString(font, pack, width / 2, y, 0xFFFFFFFF);
            y += 20;
        }
    }

    private static String trim(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == 0 && button == 0) {
            int gap = 10;
            int cardWidth = Math.min(190, Math.max(96, (width - 48 - gap * 2) / 3));
            int cardHeight = 76;
            int total = cardWidth * 3 + gap * 2;
            int left = (width - total) / 2;
            int top = Math.max(66, height / 2 - 118);
            for (int i = 0; i < CATEGORIES.size(); i++) {
                int x = left + (i % 3) * (cardWidth + gap);
                int y = top + (i / 3) * (cardHeight + gap);
                if (mouseX >= x && mouseX < x + cardWidth && mouseY >= y && mouseY < y + cardHeight) {
                    minecraft.setScreen(new PackEditorScreen(i + 1));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void choosePacks() {
        try {
            FileDialog dialog = new FileDialog((Frame) null, "7xqer – Resourcepacks auswählen", FileDialog.LOAD);
            dialog.setMultipleMode(true);
            dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".zip"));
            dialog.setVisible(true);
            File[] files = dialog.getFiles();
            if (files.length == 0) return;
            importPacks(java.util.Arrays.stream(files).map(File::toPath).toList());
        } catch (Exception e) {
            status = "Pack auswählen fehlgeschlagen: " + e.getMessage();
        }
    }

    private void importPacks(List<Path> paths) {
        try {
            int imported = PackBuilder.importPacks(paths, Minecraft.getInstance().getResourcePackDirectory());
            status = imported == 0 ? "Keine unterstützten ZIP-Packs gefunden." : imported + " Pack(s) hinzugefügt.";
        } catch (IOException e) {
            status = "Pack-Import fehlgeschlagen: " + e.getMessage();
        }
    }

    @Override public void onFilesDrop(List<Path> paths) { importPacks(paths); }

    private void openPackFolder() {
        try { java.awt.Desktop.getDesktop().open(Minecraft.getInstance().getResourcePackDirectory().toFile()); }
        catch (Exception e) { status = "resourcepacks-Ordner: " + Minecraft.getInstance().getResourcePackDirectory(); }
    }

    private void exportPack() {
        try { status = "Pack erstellt: " + PackBuilder.build(Minecraft.getInstance().getResourcePackDirectory()).getFileName(); }
        catch (Exception e) { status = "Pack-Erstellung fehlgeschlagen: " + e.getMessage(); }
    }
}
