package net.ilexiconn.qubble.client.gui.component;

import net.ilexiconn.qubble.client.ClientProxy;
import net.ilexiconn.qubble.client.gui.QubbleGUI;
import net.ilexiconn.qubble.client.model.QubbleModelBase;
import net.ilexiconn.qubble.server.model.qubble.QubbleModel;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class ModelViewComponent implements IGUIComponent {
    private float cameraOffsetX = 0.0F;
    private float cameraOffsetY = 0.0F;

    private float rotationYaw = 45.0F;
    private float rotationPitch = 45.0F;

    private float zoom = 1.0F;

    private QubbleModel currentModelContainer;
    private QubbleModelBase currentModel;

    private int prevMouseX;
    private int prevMouseY;

    @Override
    public void render(QubbleGUI gui, int mouseX, int mouseY, double offsetX, double offsetY, float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        gui.drawOutline(0, 20, gui.width, gui.height - 20, QubbleGUI.getPrimaryColor(), 1);
        ScaledResolution scaledResolution = new ScaledResolution(ClientProxy.MINECRAFT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = scaledResolution.getScaleFactor();
        GL11.glScissor(0, 0, gui.width * scaleFactor, (gui.height - 21) * scaleFactor);
        if (gui.getCurrentModel() != null) {
            GlStateManager.pushMatrix();
            GlStateManager.disableCull();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableNormalize();
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GLU.gluPerspective(30.0F, (float) (scaledResolution.getScaledWidth_double() / scaledResolution.getScaledHeight_double()), 1.0F, 10000.0F);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            int color = QubbleGUI.getSecondaryColor();
            float r = (float) (color >> 16 & 0xFF) / 255.0F;
            float g = (float) (color >> 8 & 0xFF) / 255.0F;
            float b = (float) (color & 0xFF) / 255.0F;
            GlStateManager.clearColor(r * 0.8F, g * 0.8F, b * 0.8F, 1.0F);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
            this.setupCamera(10.0F);
            RenderHelper.enableStandardItemLighting();
            if (this.currentModelContainer != gui.getCurrentModel()) {
                this.currentModel = new QubbleModelBase(gui.getCurrentModel());
                this.currentModelContainer = gui.getCurrentModel();
            }
            GlStateManager.translate(0.0F, -1.0F, 0.0F);
            this.currentModel.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
            GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
        GlStateManager.enableTexture2D();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(), 0.0, -5000.0D, 5000.0D);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
    }

    private void setupCamera(float scale) {
        GlStateManager.disableTexture2D();
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(0.0F, -2.0F, -10.0F);
        GlStateManager.scale(this.zoom, this.zoom, this.zoom);
        GlStateManager.scale(1.0F, -1.0F, 1.0F);
        GlStateManager.translate(this.cameraOffsetX, this.cameraOffsetY, 0.0F);
        GlStateManager.rotate(this.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(this.rotationYaw, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public void renderAfter(QubbleGUI gui, int mouseX, int mouseY, double offsetX, double offsetY, float partialTicks) {

    }

    @Override
    public void mouseClicked(QubbleGUI gui, int mouseX, int mouseY, int button) {
        if (button == 1) {
            this.prevMouseX = mouseX;
            this.prevMouseY = mouseY;
        }
    }

    @Override
    public void mouseDragged(QubbleGUI gui, int mouseX, int mouseY, int button, long timeSinceClick) {
        if (button == 1) {
            this.rotationYaw += mouseX - this.prevMouseX;
            this.rotationPitch -= mouseY - this.prevMouseY;

            this.prevMouseX = mouseX;
            this.prevMouseY = mouseY;
        }
    }

    @Override
    public void mouseReleased(QubbleGUI gui, int mouseX, int mouseY, int button) {

    }
}
