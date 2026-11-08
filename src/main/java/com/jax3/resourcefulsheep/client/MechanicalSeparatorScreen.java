package com.jax3.resourcefulsheep.client;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blockentity.MachineStatus;
import com.jax3.resourcefulsheep.blockentity.RedstoneMode;
import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.menu.MechanicalSeparatorMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI of the Mechanical Separator. All art comes from {@code textures/gui/mechanical_separator.png} (256x256):
 * the 176x193 background at (0,0) and sprites in the strip below it, see {@code tools/generate_textures.py}.
 */
public class MechanicalSeparatorScreen extends AbstractContainerScreen<MechanicalSeparatorMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/mechanical_separator.png");
    private static final int TEX = 256;

    // Vertical energy gauge (inner fill area, fills bottom-up) and its sprite.
    private static final int BAR_X = 11, BAR_Y = 19, BAR_W = 12, BAR_H = 64;
    private static final int BAR_U = 176, BAR_V = 96;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6;
    // Progress arrow and its filled sprite.
    private static final int ARROW_X = 66, ARROW_Y = 56, ARROW_W = 32, ARROW_H = 16;
    private static final int ARROW_U = 0, ARROW_V = 194;
    // Spinning gear (4 animation frames, 12x12) drawn inside the arrow housing while processing.
    private static final int GEAR_X = 76, GEAR_Y = 72, GEAR_SIZE = 12;
    private static final int GEAR_U = 138, GEAR_V = 194;
    // Status lamp (8x8 sprites per status) and redstone button.
    private static final int LAMP_X = 9, LAMP_Y = 87;
    private static final int LAMP_U = 186, LAMP_V = 194;
    private static final int BUTTON_X = 154, BUTTON_Y = 4, BUTTON_SIZE = 16;
    private static final int BUTTON_U = 176, BUTTON_V = 0;

    public MechanicalSeparatorScreen(MechanicalSeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = MechanicalSeparatorMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);

        // Energy bar fills left to right.
        int energyHeight = Mth.ceil(menu.getEnergyFraction() * BAR_H);
        if (energyHeight > 0) {
            int offset = BAR_H - energyHeight;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BAR_X, y + BAR_Y + offset, BAR_U, BAR_V + offset, BAR_W, energyHeight, TEX, TEX);
        }

        // Progress arrow.
        int arrowWidth = Mth.ceil(menu.getProgressFraction() * ARROW_W);
        if (arrowWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, arrowWidth, ARROW_H, TEX, TEX);
        }

        // Gear: animates while processing, frame 0 otherwise.
        MachineStatus status = menu.getStatus();
        int frame = status == MachineStatus.PROCESSING && minecraft != null && minecraft.level != null
                ? (int) (minecraft.level.getGameTime() / 3 % 4) : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GEAR_X, y + GEAR_Y, GEAR_U + frame * GEAR_SIZE, GEAR_V, GEAR_SIZE, GEAR_SIZE, TEX, TEX);

        // Status lamp.
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + status.ordinal() * 8 % 64, LAMP_V + status.ordinal() / 8 * 8, 8, 8, TEX, TEX);

        // Redstone mode button (normal / hovered background + mode icon).
        boolean hovered = isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BUTTON_X, y + BUTTON_Y, BUTTON_U + (hovered ? 16 : 0), BUTTON_V + 64, BUTTON_SIZE, BUTTON_SIZE, TEX, TEX);
        int mode = menu.getRedstoneMode().ordinal();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BUTTON_X, y + BUTTON_Y, BUTTON_U + (mode % 4) * 16 % 64, BUTTON_V + (mode / 4) * 16, BUTTON_SIZE, BUTTON_SIZE, TEX, TEX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Light text for the dark slate theme (the vanilla labels are dark grey).
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        // Stored / max FE, centred under the bar.
        Component energy = Component.translatable("gui.resourcefulsheep.energy_short", fe(menu.getEnergy()), fe(menu.getCapacity()));
        graphics.text(font, energy, 34 + (118 - font.width(energy)) / 2, 28, TEXT_DIM, false);

        // Status line, coloured like the lamp.
        MachineStatus status = menu.getStatus();
        Component line = status == MachineStatus.PROCESSING
                ? Component.translatable("gui.resourcefulsheep.processing_percent", Math.round(menu.getProgressFraction() * 100))
                : status.displayName();
        graphics.text(font, line, LAMP_X + 13, LAMP_Y + 1, status.color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && hoveredSlot == null) {
            List<Component> lines = tooltipAt(mouseX, mouseY);
            if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        boolean detailed = ModConfigs.getBool(ModConfigs.CLIENT.detailedMachineTooltips);
        if (isHovering(BAR_X - 2, BAR_Y - 2, BAR_W + 4, BAR_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.resourcefulsheep.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            if (detailed) {
                lines.add(Component.translatable("gui.resourcefulsheep.energy_input", fe(ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput))).withStyle(ChatFormatting.GRAY));
                if (menu.getEnergyPerTick() > 0) {
                    lines.add(Component.translatable("gui.resourcefulsheep.energy_usage", fe(menu.getEnergyPerTick())).withStyle(ChatFormatting.GRAY));
                }
            }
        } else if (isHovering(ARROW_X, ARROW_Y, ARROW_W, ARROW_H, mouseX, mouseY) || isHovering(GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE, mouseX, mouseY)) {
            int max = menu.getMaxProgress();
            lines.add(Component.translatable("gui.resourcefulsheep.progress", Math.round(menu.getProgressFraction() * 100)));
            if (detailed && max > 0) {
                double secondsLeft = (max - menu.getProgress()) / 20.0;
                lines.add(Component.translatable("gui.resourcefulsheep.time_left", String.format(Locale.ROOT, "%.1f", secondsLeft), String.format(Locale.ROOT, "%.1f", max / 20.0)).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.resourcefulsheep.operation_energy", fe((long) menu.getEnergyPerTick() * max)).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 90, 10, mouseX, mouseY)) {
            MachineStatus status = menu.getStatus();
            lines.add(status.displayName().copy().withColor(status.color));
            lines.add(status.description().copy().withStyle(ChatFormatting.GRAY));
        } else if (isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            RedstoneMode mode = menu.getRedstoneMode();
            lines.add(Component.translatable("gui.resourcefulsheep.redstone", mode.displayName()));
            lines.add(Component.translatable("gui.resourcefulsheep.redstone." + mode.name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(menu.isPowered() ? "gui.resourcefulsheep.redstone.powered" : "gui.resourcefulsheep.redstone.unpowered").withStyle(ChatFormatting.DARK_GRAY));
            if (!ModConfigs.getBool(ModConfigs.SERVER.redstoneControlEnabled)) {
                lines.add(Component.translatable("gui.resourcefulsheep.redstone.locked").withStyle(ChatFormatting.RED));
            } else {
                lines.add(Component.translatable("gui.resourcefulsheep.redstone.click").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, event.x(), event.y())
                && minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MechanicalSeparatorMenu.BUTTON_REDSTONE);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
