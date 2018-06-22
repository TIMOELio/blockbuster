package mchorse.blockbuster.client.gui.dashboard.panels.model_editor.modals;

import java.util.Collection;
import java.util.function.Consumer;

import mchorse.blockbuster.client.gui.framework.elements.GuiButtonElement;
import mchorse.blockbuster.client.gui.framework.elements.GuiDelegateElement;
import mchorse.blockbuster.client.gui.framework.elements.list.GuiStringListElement;
import mchorse.blockbuster.client.gui.utils.Resizer.Measure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class GuiListModal extends GuiModal
{
    public Consumer<String> callback;
    public String label;

    private GuiButtonElement<GuiButton> pick;
    private GuiButtonElement<GuiButton> cancel;
    private GuiStringListElement limbs;

    public GuiListModal(Minecraft mc, GuiDelegateElement parent, String label, Consumer<String> callback)
    {
        super(mc, parent);

        this.callback = callback;
        this.label = label;

        this.pick = GuiButtonElement.button(mc, "Pick", (b) -> this.send());
        this.cancel = GuiButtonElement.button(mc, "Cancel", (b) -> this.parent.setDelegate(null));
        this.limbs = new GuiStringListElement(mc, null);

        this.pick.resizer().set(0, 0, 50, 20).parent(this.area);
        this.pick.resizer().x.set(0.5F, Measure.RELATIVE, -55);
        this.pick.resizer().y.set(0.7F, Measure.RELATIVE, 10);
        this.cancel.resizer().set(60, 0, 50, 20).relative(this.pick.resizer());

        this.limbs.resizer().set(0, 0, 100, 0).parent(this.area);
        this.limbs.resizer().x.set(0.5F, Measure.RELATIVE, -50);
        this.limbs.resizer().y.set(0.4F, Measure.RELATIVE);
        this.limbs.resizer().h.set(0.3F, Measure.RELATIVE);
        this.limbs.add("(none)");
        this.limbs.current = 0;

        this.children.add(this.pick, this.cancel, this.limbs);
    }

    public GuiListModal setValue(String parent)
    {
        if (parent.isEmpty())
        {
            this.limbs.current = 0;
        }
        else
        {
            this.limbs.setCurrent(parent);
        }

        return this;
    }

    public GuiListModal addValues(Collection<String> values)
    {
        this.limbs.add(values);

        return this;
    }

    private void send()
    {
        String parent = this.limbs.getCurrent();

        if (this.limbs.current == -1)
        {
            return;
        }

        this.parent.setDelegate(null);

        if (this.callback != null)
        {
            this.callback.accept(this.limbs.current == 0 ? "" : parent);
        }
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks)
    {
        super.draw(mouseX, mouseY, partialTicks);

        this.font.drawSplitString(this.label, this.area.getX(0.15F), this.area.getY(0.1F), (int) (this.area.w * 0.7), 0xffffff);
    }
}