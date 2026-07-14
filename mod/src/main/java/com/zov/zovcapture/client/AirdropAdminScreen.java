package com.zov.zovcapture.client;

import com.zov.zovcapture.network.AirdropAdminSyncPayload;
import com.zov.zovcapture.network.RequestAirdropAdminSyncPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class AirdropAdminScreen extends AbstractZovScreen {
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 2;

    public AirdropAdminScreen() {
        super(Component.translatable("gui.zovcapture.airdrop.title"), 260, 240);
    }

    public static void open() {
        PacketDistributor.sendToServer(new RequestAirdropAdminSyncPayload());
        net.minecraft.client.Minecraft.getInstance().setScreen(new AirdropAdminScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHeader(graphics);

        drawInfoBar(
                graphics,
                topPos() + 24,
                16,
                Component.translatable(
                        "gui.zovcapture.airdrop.settings",
                        ClientAirdropData.enabled(),
                        ClientAirdropData.intervalSeconds(),
                        ClientAirdropData.captureSeconds(),
                        ClientAirdropData.particle()
                ),
                0xFFE8C878
        );

        drawSectionTitle(graphics, Component.translatable("gui.zovcapture.airdrop.spawn_points"), topPos() + 46);
        int pointY = topPos() + 58;
        if (ClientAirdropData.spawnPoints().isEmpty()) {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.airdrop.spawn_points.empty"), pointY);
        } else {
            int line = 0;
            for (AirdropAdminSyncPayload.AirdropSpawnPointSync point : ClientAirdropData.spawnPoints()) {
                if (line >= 3) {
                    break;
                }
                graphics.drawString(
                        this.font,
                        Component.translatable(
                                "gui.zovcapture.airdrop.spawn_point.line",
                                point.displayName(),
                                point.radius(),
                                point.center().getX(),
                                point.center().getY(),
                                point.center().getZ()
                        ),
                        leftPos() + PANEL_MARGIN,
                        pointY + line * 10,
                        0xFFCCCCCC,
                        false
                );
                line++;
            }
        }

        drawSectionTitle(graphics, Component.translatable("gui.zovcapture.airdrop.loot"), topPos() + 92);
        int gridX = leftPos() + (panelWidth - GRID_COLS * SLOT_SIZE) / 2;
        int gridY = topPos() + 104;
        List<AirdropAdminSyncPayload.AirdropLootSync> loot = ClientAirdropData.loot();

        for (int i = 0; i < GRID_COLS * GRID_ROWS; i++) {
            int row = i / GRID_COLS;
            int col = i % GRID_COLS;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;
            boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            drawSlot(graphics, slotX, slotY, hovered);

            if (i >= loot.size()) {
                continue;
            }

            AirdropAdminSyncPayload.AirdropLootSync entry = loot.get(i);
            renderLootIcon(graphics, entry, slotX, slotY);
            drawPriceBadge(graphics, slotX, slotY, String.valueOf(entry.weight()));

            if (hovered) {
                renderTooltip(graphics, buildLootTooltip(entry), mouseX, mouseY);
            }
        }

        if (loot.isEmpty()) {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.airdrop.loot.empty"), gridY + 10);
        }

        drawMutedCentered(
                graphics,
                Component.translatable("gui.zovcapture.airdrop.hint"),
                topPos() + panelHeight - 14
        );
    }

    private List<Component> buildLootTooltip(AirdropAdminSyncPayload.AirdropLootSync entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.displayName()));
        lines.add(Component.translatable("gui.zovcapture.airdrop.loot.weight", entry.weight()));
        lines.add(Component.translatable("gui.zovcapture.airdrop.loot.type", entry.type()));
        if ("BUNDLE".equals(entry.type())) {
            for (String line : entry.bundleLines()) {
                lines.add(formatBundleLine(line));
            }
        } else if ("PERSONAL_MONEY".equals(entry.type()) || "TEAM_MONEY".equals(entry.type())) {
            lines.add(Component.translatable("gui.zovcapture.airdrop.loot.money", entry.count()));
        }
        return lines;
    }

    private Component formatBundleLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 2) {
            return Component.literal(" - " + line);
        }
        ResourceLocation itemId = ResourceLocation.parse(parts[0]);
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.BARRIER);
        return Component.literal(" - ").append(new ItemStack(item, Integer.parseInt(parts[1])).getHoverName());
    }

    private void renderLootIcon(GuiGraphics graphics, AirdropAdminSyncPayload.AirdropLootSync entry, int slotX, int slotY) {
        ItemStack icon = switch (entry.type()) {
            case "ITEM" -> {
                ResourceLocation itemId = ResourceLocation.parse(entry.payload());
                Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.BARRIER);
                yield new ItemStack(item, Math.max(1, entry.count()));
            }
            case "BUNDLE" -> new ItemStack(Items.CHEST);
            case "PERSONAL_MONEY" -> new ItemStack(Items.EMERALD);
            case "TEAM_MONEY" -> new ItemStack(Items.GOLD_INGOT);
            default -> new ItemStack(Items.PAPER);
        };
        graphics.renderItem(icon, slotX + 1, slotY + 1);
    }
}
