package net.ilexiconn.qubble.client.gui.element;

import net.ilexiconn.llibrary.client.model.qubble.QubbleCube;
import net.ilexiconn.llibrary.client.model.qubble.QubbleModel;
import net.ilexiconn.qubble.Qubble;
import net.ilexiconn.qubble.client.ClientProxy;
import net.ilexiconn.qubble.client.gui.Project;
import net.ilexiconn.qubble.client.gui.QubbleGUI;
import net.ilexiconn.qubble.server.color.ColorScheme;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelTreeElement extends Element<QubbleGUI> {
    private boolean resizing;
    private int cubeY;
    private int entryCount;
    private List<QubbleCube> expandedCubes = new ArrayList<>();

    private ScrollBarElement scroller;

    private QubbleCube parenting;

    public ModelTreeElement(QubbleGUI gui) {
        super(gui, 0.0F, 20.0F, 100, gui.height - 36);
    }

    @Override
    public void init() {
        ElementHandler.INSTANCE.addElement(this.getGUI(), this.scroller = new ScrollBarElement(this.getGUI(), this, () -> this.getWidth() - 8.0F, () -> 2.0F, () -> (float) this.getHeight(), 12, () -> this.entryCount));
        ElementHandler.INSTANCE.addElement(this.getGUI(), new ButtonElement(this.getGUI(), "+", this.getPosX(), this.getPosY() + this.getHeight(), 16, 16, (button) -> {
            WindowElement createCubeWindow = new WindowElement(this.getGUI(), "Create Cube", 100, 42);
            InputElement nameElement = new InputElement(this.getGUI(), "Cube Name", 2, 16, 96);
            createCubeWindow.addElement(nameElement);
            createCubeWindow.addElement(new ButtonElement(this.getGUI(), "Create", 2, 30, 96, 10, (element) -> {
                Project selectedProject = this.getGUI().getSelectedProject();
                if (selectedProject != null && selectedProject.getModel() != null && nameElement.getText().length() > 0) {
                    QubbleCube cube = QubbleCube.create(nameElement.getText());
                    cube.setDimensions(1, 1, 1);
                    cube.setScale(1.0F, 1.0F, 1.0F);
                    selectedProject.getModel().getCubes().add(cube);
                    selectedProject.setSelectedCube(cube);
                    this.getGUI().getModelView().updateModel();
                    ElementHandler.INSTANCE.removeElement(this.getGUI(), createCubeWindow);
                    return true;
                }
                return false;
            }).withColorScheme(ColorScheme.WINDOW));
            ElementHandler.INSTANCE.addElement(this.getGUI(), createCubeWindow);
            return true;
        }));
        ElementHandler.INSTANCE.addElement(this.getGUI(), new ButtonElement(this.getGUI(), "-", this.getPosX() + 16, this.getPosY() + this.getHeight(), 16, 16, (button) -> {
            Project selectedProject = this.getGUI().getSelectedProject();
            if (selectedProject != null && selectedProject.getModel() != null && selectedProject.getSelectedCube() != null) {
                this.removeCube(selectedProject);
            }
            return true;
        }));
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        QubbleGUI gui = this.getGUI();
        float posX = this.getPosX();
        float posY = this.getPosY();
        float width = this.getWidth();
        float height = this.getHeight();

        this.startScissor();

        int i = 0;
        float offset = this.scroller.getScrollOffset();
        for (float y = -offset; y < height + offset; y += 12.0F) {
            gui.drawRectangle(posX, posY + y, width, 12.0F, i % 2 == 0 ? Qubble.CONFIG.getSecondarySubcolor() : Qubble.CONFIG.getPrimarySubcolor());
            i++;
        }

        this.cubeY = 0;

        if (gui.getSelectedProject() != null) {
            QubbleModel model = gui.getSelectedProject().getModel();
            for (QubbleCube cube : model.getCubes()) {
                this.drawCubeEntry(cube, 0);
            }
        }

        this.entryCount = this.cubeY;

        gui.drawRectangle(posX + width - 2, posY, 2, height, Qubble.CONFIG.getAccentColor());

        this.endScissor();

        if (this.parenting != null) {
            FontRenderer fontRenderer = ClientProxy.MINECRAFT.fontRendererObj;
            String name = this.parenting.getName();
            float entryX = mouseX - 12;
            float entryY = mouseY - 2;
            this.getGUI().drawRectangle(entryX + 9, entryY - 1, fontRenderer.getStringWidth(name) + 1, fontRenderer.FONT_HEIGHT + 1, Qubble.CONFIG.getSecondaryColor());
            fontRenderer.drawString(name, entryX + 10, entryY, Qubble.CONFIG.getAccentColor(), false);
        }

        gui.drawRectangle(posX, posY + height, this.getWidth(), 16, Qubble.CONFIG.getAccentColor());
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (mouseX > this.getWidth() - 2 && mouseX < this.getWidth() && mouseY > this.getPosY()) {
            this.resizing = true;
            return true;
        }
        if (button == 0) {
            if (this.getSelectedCube(mouseX, mouseY) != null) {
                return true;
            }
        }
        return false;
    }

    private QubbleCube getSelectedCube(float mouseX, float mouseY) {
        QubbleGUI gui = this.getGUI();
        if (gui.getSelectedProject() != null) {
            this.cubeY = 0;
            if (mouseX >= this.getPosX() && mouseX < this.getPosX() + this.getWidth() - 10 && mouseY >= this.getPosY() && mouseY < this.getPosY() + this.getHeight()) {
                gui.getSelectedProject().setSelectedCube(null);
            }
            QubbleModel model = gui.getSelectedProject().getModel();
            for (QubbleCube cube : model.getCubes()) {
                QubbleCube selected = this.mouseDetectionCubeEntry(cube, 0, mouseX, mouseY);
                if (selected != null) {
                    return selected;
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseDragged(float mouseX, float mouseY, int button, long timeSinceClick) {
        if (this.resizing) {
            this.setWidth((int) Math.max(50, Math.min(300, mouseX - this.getPosX())));
            return true;
        } else if (button == 0 && this.isSelected(mouseX, mouseY)) {
            if (this.parenting == null) {
                Project selectedProject = this.getGUI().getSelectedProject();
                if (this.getGUI().getSelectedProject() != null && selectedProject.getSelectedCube() != null) {
                    this.parenting = selectedProject.getSelectedCube();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        this.resizing = false;
        if (this.parenting != null) {
            Project selectedProject = this.getGUI().getSelectedProject();
            if (selectedProject != null && selectedProject.getModel() != null) {
                QubbleModel model = selectedProject.getModel();
                QubbleCube newParent = this.getSelectedCube(mouseX, mouseY);
                QubbleCube prevParent = this.getParent(model, parenting);
                if (!this.hasChild(parenting, newParent)) {
                    if (newParent != parenting) {
                        if (GuiScreen.isShiftKeyDown()) {
                            this.maintainParentTransformation(model, parenting);
                            if (newParent != null) {
                                this.inheritParentTransformation(model, parenting, newParent);
                            }
                        }
                        model.getCubes().remove(parenting);
                        if (newParent != parenting && newParent != null && newParent != prevParent) {
                            if (!newParent.getChildren().contains(parenting)) {
                                newParent.getChildren().add(parenting);
                            }
                        } else if (newParent == null) {
                            model.getCubes().add(parenting);
                        }
                        if (prevParent != null && newParent != prevParent) {
                            prevParent.getChildren().remove(parenting);
                        }
                        this.getGUI().getModelView().updateModel();
                    }
                }
            }
            this.parenting = null;
        }
        return false;
    }

    private void maintainParentTransformation(QubbleModel model, QubbleCube parenting) {
        this.applyTransformation(parenting, this.getParentTransformation(model, parenting, true, false));
    }

    private void inheritParentTransformation(QubbleModel model, QubbleCube parenting, QubbleCube newParent) {
        Matrix4d matrix = this.getParentTransformationMatrix(model, newParent, true, false);
        matrix.invert();
        matrix.mul(this.getParentTransformationMatrix(model, parenting, false, false));

        float[][] parentTransformation = this.getParentTransformation(matrix);
        this.applyTransformation(parenting, parentTransformation);
    }

    private void applyTransformation(QubbleCube parenting, float[][] parentTransformation) {
        parenting.setPosition(parentTransformation[0][0], parentTransformation[0][1], parentTransformation[0][2]);
        parenting.setRotation(parentTransformation[1][0], parentTransformation[1][1], parentTransformation[1][2]);
    }

    private QubbleCube mouseDetectionCubeEntry(QubbleCube cube, int xOffset, float mouseX, float mouseY) {
        float entryX = this.getPosX() + xOffset;
        float entryY = this.getPosY() + this.cubeY * 12.0F + 2.0F - this.scroller.getScrollOffset();
        this.cubeY++;
        boolean expanded = this.isExpanded(cube);
        if (expanded) {
            for (QubbleCube child : cube.getChildren()) {
                QubbleCube selected = this.mouseDetectionCubeEntry(child, xOffset + 6, mouseX, mouseY);
                if (selected != null) {
                    return selected;
                }
            }
        }
        if (cube.getChildren().size() > 0) {
            if (mouseX >= entryX + 2 && mouseX < entryX + 6 && mouseY >= entryY + 2 && mouseY < entryY + 6) {
                this.setExpanded(cube, !this.isExpanded(cube));
            }
        }
        if (mouseX >= entryX + 10 && mouseX < entryX - xOffset + this.getWidth() - 10 && mouseY >= entryY && mouseY < entryY + 10) {
            this.getGUI().getSelectedProject().setSelectedCube(cube);
            return cube;
        }
        return null;
    }

    private void drawCubeEntry(QubbleCube cube, int xOffset) {
        FontRenderer fontRenderer = ClientProxy.MINECRAFT.fontRendererObj;
        String name = cube.getName();
        float entryX = this.getPosX() + xOffset;
        float entryY = this.getPosY() + this.cubeY * 12.0F + 2.0F - this.scroller.getScrollOffset();
        if (!cube.equals(parenting)) {
            fontRenderer.drawString(name, entryX + 10, entryY, this.getGUI().getSelectedProject().getSelectedCube() == cube ? Qubble.CONFIG.getAccentColor() : Qubble.CONFIG.getTextColor(), false);
        }
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
        this.getGUI().drawRectangle(entryX - 5, entryY + 3.5, 11, 0.75, outlineColor);
        if (cube.getChildren().size() > 0) {
            if (expanded) {
                this.getGUI().drawRectangle(entryX + 1, entryY + 3.5, 0.75, size * 12.0F, outlineColor);
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

    @Override
    public boolean keyPressed(char character, int key) {
        Project selectedProject = this.getGUI().getSelectedProject();
        if (key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK && selectedProject != null && selectedProject.getSelectedCube() != null) {
            this.removeCube(selectedProject);
            return true;
        }
        return false;
    }

    private void removeCube(Project selectedProject) {
        QubbleCube selectedCube = selectedProject.getSelectedCube();
        for (QubbleCube currentCube : selectedProject.getModel().getCubes()) {
            if (this.removeChildCube(currentCube, selectedCube)) {
                break;
            }
        }
        selectedProject.getModel().getCubes().remove(selectedCube);
        this.getGUI().getModelView().updateModel();
        this.getGUI().getSidebar().clearFields();
    }

    private boolean removeChildCube(QubbleCube parent, QubbleCube cube) {
        boolean isChild = false;
        for (QubbleCube currentCube : parent.getChildren()) {
            if (currentCube.equals(cube)) {
                isChild = true;
                break;
            }
            if (this.removeChildCube(currentCube, cube)) {
                return true;
            }
        }
        if (isChild) {
            parent.getChildren().remove(cube);
            return true;
        }
        return false;
    }

    public QubbleCube getParent(QubbleModel model, QubbleCube cuboid) {
        for (QubbleCube currentCube : model.getCubes()) {
            QubbleCube foundParent = this.getParent(currentCube, cuboid);
            if (foundParent != null) {
                return foundParent;
            }
        }
        return null;
    }

    private QubbleCube getParent(QubbleCube parent, QubbleCube cuboid) {
        if (parent.getChildren().contains(cuboid)) {
            return parent;
        }
        for (QubbleCube child : parent.getChildren()) {
            QubbleCube foundParent = this.getParent(child, cuboid);
            if (foundParent != null) {
                return foundParent;
            }
        }
        return null;
    }

    private boolean hasChild(QubbleCube parent, QubbleCube child) {
        if (parent.getChildren().contains(child)) {
            return true;
        }
        for (QubbleCube c : parent.getChildren()) {
            boolean hasChild = this.hasChild(c, child);
            if (hasChild) {
                return true;
            }
        }
        return false;
    }

    private List<QubbleCube> getParents(QubbleModel model, QubbleCube cube, boolean ignoreSelf) {
        QubbleCube parent = cube;
        List<QubbleCube> parents = new ArrayList<>();
        if (!ignoreSelf) {
            parents.add(cube);
        }
        while ((parent = this.getParent(model, parent)) != null) {
            parents.add(parent);
        }
        Collections.reverse(parents);
        return parents;
    }

    private float[][] getParentTransformation(QubbleModel model, QubbleCube cube, boolean includeParents, boolean ignoreSelf) {
        return this.getParentTransformation(this.getParentTransformationMatrix(model, cube, includeParents, ignoreSelf));
    }

    private Matrix4d getParentTransformationMatrix(QubbleModel model, QubbleCube cube, boolean includeParents, boolean ignoreSelf) {
        List<QubbleCube> parentCubes = new ArrayList<>();
        if (includeParents) {
            parentCubes = this.getParents(model, cube, ignoreSelf);
        } else if (!ignoreSelf) {
            parentCubes.add(cube);
        }
        Matrix4d matrix = new Matrix4d();
        matrix.setIdentity();
        Matrix4d transform = new Matrix4d();
        for (QubbleCube child : parentCubes) {
            transform.setIdentity();
            transform.setTranslation(new Vector3d(child.getPositionX(), child.getPositionY(), child.getPositionZ()));
            matrix.mul(transform);
            transform.rotZ(child.getRotationZ() / 180 * Math.PI);
            matrix.mul(transform);
            transform.rotY(child.getRotationY() / 180 * Math.PI);
            matrix.mul(transform);
            transform.rotX(child.getRotationX() / 180 * Math.PI);
            matrix.mul(transform);
        }
        return matrix;
    }

    private float[][] getParentTransformation(Matrix4d matrix) {
        double sinRotationAngleY, cosRotationAngleY, sinRotationAngleX, cosRotationAngleX, sinRotationAngleZ, cosRotationAngleZ;
        sinRotationAngleY = -matrix.m20;
        cosRotationAngleY = Math.sqrt(1 - sinRotationAngleY * sinRotationAngleY);
        if (Math.abs(cosRotationAngleY) > 0.0001) {
            sinRotationAngleX = matrix.m21 / cosRotationAngleY;
            cosRotationAngleX = matrix.m22 / cosRotationAngleY;
            sinRotationAngleZ = matrix.m10 / cosRotationAngleY;
            cosRotationAngleZ = matrix.m00 / cosRotationAngleY;
        } else {
            sinRotationAngleX = -matrix.m12;
            cosRotationAngleX = matrix.m11;
            sinRotationAngleZ = 0;
            cosRotationAngleZ = 1;
        }
        float rotationAngleX = (float) (epsilon((float) Math.atan2(sinRotationAngleX, cosRotationAngleX)) / Math.PI * 180);
        float rotationAngleY = (float) (epsilon((float) Math.atan2(sinRotationAngleY, cosRotationAngleY)) / Math.PI * 180);
        float rotationAngleZ = (float) (epsilon((float) Math.atan2(sinRotationAngleZ, cosRotationAngleZ)) / Math.PI * 180);
        return new float[][]{{epsilon((float) matrix.m03), epsilon((float) matrix.m13), epsilon((float) matrix.m23)}, {rotationAngleX, rotationAngleY, rotationAngleZ}};
    }

    private float epsilon(float x) {
        return x < 0 ? x > -0.0001F ? 0 : x : x < 0.0001F ? 0 : x;
    }

    public boolean isParenting() {
        return this.parenting != null;
    }
}
