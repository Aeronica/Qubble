package net.ilexiconn.qubble.client.gui.component;

import net.ilexiconn.qubble.Qubble;
import net.ilexiconn.qubble.client.ClientProxy;
import net.ilexiconn.qubble.client.gui.QubbleGUI;
import net.ilexiconn.qubble.server.model.qubble.QubbleCube;
import net.ilexiconn.qubble.server.model.qubble.QubbleModel;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ModelTreeComponent extends Gui implements IComponent<QubbleGUI> {
    private int width = 100;
    private boolean rescaling;
    private int partY;

    private int scroll;
    private int scrollYOffset;
    private boolean scrolling;

    public ModelTreeComponent() {
    }

    @Override
    public void render(QubbleGUI gui, float mouseX, float mouseY, double offsetX, double offsetY, float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();

        int height = gui.height - 22;

        gui.drawRectangle(0, 21, this.width, height, QubbleGUI.getSecondaryColor());

        float scrollPerEntry = (float) (this.partY) / (float) (height - 21);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution resolution = new ScaledResolution(ClientProxy.MINECRAFT);
        int scaleFactor = resolution.getScaleFactor();
        GL11.glScissor(0, 0, (this.width - 11) * scaleFactor, height * scaleFactor);
        if (gui.getCurrentModel() != null) {
            this.partY = 0;
            QubbleModel model = gui.getCurrentModel();
            for (QubbleCube cube : model.getCubes()) {
                this.drawCubeEntry(gui, cube, 0, scrollPerEntry);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        gui.drawOutline(0, 21, this.width, height + 1, Qubble.CONFIG.getAccentColor(), 1);

        float maxDisplayEntries = height / 13;
        float maxScroll = Math.max(0, this.partY - maxDisplayEntries);

        if (maxScroll > 0) {
            int scrollX = this.width - 9;
            int scrollY = this.scroll + 23;
            int scrollerHeight = (int) ((height - 21) / ((float) this.partY / maxDisplayEntries));
            gui.drawRectangle(scrollX, scrollY, 6, scrollerHeight, this.scrolling ? QubbleGUI.getPrimaryColor() : QubbleGUI.getSecondaryColor());
            gui.drawOutline(scrollX, scrollY, 6, scrollerHeight, Qubble.CONFIG.getAccentColor(), 1);
        }
    }

    @Override
    public void renderAfter(QubbleGUI gui, float mouseX, float mouseY, double offsetX, double offsetY, float partialTicks) {

    }

    private void drawCubeEntry(QubbleGUI gui, QubbleCube cube, int xOffset, float scrollPerEntry) {
        int y = (int) (-this.scroll * scrollPerEntry);
        FontRenderer fontRenderer = ClientProxy.MINECRAFT.fontRendererObj;
        String name = cube.getName();
        fontRenderer.drawString(name, xOffset + 5, 25 + (this.partY + y) * 12, QubbleGUI.getTextColor());
        gui.drawOutline(xOffset + 3, 23 + (this.partY + y) * 12, this.width - xOffset - 14, 11, QubbleGUI.getPrimaryColor(), 1);
        this.partY++;
        for (QubbleCube child : cube.getChildren()) {
            this.drawCubeEntry(gui, child, xOffset + 3, scrollPerEntry);
        }
    }

    @Override
    public boolean mouseClicked(QubbleGUI gui, float mouseX, float mouseY, int button) {
        if (mouseX > this.width - 4 && mouseX < this.width + 4 && mouseY > 21) {
            this.rescaling = true;
        }
        int height = gui.height - 21;
        float maxDisplayEntries = height / 13;
        float maxScroll = Math.max(0, this.partY - maxDisplayEntries);
        if (maxScroll > 0) {
            int scrollX = this.width - 9;
            int scrollY = this.scroll + 23;
            int scrollerHeight = (int) ((height - 21) / ((float) this.partY / maxDisplayEntries));
            if (mouseX >= scrollX && mouseX < scrollX + 6 && mouseY >= scrollY && mouseY < scrollY + scrollerHeight) {
                this.scrolling = true;
                this.scrollYOffset = (int) (mouseY - scrollY);
                return true;
            }
        }
        return this.rescaling;
    }

    @Override
    public boolean mouseDragged(QubbleGUI gui, float mouseX, float mouseY, int button, long timeSinceClick) {
        if (this.rescaling) {
            this.width = (int) Math.max(50, Math.min(300, mouseX));
        }
        if (this.scrolling) {
            int height = gui.height - 21;
            float maxDisplayEntries = height / 13;
            float maxScroll = Math.max(0, this.partY - maxDisplayEntries);
            float scrollPerEntry = (float) (this.partY) / (float) (height - 23);
            this.scroll = (int) Math.max(0, Math.min(maxScroll / scrollPerEntry, mouseY - 23 - this.scrollYOffset));
        }
        return this.rescaling || this.scrolling;
    }

    @Override
    public boolean mouseReleased(QubbleGUI gui, float mouseX, float mouseY, int button) {
        this.rescaling = false;
        this.scrolling = false;
        return false;
    }

    @Override
    public boolean keyPressed(QubbleGUI gui, char character, int key) {
        return false;
    }
}
