package com.zov.zovcapture.client;

import com.zov.zovcapture.network.RequestEconomySyncPayload;
import com.zov.zovcapture.network.ShopBuyPayload;
import com.zov.zovcapture.network.ShopOfferSync;
import com.zov.zovcapture.shop.ShopCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ShopScreen extends AbstractZovScreen {
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 3;
    private static final int GRID_SLOTS = GRID_COLS * GRID_ROWS;

    private enum WalletTab {
        PERSONAL,
        TEAM
    }

    private WalletTab walletTab = WalletTab.PERSONAL;
    @Nullable
    private ShopCategory categoryFilter;
    private int page;
    private Button personalTab;
    private Button teamTab;
    private final List<Button> categoryButtons = new ArrayList<>();

    public ShopScreen() {
        super(Component.translatable("gui.zovcapture.shop.title"), 240, 224);
    }

    @Override
    protected boolean shouldDrawCredit() {
        return true;
    }

    public static void open() {
        PacketDistributor.sendToServer(new RequestEconomySyncPayload());
        net.minecraft.client.Minecraft.getInstance().setScreen(new ShopScreen());
    }

    public static void onEconomySync() {
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof ShopScreen screen) {
            screen.refreshFromSync();
        }
    }

    public void refreshFromSync() {
        rebuild();
    }

    @Override
    protected void init() {
        super.init();
        categoryButtons.clear();
        int x = leftPos() + PANEL_MARGIN;
        int y = topPos() + 22;

        personalTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.zovcapture.menu.tab.personal"),
                button -> selectWallet(WalletTab.PERSONAL)
        ).bounds(x, y, 108, 18).build());

        teamTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.zovcapture.menu.tab.team"),
                button -> selectWallet(WalletTab.TEAM)
        ).bounds(x + 112, y, 108, 18).build());

        y += 22;
        int buttonWidth = 32;
        int gap = 2;
        int catX = leftPos() + (panelWidth - (buttonWidth * 6 + gap * 5)) / 2;

        categoryButtons.add(addRenderableWidget(createCategoryButton(
                catX,
                y,
                buttonWidth,
                null,
                Component.translatable("gui.zovcapture.shop.category.all")
        )));

        int index = 1;
        for (ShopCategory category : ShopCategory.values()) {
            categoryButtons.add(addRenderableWidget(createCategoryButton(
                    catX + index * (buttonWidth + gap),
                    y,
                    buttonWidth,
                    category,
                    Component.translatable(category.translationKey())
            )));
            index++;
        }

        int maxPage = Math.max(0, (filteredOffers().size() - 1) / GRID_SLOTS);
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.literal("◀"), button -> {
                page--;
                rebuild();
            }).bounds(leftPos() + PANEL_MARGIN, topPos() + panelHeight - 24, 24, 18).build());
        }
        if (page < maxPage) {
            addRenderableWidget(Button.builder(Component.literal("▶"), button -> {
                page++;
                rebuild();
            }).bounds(leftPos() + panelWidth - PANEL_MARGIN - 24, topPos() + panelHeight - 24, 24, 18).build());
        }
    }

    private Button createCategoryButton(int x, int y, int width, @Nullable ShopCategory category, Component label) {
        return Button.builder(label, button -> {
            categoryFilter = category;
            page = 0;
            rebuild();
        }).bounds(x, y, width, 18).build();
    }

    private void selectWallet(WalletTab tab) {
        walletTab = tab;
        page = 0;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private List<ShopOfferSync> filteredOffers() {
        List<ShopOfferSync> filtered = new ArrayList<>();
        for (ShopOfferSync offer : ClientEconomyData.shopOffers()) {
            if (walletTab == WalletTab.PERSONAL && !"PERSONAL".equals(offer.wallet())) {
                continue;
            }
            if (walletTab == WalletTab.TEAM && !"TEAM".equals(offer.wallet())) {
                continue;
            }
            if (!matchesCategory(offer)) {
                continue;
            }
            filtered.add(offer);
        }
        return filtered;
    }

    private boolean matchesCategory(ShopOfferSync offer) {
        if (categoryFilter == null) {
            return true;
        }
        if ("TEAM_STAT".equals(offer.type()) || "COMMAND".equals(offer.type())) {
            return categoryFilter == ShopCategory.TOOLS;
        }
        try {
            return categoryFilter.name().equals(offer.category());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private int gridOriginX() {
        return leftPos() + (panelWidth - GRID_COLS * SLOT_SIZE) / 2;
    }

    private int gridOriginY() {
        return topPos() + 118;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHeader(graphics);

        drawTabHighlight(graphics, personalTab.getX(), personalTab.getY(), personalTab.getWidth(), personalTab.getHeight(), walletTab == WalletTab.PERSONAL);
        drawTabHighlight(graphics, teamTab.getX(), teamTab.getY(), teamTab.getWidth(), teamTab.getHeight(), walletTab == WalletTab.TEAM);

        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            boolean active = i == 0 ? categoryFilter == null : categoryFilter == ShopCategory.values()[i - 1];
            drawTabHighlight(graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(), active);
        }

        drawInfoBar(
                graphics,
                topPos() + 66,
                15,
                Component.translatable("gui.zovcapture.menu.personal", ClientEconomyData.personalMoney()),
                0x66FF66
        );
        if (!ClientEconomyData.teamName().isEmpty()) {
            drawInfoBar(
                    graphics,
                    topPos() + 83,
                    15,
                    Component.translatable("gui.zovcapture.menu.team", ClientEconomyData.teamMoney()),
                    0x66BBFF
            );
        }
        if (!ClientEconomyData.shopAtBase()) {
            drawInfoBar(
                    graphics,
                    topPos() + 100,
                    15,
                    Component.translatable("gui.zovcapture.shop.need_base"),
                    0xFFAA55
            );
        }

        List<ShopOfferSync> offers = filteredOffers();
        int start = page * GRID_SLOTS;
        int slotBaseX = gridOriginX();
        int slotBaseY = gridOriginY();

        for (int i = 0; i < GRID_SLOTS; i++) {
            int row = i / GRID_COLS;
            int col = i % GRID_COLS;
            int slotX = slotBaseX + col * SLOT_SIZE;
            int slotY = slotBaseY + row * SLOT_SIZE;
            boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            drawSlot(graphics, slotX, slotY, hovered);

            int index = start + i;
            if (index >= offers.size()) {
                continue;
            }

            ShopOfferSync offer = offers.get(index);
            renderOfferIcon(graphics, offer, slotX, slotY);

            if (hovered) {
                renderTooltip(graphics, buildTooltip(offer), mouseX, mouseY);
            }
        }

        if (offers.isEmpty()) {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.shop.empty"), slotBaseY + 24);
        }

        int maxPage = Math.max(0, (offers.size() - 1) / GRID_SLOTS);
        if (maxPage > 0) {
            drawMutedCentered(
                    graphics,
                    Component.translatable("gui.zovcapture.shop.page", page + 1, maxPage + 1),
                    topPos() + panelHeight - 20
            );
        }
    }

    private List<Component> buildTooltip(ShopOfferSync offer) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(offer.displayName()));
        if (offer.description() != null && !offer.description().isBlank()) {
            lines.add(Component.literal(offer.description()).withStyle(ChatFormatting.GRAY));
        }
        if ("sbw_p_scout_jammer".equals(offer.id())) {
            lines.add(Component.literal("Глушит вражеские и союзные дроны"));
        }
        lines.add(Component.translatable("gui.zovcapture.shop.cost", offer.cost()));
        lines.add(Component.translatable(categoryTranslationKey(offer.category())));
        if ("BUNDLE".equals(offer.type())) {
            lines.add(Component.translatable("gui.zovcapture.shop.bundle_items", offer.bundleLines().size()));
            for (String line : offer.bundleLines()) {
                lines.add(formatBundleLine(line));
            }
        }
        lines.add(Component.translatable("gui.zovcapture.shop.click_buy"));
        return lines;
    }

    private Component formatBundleLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 2) {
            return Component.literal(" - " + line);
        }
        ResourceLocation itemId = ResourceLocation.parse(parts[0]);
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.BARRIER);
        int count;
        try {
            count = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return Component.literal(" - " + line);
        }
        return Component.literal(" - ").append(new ItemStack(item, count).getHoverName()).append(" x" + count);
    }

    private String categoryTranslationKey(String category) {
        ShopCategory parsed = ShopCategory.parse(category).orElse(ShopCategory.BLOCKS);
        return parsed.translationKey();
    }

    private void renderOfferIcon(GuiGraphics graphics, ShopOfferSync offer, int slotX, int slotY) {
        if ("BUNDLE".equals(offer.type())) {
            ItemStack icon = new ItemStack(Items.CHEST);
            if (!offer.bundleLines().isEmpty()) {
                String[] parts = offer.bundleLines().getFirst().split("\\|");
                if (parts.length == 2) {
                    ResourceLocation itemId = ResourceLocation.parse(parts[0]);
                    Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.CHEST);
                    icon = new ItemStack(item);
                }
            }
            graphics.renderItem(icon, slotX + 1, slotY + 1);
            if (offer.bundleLines().size() > 1) {
                graphics.drawString(this.font, String.valueOf(offer.bundleLines().size()), slotX + 10, slotY + 10, 0xFFFFFF, true);
            }
            return;
        }

        if (!"ITEM".equals(offer.type())) {
            graphics.renderItem(new ItemStack(Items.PAPER), slotX + 1, slotY + 1);
            return;
        }

        ResourceLocation itemId = ResourceLocation.parse(offer.payload());
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.BARRIER);
        ItemStack stack = new ItemStack(item, Math.max(1, offer.count()));
        graphics.renderItem(stack, slotX + 1, slotY + 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<ShopOfferSync> offers = filteredOffers();
            int start = page * GRID_SLOTS;
            int slotBaseX = gridOriginX();
            int slotBaseY = gridOriginY();

            for (int i = 0; i < GRID_SLOTS; i++) {
                int index = start + i;
                if (index >= offers.size()) {
                    break;
                }
                int row = i / GRID_COLS;
                int col = i % GRID_COLS;
                int slotX = slotBaseX + col * SLOT_SIZE;
                int slotY = slotBaseY + row * SLOT_SIZE;
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    if (!ClientEconomyData.shopAtBase()) {
                        return true;
                    }
                    PacketDistributor.sendToServer(new ShopBuyPayload(offers.get(index).id()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
