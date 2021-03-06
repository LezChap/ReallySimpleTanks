package com.reallysimpletanks.blocks;

import com.reallysimpletanks.ReallySimpleTanks;
import com.mojang.blaze3d.platform.GlStateManager;
import com.reallysimpletanks.api.TankMode;
import com.reallysimpletanks.client.DumpButton;
import com.reallysimpletanks.client.ModeButton;
import com.reallysimpletanks.network.Networking;
import com.reallysimpletanks.network.TankModePacket;
import com.reallysimpletanks.utils.EnumUtils;
import com.reallysimpletanks.utils.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BasicTankScreen extends ContainerScreen<BasicTankContainer> {

    private ResourceLocation GUI = new ResourceLocation(ReallySimpleTanks.MODID, "textures/gui/tank_gui.png");
    private ResourceLocation GAUGES = new ResourceLocation(ReallySimpleTanks.MODID, "textures/gui/tank_gauge.png");

    public BasicTankScreen(BasicTankContainer screenContainer, PlayerInventory inv, ITextComponent name) {
        super(screenContainer, inv, name);
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(new DumpButton(container, this.guiLeft + 81, this.guiTop + 61, 18, 7, "", button -> {
            if (hasControlDown()) {
                container.dumpTank();
            }
        }));
        this.addButton(new ModeButton(container, this.guiLeft + 3, this.guiTop + 3, 40, 16, button -> {
            TankMode mode = ((ModeButton) button).getMode();
            Networking.INSTANCE.sendToServer(new TankModePacket(mode));
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        for (Widget widget : this.buttons) {
            if (widget.isHovered() && widget instanceof DumpButton) {
                List<String> tooltip = Arrays.asList(
                        new TranslationTextComponent("misc.reallysimpletanks.dump_tank1").getFormattedText(),
                        new TranslationTextComponent("misc.reallysimpletanks.dump_tank2").getFormattedText());
                renderTooltip(tooltip, mouseX - guiLeft, mouseY - guiTop);
            } else if (widget.isHovered() && widget instanceof ModeButton) {
                int modeOrdinal = container.getTankMode().ordinal();
                List<String> tooltip = new ArrayList<>();
                switch (modeOrdinal) {
                    case 0:  //NORMAL mode
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.normal_tooltip1").getFormattedText());
                        tooltip.add("--------------------------");
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.normal_tooltip2").getFormattedText());
                        tooltip.add(" ");
                        break;
                    case 1:  //EXCESS mode
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.excess_tooltip1").getFormattedText());
                        tooltip.add("--------------------------");
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.excess_tooltip2").getFormattedText());
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.excess_tooltip3").getFormattedText());
                        break;
                    case 2:  //PUMP mode
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.pump_tooltip1").getFormattedText());
                        tooltip.add("------------------------");
                        tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.pump_tooltip2").getFormattedText());
                        tooltip.add(" ");
                        break;
                }
                int nextOrdinal = modeOrdinal + 1;
                if (nextOrdinal >= TankMode.values().length) nextOrdinal = 0;
                String nextMode = EnumUtils.byOrdinal(nextOrdinal, TankMode.NORMAL).name();
                tooltip.add(" ");
                tooltip.add(new TranslationTextComponent("misc.reallysimpletanks.next_mode", nextMode).getFormattedText());
                renderTooltip(tooltip, mouseX - guiLeft, mouseY - guiTop);
            }
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        if (isPointInRegion(82, 8, 16, 52, mouseX, mouseY)) {
            FluidStack stack = container.getFluidInTank();
            ITextComponent text = Tools.formatFluid(stack, container.getFluidCapacity());
            renderTooltip(text.getFormattedText(), mouseX, mouseY);
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI);
        int relX = (this.width - this.xSize) / 2;
        int relY = (this.height - this.ySize) / 2;
        this.blit(relX, relY, 0, 0, this.xSize, this.ySize);

        renderGuiTank(container.getFluidInTank(), BasicTankTileEntity.CAPACITY, relX + 82, relY + 8, 0, 16, 52);

        this.minecraft.getTextureManager().bindTexture(GAUGES);
        this.blit(relX, relY, 0, 0, this.xSize, this.ySize);
    }

    public static void renderGuiTank(FluidStack stack, int tankCapacity, double x, double y, double zLevel, double width, double height) {
        // Adapted from Silents Mechanisms which adapted from Ender IO
        int amount = stack.getAmount();
        if (stack.getFluid() == null || amount <= 0) {
            return;
        }

        ResourceLocation stillTexture = stack.getFluid().getAttributes().getStillTexture();
        TextureAtlasSprite icon = Minecraft.getInstance().getTextureMap().getSprite(stillTexture);

        int renderAmount = (int) Math.max(Math.min(height, amount * height / tankCapacity), 1);
        int posY = (int) (y + height - renderAmount);

        Minecraft.getInstance().getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        int color = stack.getFluid().getAttributes().getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GlStateManager.color3f(r, g, b);

        GlStateManager.enableBlend();
        for (int i = 0; i < width; i += 16) {
            for (int j = 0; j < renderAmount; j += 16) {
                int drawWidth = (int) Math.min(width - i, 16);
                int drawHeight = Math.min(renderAmount - j, 16);

                int drawX = (int) (x + i);
                int drawY = posY + j;

                double minU = icon.getMinU();
                double maxU = icon.getMaxU();
                double minV = icon.getMinV();
                double maxV = icon.getMaxV();

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder tes = tessellator.getBuffer();
                tes.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                tes.pos(drawX, drawY + drawHeight, 0).tex(minU, minV + (maxV - minV) * drawHeight / 16F).endVertex();
                tes.pos(drawX + drawWidth, drawY + drawHeight, 0).tex(minU + (maxU - minU) * drawWidth / 16F, minV + (maxV - minV) * drawHeight / 16F).endVertex();
                tes.pos(drawX + drawWidth, drawY, 0).tex(minU + (maxU - minU) * drawWidth / 16F, minV).endVertex();
                tes.pos(drawX, drawY, 0).tex(minU, minV).endVertex();
                tessellator.draw();
            }
        }
        GlStateManager.disableBlend();
        GlStateManager.color3f(1, 1, 1);
    }
}
