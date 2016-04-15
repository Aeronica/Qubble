package net.ilexiconn.qubble.client.gui.component;

import net.ilexiconn.qubble.Qubble;
import net.ilexiconn.qubble.client.ClientProxy;
import net.ilexiconn.qubble.client.gui.QubbleGUI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.config.HoverChecker;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;

@SideOnly(Side.CLIENT)
public class ButtonComponent extends Gui implements IGUIComponent {
    private String text;
    private String tooltip;

    private int posX;
    private int posY;
    private int width;
    private int height;

    private int primaryColor = QubbleGUI.PRIMARY_COLOR;
    private int secondaryColor = QubbleGUI.SECONDARY_COLOR;
    private int accentColor = Qubble.CONFIG.getAccentColor();

    private HoverChecker hoverChecker;
    private IActionHandler<ButtonComponent> actionHandler;

    public ButtonComponent(String text, int posX, int posY, int width, int height, IActionHandler<ButtonComponent> action) {
        this(text, posX, posY, width, height, null, action);
    }

    public ButtonComponent(String text, int posX, int posY, int width, int height, String tooltip, IActionHandler<ButtonComponent> action) {
        this.text = text;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.tooltip = tooltip;
        this.actionHandler = action;
        this.hoverChecker = new HoverChecker(posY, posY + height, posX, posX + width, 800);
    }

    @Override
    public void render(QubbleGUI gui, int mouseX, int mouseY, float partialTicks) {
        boolean selected = this.isSelected(mouseX, mouseY);
        GlStateManager.disableTexture2D();
        this.drawGradientRect(this.posX + 1, this.posY + 1, this.posX + this.width - 1, this.posY + this.height - 1, selected ? primaryColor : primaryColor, selected ? secondaryColor : primaryColor);
        gui.drawOutline(this.posX, this.posY, this.width, this.height, this.accentColor, 1);
        GlStateManager.enableTexture2D();
        FontRenderer fontRenderer = ClientProxy.MINECRAFT.fontRendererObj;
        fontRenderer.drawString(this.text, this.posX + (this.width / 2) - (fontRenderer.getStringWidth(this.text) / 2) + 0.625F, this.posY + (this.height / 2) - (fontRenderer.FONT_HEIGHT / 2) - 0.625F, 0xFFFFFF, false);
        if (this.tooltip != null && this.hoverChecker.checkHover(mouseX, mouseY)) {
            GuiScreen currentScreen = ClientProxy.MINECRAFT.currentScreen;
            GuiUtils.drawHoveringText(Collections.singletonList(this.tooltip), mouseX, mouseY, currentScreen.width, currentScreen.height, 300, fontRenderer);
            GlStateManager.disableLighting();
        }
    }

    @Override
    public void mouseClicked(QubbleGUI gui, int mouseX, int mouseY, int button) {
        if (this.isSelected(mouseX, mouseY)) {
            this.actionHandler.onAction(gui, this);
        }
    }

    @Override
    public void mouseDragged(QubbleGUI gui, int mouseX, int mouseY, int button, long timeSinceClick) {

    }

    protected boolean isSelected(int mouseX, int mouseY) {
        return mouseX > this.posX && mouseY > this.posY && mouseX < this.posX + this.width && mouseY < this.posY + this.height;
    }

    public void setColorScheme(int primaryColor, int secondaryColor, int accentColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
    }
}
