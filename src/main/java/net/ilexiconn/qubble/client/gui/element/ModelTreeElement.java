package net.ilexiconn.qubble.client.gui.element;

import net.ilexiconn.llibrary.client.model.qubble.QubbleCube;
import net.ilexiconn.llibrary.client.model.qubble.QubbleModel;
import net.ilexiconn.qubble.Qubble;
import net.ilexiconn.qubble.client.ClientProxy;
import net.ilexiconn.qubble.client.gui.QubbleGUI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ModelTreeElement extends Element<QubbleGUI> {
    private boolean resizing;
    private int cubeY;
    private int entryCount;
    private List<QubbleCube> expandedCubes = new ArrayList<>();

    private ScrollerElement scroller;

    public ModelTreeElement(QubbleGUI gui) {
        super(gui, 0.0F, 20.0F, 100, gui.height - 20);
    }

    @Override
    public void init() {
        ElementHandler.INSTANCE.addElement(this.getGUI(), this.scroller = new ScrollerElement(this.getGUI(), 4));
    }

    @Override
    public void update() {
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        this.scroller.updateState(this, this.getWidth() - 8, 2.0F, 10, this.entryCount + 1);

        QubbleGUI gui = this.getGUI();
        float posX = this.getPosX();
        float posY = this.getPosY();
        float width = this.getWidth();
        float height = this.getHeight();

        this.startScissor();
        gui.drawRectangle(posX, posY, width, 15.0F, Qubble.CONFIG.getPrimarySubcolor());

        int i = 0;
        float offset = this.scroller.getScrollYOffset();
        for (float y = 15.0F - offset; y < height + offset; y += 10.0F) {
            gui.drawRectangle(posX, posY + y, width, 10.0F, i % 2 == 0 ? Qubble.CONFIG.getSecondarySubcolor() : Qubble.CONFIG.getPrimarySubcolor());
            i++;
        }

        if (gui.getSelectedModel() != null) {
            this.cubeY = 0;
            QubbleModel model = gui.getSelectedModel();
            for (QubbleCube cube : model.getCubes()) {
                this.drawCubeEntry(cube, 0);
            }
        }

        this.entryCount = this.cubeY;

        FontRenderer fontRenderer = ClientProxy.MINECRAFT.fontRendererObj;
        fontRenderer.drawString("Model Tree", posX + 4, posY + 4, Qubble.CONFIG.getTextColor(), false);
        gui.drawRectangle(posX + width - 2, posY, 2, height, Qubble.CONFIG.getAccentColor());

        this.endScissor();
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (mouseX > this.getWidth() - 2 && mouseX < this.getWidth() && mouseY > this.getPosY()) {
            this.resizing = true;
            return true;
        }
        QubbleGUI gui = this.getGUI();
        if (button == 0) {
            if (gui.getSelectedModel() != null) {
                this.cubeY = 0;
                if (mouseX >= this.getPosX() && mouseX < this.getPosX() + this.getWidth() - 10 && mouseY >= this.getPosY() && mouseY < this.getPosY() + this.getHeight()) {
                    gui.setSelectedCube(null);
                }
                QubbleModel model = gui.getSelectedModel();
                for (QubbleCube cube : model.getCubes()) {
                    if (this.mouseDetectionCubeEntry(cube, 0, mouseX, mouseY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(float mouseX, float mouseY, int button, long timeSinceClick) {
        if (this.resizing) {
            this.setWidth((int) Math.max(50, Math.min(300, mouseX - this.getPosX())));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        this.resizing = false;
        return false;
    }

    private boolean mouseDetectionCubeEntry(QubbleCube cube, int xOffset, float mouseX, float mouseY) {
        float entryX = this.getPosX() + xOffset;
        float entryY = this.getPosY() + this.cubeY * 10.0F + 16.0F - this.scroller.getScrollYOffset();
        this.cubeY++;
        boolean expanded = this.isExpanded(cube);
        if (expanded) {
            for (QubbleCube child : cube.getChildren()) {
                this.mouseDetectionCubeEntry(child, xOffset + 6, mouseX, mouseY);
            }
        }
        if (cube.getChildren().size() > 0) {
            if (mouseX >= entryX + 2 && mouseX < entryX + 6 && mouseY >= entryY + 2 && mouseY < entryY + 6) {
                this.setExpanded(cube, !this.isExpanded(cube));
                return true;
            }
        }
        if (mouseX >= entryX + 10 && mouseX < entryX - xOffset + this.getWidth() - 10 && mouseY >= entryY && mouseY < entryY + 10) {
            this.getGUI().setSelectedCube(cube);
            return true;
        }
        return false;
    }

    private void drawCubeEntry(QubbleCube cube, int xOffset) {
        FontRenderer fontRenderer = ClientProxy.MINECRAFT.fontRendererObj;
        String name = cube.getName();
        float entryX = this.getPosX() + xOffset;
        float entryY = this.getPosY() + this.cubeY * 10.0F + 16.0F - this.scroller.getScrollYOffset();
        fontRenderer.drawString(name, entryX + 10, entryY, this.getGUI().getSelectedCube() == cube ? Qubble.CONFIG.getAccentColor() : Qubble.CONFIG.getTextColor(), false);
        this.cubeY++;
        boolean expanded = this.isExpanded(cube);
        int prevCubeY = this.cubeY;
        int size = 0;
        if (expanded) {
            int i = 0;
            for (QubbleCube child : cube.getChildren()) {
                if (i == cube.getChildren().size() - 1) {
                    size = (this.cubeY + 1) - prevCubeY;
                }
                this.drawCubeEntry(child, xOffset + 6);
                i++;
            }
        }
        int outlineColor = 0xFF9E9E9E;
        this.getGUI().drawRectangle(entryX + 1 - 6, entryY + 3.5, 11, 0.75, outlineColor);
        if (cube.getChildren().size() > 0) {
            if (expanded) {
                this.getGUI().drawRectangle(entryX + 1, entryY + 3.5, 0.75, (size) * 10.0F, outlineColor);
            }
            this.getGUI().drawRectangle(entryX + 2, entryY + 2, 4, 4, 0xFF464646);
            this.getGUI().drawRectangle(entryX + 3, entryY + 3.5, 2, 0.75, outlineColor);
            if (!expanded) {
                this.getGUI().drawRectangle(entryX + 3.75, entryY + 3, 0.75, 2, outlineColor);
            }
        }
    }

    private boolean isExpanded(QubbleCube cube) {
        return this.expandedCubes.contains(cube);
    }

    private void setExpanded(QubbleCube cube, boolean expanded) {
        boolean carryToChildren = GuiScreen.isShiftKeyDown();
        if (expanded) {
            if (!this.expandedCubes.contains(cube)) {
                this.expandedCubes.add(cube);
            }
        } else {
            this.expandedCubes.remove(cube);
            carryToChildren = true;
        }
        if (carryToChildren) {
            for (QubbleCube child : cube.getChildren()) {
                this.setExpanded(child, expanded);
            }
        }
    }
}
