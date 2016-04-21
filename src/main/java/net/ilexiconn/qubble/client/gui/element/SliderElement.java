package net.ilexiconn.qubble.client.gui.element;

import net.ilexiconn.qubble.Qubble;
import net.ilexiconn.qubble.client.gui.QubbleGUI;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class SliderElement extends Element<QubbleGUI> {
    private float value;
    private Function<Float, Boolean> function;
    private boolean intValue;
    private DecimalFormat decimalFormat;
    private boolean editable;

    public SliderElement(QubbleGUI gui, float posX, float posY, Function<Float, Boolean> function) {
        this(gui, posX, posY, false, function);
    }

    public SliderElement(QubbleGUI gui, float posX, float posY, boolean intValue, Function<Float, Boolean> function) {
        super(gui, posX, posY, 38, 12);
        this.function = function;
        this.intValue = intValue;
        this.decimalFormat = new DecimalFormat("#.#");
    }

    @Override
    public void render(float mouseX, float mouseY, float partialTicks) {
        this.getGUI().drawRectangle(this.getPosX(), this.getPosY(), this.getWidth(), this.getHeight(), this.editable ? Qubble.CONFIG.getSecondaryColor() : Qubble.CONFIG.getSecondarySubcolor());
        boolean upperSelected = this.editable && this.isSelected(mouseX, mouseY) && mouseX >= this.getPosX() + this.getWidth() - 11 && mouseY < this.getPosY() + 6;
        boolean lowerSelected = this.editable && this.isSelected(mouseX, mouseY) && mouseX >= this.getPosX() + this.getWidth() - 11 && mouseY > this.getPosY() + 6;
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 11, this.getPosY(), 11, 6, this.editable ? upperSelected ? Qubble.CONFIG.getDarkAccentColor() : Qubble.CONFIG.getAccentColor() : Qubble.CONFIG.getTertiaryColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 11, this.getPosY() + 6, 11, 6, this.editable ? lowerSelected ? Qubble.CONFIG.getDarkAccentColor() : Qubble.CONFIG.getAccentColor() : Qubble.CONFIG.getTertiaryColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 8, this.getPosY() + 4, 5, 1, Qubble.CONFIG.getTextColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 7, this.getPosY() + 3, 3, 1, Qubble.CONFIG.getTextColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 6, this.getPosY() + 2, 1, 1, Qubble.CONFIG.getTextColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 8, this.getPosY() + 7, 5, 1, Qubble.CONFIG.getTextColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 7, this.getPosY() + 8, 3, 1, Qubble.CONFIG.getTextColor());
        this.getGUI().drawRectangle(this.getPosX() + this.getWidth() - 6, this.getPosY() + 9, 1, 1, Qubble.CONFIG.getTextColor()); //who needs good code, amirite?
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float scaleFactor = this.getGUI().getResolution().getScaleFactor();
        GL11.glScissor((int) (this.getPosX() * scaleFactor), (int) ((this.getGUI().height - (this.getPosY() + this.getHeight())) * scaleFactor), (int) ((this.getWidth() - 11) * scaleFactor), (int) (this.getHeight() * scaleFactor));
        String string = this.intValue ? String.valueOf(this.value).split("\\.")[0] : this.decimalFormat.format(this.value);
        if (string.equals("-0")) { //.-.
            string = "0";
        }
        this.getGUI().mc.fontRendererObj.drawString(string, this.getPosX() + 2, this.getPosY() + 3.0F, Qubble.CONFIG.getTextColor(), false);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (!this.editable) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        boolean upperSelected = this.isSelected(mouseX, mouseY) && mouseX >= this.getPosX() + this.getWidth() - 11 && mouseY < this.getPosY() + 6;
        boolean lowerSelected = this.isSelected(mouseX, mouseY) && mouseX >= this.getPosX() + this.getWidth() - 11 && mouseY > this.getPosY() + 6;
        if (upperSelected) {
            float newValue = this.intValue || GuiScreen.isShiftKeyDown() ? this.value + 1 : this.value + 0.1F;
            if (this.function.apply(newValue)) {
                this.getGUI().mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ui_button_click, 1.0F));
                this.value = newValue;
                return true;
            }
        } else if (lowerSelected) {
            float newValue = this.intValue || GuiScreen.isShiftKeyDown() ? this.value - 1 : this.value - 0.1F;
            if (this.function.apply(newValue)) {
                this.getGUI().mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ui_button_click, 1.0F));
                this.value = newValue;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}