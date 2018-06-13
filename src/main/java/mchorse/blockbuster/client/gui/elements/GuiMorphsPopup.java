package mchorse.blockbuster.client.gui.elements;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import mchorse.blockbuster.api.Model.Pose;
import mchorse.blockbuster.api.Model.Transform;
import mchorse.blockbuster.client.gui.framework.elements.GuiButtonElement;
import mchorse.blockbuster.client.gui.framework.elements.GuiElement;
import mchorse.blockbuster.client.gui.framework.elements.GuiElements;
import mchorse.blockbuster.client.gui.framework.elements.GuiListElement;
import mchorse.blockbuster.client.gui.framework.elements.GuiTrackpadElement;
import mchorse.blockbuster.client.gui.utils.Area;
import mchorse.blockbuster.client.gui.utils.Resizer.Measure;
import mchorse.blockbuster_pack.morphs.CustomMorph;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.client.gui.elements.GuiCreativeMorphs;
import mchorse.metamorph.client.gui.elements.GuiCreativeMorphs.MorphCell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

/**
 * Creative morphs GUI picker
 *
 * This class is responsible for controlling {@link GuiCreativeMorphs}.
 */
public class GuiMorphsPopup extends GuiScreen
{
    /* GUI fields */
    public GuiTextField search;
    public GuiButton close;
    public GuiButton poses;
    public Consumer<AbstractMorph> callback;
    private GuiCreativeMorphs morphs;
    private AbstractMorph lastMorph;

    /* Poser */
    private Area area = new Area();
    private boolean hidden = true;

    private GuiElements elements = new GuiElements();
    private GuiTrackpadElement tx;
    private GuiTrackpadElement ty;
    private GuiTrackpadElement tz;

    private GuiTrackpadElement sx;
    private GuiTrackpadElement sy;
    private GuiTrackpadElement sz;

    private GuiTrackpadElement rx;
    private GuiTrackpadElement ry;
    private GuiTrackpadElement rz;

    private GuiListElement list;

    private Pose pose;
    private Transform trans;

    public GuiMorphsPopup(int perRow, AbstractMorph selected, IMorphing morphing)
    {
        this.morphs = new GuiCreativeMorphs(perRow, selected, morphing);
        this.morphs.setHidden(true);
        this.morphs.shiftX = 8;

        Minecraft mc = Minecraft.getMinecraft();

        this.elements.setVisible(false);
        this.tx = new GuiTrackpadElement(mc, "X", (value) -> this.trans.translate[0] = value);
        this.ty = new GuiTrackpadElement(mc, "Y", (value) -> this.trans.translate[1] = value);
        this.tz = new GuiTrackpadElement(mc, "Z", (value) -> this.trans.translate[2] = value);

        this.tx.resizer().set(0, 40, 60, 20).parent(this.area).x.set(1, Measure.RELATIVE, -65);
        this.tx.resizer().y.set(0.5F, Measure.RELATIVE, -40);
        this.ty.resizer().set(0, 25, 60, 20).relative(this.tx.resizer());
        this.tz.resizer().set(0, 25, 60, 20).relative(this.ty.resizer());

        this.sx = new GuiTrackpadElement(mc, "X", (value) -> this.trans.scale[0] = value);
        this.sy = new GuiTrackpadElement(mc, "Y", (value) -> this.trans.scale[1] = value);
        this.sz = new GuiTrackpadElement(mc, "Z", (value) -> this.trans.scale[2] = value);

        this.sx.resizer().set(0, 40, 60, 20).parent(this.area).x.set(1, Measure.RELATIVE, -130);
        this.sx.resizer().y.set(0.5F, Measure.RELATIVE, -40);
        this.sy.resizer().set(0, 25, 60, 20).relative(this.sx.resizer());
        this.sz.resizer().set(0, 25, 60, 20).relative(this.sy.resizer());

        this.rx = new GuiTrackpadElement(mc, "X", (value) -> this.trans.rotate[0] = value);
        this.ry = new GuiTrackpadElement(mc, "Y", (value) -> this.trans.rotate[1] = value);
        this.rz = new GuiTrackpadElement(mc, "Z", (value) -> this.trans.rotate[2] = value);

        this.rx.resizer().set(0, 40, 60, 20).parent(this.area).x.set(1, Measure.RELATIVE, -130 - 65);
        this.rx.resizer().y.set(0.5F, Measure.RELATIVE, -40);
        this.ry.resizer().set(0, 25, 60, 20).relative(this.rx.resizer());
        this.rz.resizer().set(0, 25, 60, 20).relative(this.ry.resizer());

        GuiElement element = GuiButtonElement.button(mc, "Reset pose", (b) -> this.reset());

        element.resizer().set(0, 50, 80, 20).parent(this.area).x.set(0.5F, Measure.RELATIVE, -40);
        element.resizer().y.set(1, Measure.RELATIVE, -30);
        this.elements.add(element);

        element = this.list = new GuiListElement(mc, (str) -> this.setTransform(this.pose.limbs.get(str)));
        element.resizer().parent(this.area).set(5, 30, 80, 90).h.set(1, Measure.RELATIVE, -40);
        this.elements.add(element);

        this.elements.add(this.tx, this.ty, this.tz, this.sx, this.sy, this.sz, this.rx, this.ry, this.rz);
    }

    public MorphCell getSelected()
    {
        return this.morphs.getSelected();
    }

    public void setSelected(AbstractMorph morph)
    {
        /* TODO: make creative morph menu add the morph if it doesn't
         * exists */
        this.morphs.setSelected(morph);
        this.lastMorph = morph;

        /* Saving the custom pose */
        if (morph instanceof CustomMorph)
        {
            MorphCell cell = this.getSelected();

            if (cell != null && cell.current().morph instanceof CustomMorph)
            {
                ((CustomMorph) cell.current().morph).customPose = ((CustomMorph) morph).customPose;
            }
        }

        if (this.poses != null)
        {
            this.poses.enabled = this.lastMorph instanceof CustomMorph;
        }
    }

    public void setTransform(Transform trans)
    {
        this.trans = trans;

        if (trans != null)
        {
            this.tx.trackpad.setValue(trans.translate[0]);
            this.ty.trackpad.setValue(trans.translate[1]);
            this.tz.trackpad.setValue(trans.translate[2]);

            this.sx.trackpad.setValue(trans.scale[0]);
            this.sy.trackpad.setValue(trans.scale[1]);
            this.sz.trackpad.setValue(trans.scale[2]);

            this.rx.trackpad.setValue(trans.rotate[0]);
            this.ry.trackpad.setValue(trans.rotate[1]);
            this.rz.trackpad.setValue(trans.rotate[2]);
        }
    }

    public void hide(boolean hide)
    {
        this.hidden = hide;
        this.morphs.setHidden(hide);
        this.elements.setVisible(false);
    }

    public boolean isHidden()
    {
        return this.hidden;
    }

    public void updateRect(int x, int y, int w, int h)
    {
        this.area.set(x, y, w, h);

        this.morphs.updateRect(x, y + 25, w, h - 25);
        this.morphs.setPerRow((int) Math.ceil(w / 54.0F));
    }

    public boolean isInside(int x, int y)
    {
        return !this.isHidden() && this.area.isInside(x, y);
    }

    /* Input */

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 1)
        {
            this.hide(true);
        }
        else if (button.id == 2)
        {
            this.morphs.setHidden(!this.morphs.getHidden());
            this.elements.setVisible(this.morphs.getHidden());

            if (this.elements.isVisible())
            {
                CustomMorph morph = (CustomMorph) this.lastMorph;

                if (morph.customPose == null)
                {
                    this.pose = morph.getPose(this.mc.player).clone();
                    morph.customPose = this.pose;
                }
                else
                {
                    this.pose = morph.customPose;
                }

                Map.Entry<String, Transform> entry = this.pose.limbs.entrySet().iterator().next();

                this.setTransform(entry.getValue());

                this.list.list.clear();
                this.list.list.addAll(this.pose.limbs.keySet());
                this.list.scroll.setSize(this.list.list.size());
                this.list.current = this.list.list.indexOf(entry.getKey());
            }
        }
    }

    private void reset()
    {
        this.morphs.setHidden(!this.morphs.getHidden());
        this.elements.setVisible(this.morphs.getHidden());

        CustomMorph morph = (CustomMorph) this.lastMorph;

        morph.customPose = null;
        this.setTransform(null);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height)
    {
        super.setWorldAndResolution(mc, width, height);
        this.morphs.setWorldAndResolution(mc, width, height);
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        MorphCell cell = this.morphs.getSelected();
        this.lastMorph = cell == null ? null : cell.current().morph;

        this.morphs.handleMouseInput();
        super.handleMouseInput();

        cell = this.morphs.getSelected();
        AbstractMorph morph = cell == null ? null : cell.current().morph;

        if (this.lastMorph != morph)
        {
            this.poses.enabled = morph instanceof CustomMorph;

            if (this.callback != null)
            {
                this.callback.accept(morph);
            }
        }

        /* Firing a mouse scroll event */
        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = -Mouse.getEventDWheel();

        if (scroll == 0)
        {
            return;
        }

        if (this.elements.isEnabled())
        {
            this.elements.mouseScrolled(x, y, scroll);
        }
    }

    /**
     * This method is responsible for hiding this popup when clicked outside of
     * this popup.
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (this.isHidden())
        {
            return;
        }

        if (!this.isInside(mouseX, mouseY))
        {
            this.morphs.setHidden(true);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.search.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.elements.isEnabled())
        {
            this.elements.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.isHidden())
        {
            return;
        }

        if (this.elements.isEnabled())
        {
            this.elements.mouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * This method is responsible for scrolling morphs, typing in the search
     * bar and finally it also responsible for setting up filter for morphs.
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.isHidden())
        {
            return;
        }

        super.keyTyped(typedChar, keyCode);

        this.search.textboxKeyTyped(typedChar, keyCode);

        if (this.search.isFocused())
        {
            this.morphs.setFilter(this.search.getText());
        }
        else if (!this.morphs.getHidden())
        {
            if (keyCode == Keyboard.KEY_DOWN)
            {
                this.morphs.scrollBy(30);
            }
            else if (keyCode == Keyboard.KEY_UP)
            {
                this.morphs.scrollBy(-30);
            }
            else if (keyCode == Keyboard.KEY_LEFT)
            {
                this.morphs.scrollTo(0);
            }
            else if (keyCode == Keyboard.KEY_RIGHT)
            {
                this.morphs.scrollTo(this.morphs.getHeight());
            }
        }
        else if (this.elements.isEnabled())
        {
            this.elements.keyTyped(typedChar, keyCode);
        }
    }

    /* GUI */

    /**
     * Initiate the search bar
     */
    @Override
    public void initGui()
    {
        this.search = new GuiTextField(0, this.fontRenderer, this.area.x + 61 - 3, this.area.y + 4, this.area.w - 87 - 65, 18);
        this.close = new GuiButton(1, this.area.x + this.area.w - 23, this.area.y + 3, 20, 20, "X");
        this.poses = new GuiButton(2, this.area.x + this.area.w - 23 - 65, this.area.y + 3, 60, 20, "Pose");
        this.poses.enabled = this.lastMorph instanceof CustomMorph;

        this.buttonList.add(this.close);
        this.buttonList.add(this.poses);

        this.elements.resize(this.width, this.height);
    }

    /* Rendering */

    /**
     * Render popup
     *
     * This popup won't be rendered if the morphs picker is hidden.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.isHidden())
        {
            return;
        }

        Gui.drawRect(this.area.x, this.area.y, this.area.getX(1), this.area.getY(1), 0xcc000000);
        this.fontRenderer.drawStringWithShadow(I18n.format("blockbuster.gui.search"), this.area.x + 9, this.area.y + 9, 0xffffffff);

        this.search.drawTextBox();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 120);
        this.morphs.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();

        MorphCell cell = this.morphs.getSelected();

        if (this.elements.isVisible())
        {
            this.elements.draw(mouseX, mouseY, partialTicks);
            this.lastMorph.renderOnScreen(this.mc.player, this.area.getX(0.33F), this.area.getY(0.8F), this.area.h / 3, 1);
        }
        else if (cell != null)
        {
            int width = Math.max(this.fontRenderer.getStringWidth(cell.current().name), this.fontRenderer.getStringWidth(cell.current().morph.name)) + 6;
            int center = this.area.getX(0.5F);
            int y = this.area.y + 34;

            Gui.drawRect(center - width / 2, y - 4, center + width / 2, y + 24, 0xcc000000);

            this.drawCenteredString(this.fontRenderer, cell.current().name, center, y, 0xffffff);
            this.drawCenteredString(this.fontRenderer, cell.current().morph.name, center, y + 14, 0x888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}