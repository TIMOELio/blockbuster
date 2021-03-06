package mchorse.blockbuster.aperture.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import mchorse.aperture.camera.CameraAPI;
import mchorse.aperture.camera.destination.AbstractDestination;
import mchorse.aperture.camera.destination.ClientDestination;
import mchorse.aperture.client.gui.GuiCameraEditor;
import mchorse.blockbuster.aperture.CameraHandler;
import mchorse.blockbuster.aperture.network.common.PacketPlaybackButton;
import mchorse.blockbuster.aperture.network.common.PacketRequestProfiles;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.mclib.client.gui.utils.GuiUtils;
import mchorse.mclib.client.gui.utils.ScrollArea;
import mchorse.mclib.client.gui.widgets.buttons.GuiCirculate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;

/**
 * GUI playback
 *
 * This GUI is responsible for attaching some properties to playback button's
 * ItemStack.
 */
public class GuiPlayback extends GuiScreen
{
    private String stringTitle = I18n.format("blockbuster.gui.playback.title");
    private String stringCameraMode = I18n.format("blockbuster.gui.playback.camera_mode");
    private String stringProfile = I18n.format("blockbuster.gui.playback.profile");

    private GuiCirculate cameraMode;
    private GuiButton done;

    public List<AbstractDestination> profiles = new ArrayList<AbstractDestination>();
    public ScrollArea area = new ScrollArea(20);
    public int index = -1;
    public String profile;

    public GuiPlayback()
    {
        for (String filename : CameraAPI.getClientProfiles())
        {
            this.profiles.add(new ClientDestination(filename));
        }

        if (CameraHandler.server)
        {
            Dispatcher.sendToServer(new PacketRequestProfiles());
        }
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    /* Input */

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();

        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (this.area.isInside(x, y))
        {
            int scroll = -Mouse.getEventDWheel();

            if (scroll != 0)
            {
                this.area.mouseScroll(x, y, scroll);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.area.mouseClicked(mouseX, mouseY))
        {
            return;
        }

        if (this.area.isInside(mouseX, mouseY))
        {
            int index = this.area.getIndex(mouseX, mouseY);

            this.index = index;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);

        this.area.mouseReleased(mouseX, mouseY);
    }

    /* Actions */

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        int id = button.id;

        if (id == 1)
        {
            this.setValue(this.cameraMode.getValue());
        }
        else if (id == 2)
        {
            this.saveAndQuit();
        }
    }

    private void saveAndQuit()
    {
        Dispatcher.sendToServer(new PacketPlaybackButton(this.cameraMode.getValue(), this.getSelected()));

        this.mc.displayGuiScreen(null);
    }

    private String getSelected()
    {
        if (this.index >= 0 && this.index < this.profiles.size())
        {
            return this.profiles.get(this.index).toResourceLocation().toString();
        }

        return "";
    }

    /* GUI and drawing */

    @Override
    public void initGui()
    {
        int x = this.width / 2 - 75;
        int w = 150;

        this.cameraMode = new GuiCirculate(1, x, this.height - 55, w, 20);
        this.done = new GuiButton(2, x, this.height - 30, w, 20, I18n.format("blockbuster.gui.done"));

        this.buttonList.add(this.cameraMode);
        this.buttonList.add(this.done);

        this.cameraMode.addLabel(I18n.format("blockbuster.gui.playback.nothing"));
        this.cameraMode.addLabel(I18n.format("blockbuster.gui.playback.play"));
        this.cameraMode.addLabel(I18n.format("blockbuster.gui.playback.load_profile"));

        this.area.set(this.width / 2 - 75, 45, 150, this.height - 125);

        NBTTagCompound compound = Minecraft.getMinecraft().thePlayer.getHeldItemMainhand().getTagCompound();

        if (compound.hasKey("CameraPlay"))
        {
            this.setValue(1);
        }
        else if (compound.hasKey("CameraProfile"))
        {
            this.setValue(2, compound.getString("CameraProfile"));
        }
        else
        {
            this.setValue(0);
        }
    }

    private void setValue(int value)
    {
        this.setValue(value, "");
        this.selectProfile();
    }

    private void setValue(int value, String profile)
    {
        this.cameraMode.setValue(value);
        this.profile = profile;
    }

    public void selectProfile()
    {
        this.index = -1;

        int i = 0;

        for (AbstractDestination dest : this.profiles)
        {
            if (dest.toResourceLocation().toString().equals(this.profile))
            {
                this.index = i;

                break;
            }

            i++;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        boolean isCameraProfile = this.cameraMode.getValue() == 2;

        int x = this.width / 2 - 75;

        this.drawDefaultBackground();
        this.drawString(this.fontRendererObj, this.stringTitle, x, 10, 0xffffffff);
        this.drawString(this.fontRendererObj, this.stringCameraMode, x, this.height - 69, 0xffcccccc);

        if (isCameraProfile)
        {
            this.drawString(this.fontRendererObj, this.stringProfile, x, 35 - 4, 0xffcccccc);
        }

        this.drawProfiles(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Draw camera profiles
     */
    private void drawProfiles(int mouseX, int mouseY)
    {
        boolean isCameraProfile = this.cameraMode.getValue() == 2;

        if (isCameraProfile)
        {
            this.area.drag(mouseX, mouseY);

            int x = this.area.x;
            int y = this.area.y;
            int w = this.area.w;

            Gui.drawRect(x, y, x + w, y + this.area.h, 0x88000000);
            GuiUtils.scissor(x, y, w, this.area.h, this.width, this.height);

            y -= this.area.scroll;

            int i = 0;

            for (AbstractDestination dest : this.profiles)
            {
                boolean hovered = this.area.isInside(mouseX, mouseY) && mouseY >= y && mouseY < y + this.area.scrollItemSize;
                boolean current = this.index == i;

                if (hovered || current)
                {
                    Gui.drawRect(x, y, x + w, y + this.area.scrollItemSize, current ? 0x880088ff : 0x88000000);
                }

                this.mc.fontRendererObj.drawStringWithShadow(dest.getFilename(), x + 22, y + 7, 0xffffff);
                this.mc.renderEngine.bindTexture(GuiCameraEditor.EDITOR_TEXTURE);

                GlStateManager.color(1, 1, 1, 1);
                Gui.drawModalRectWithCustomSizedTexture(x + 2, y + 2, 0 + (dest instanceof ClientDestination ? 16 : 0), 32, 16, 16, 256, 256);

                y += this.area.scrollItemSize;
                i++;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            this.area.drawScrollbar();
        }
    }
}