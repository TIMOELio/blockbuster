package mchorse.blockbuster.client.gui;

import mchorse.blockbuster.capabilities.gun.Gun;
import mchorse.blockbuster.client.gui.dashboard.GuiDashboard;
import mchorse.blockbuster.common.GunProps;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.guns.PacketGunInfo;
import mchorse.blockbuster_pack.client.gui.GuiPosePanel.GuiPoseTransformations;
import mchorse.mclib.client.gui.framework.GuiBase;
import mchorse.mclib.client.gui.framework.elements.GuiButtonElement;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import mchorse.mclib.client.gui.framework.elements.GuiPanelBase;
import mchorse.mclib.client.gui.framework.elements.GuiTextElement;
import mchorse.mclib.client.gui.framework.elements.GuiTrackpadElement;
import mchorse.mclib.client.gui.framework.elements.IGuiElement;
import mchorse.mclib.client.gui.utils.Area;
import mchorse.mclib.client.gui.utils.Resizer.Measure;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import mchorse.metamorph.client.gui.elements.GuiCreativeMorphsMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiGun extends GuiBase
{
    public GunProps props;
    public int index;

    public GuiPanelBase<IGuiElement> panel;

    /* Morphs configuration */
    public GuiCreativeMorphsMenu morphs;

    /* Gun options */
    public GuiElement gunOptions;
    public GuiButtonElement<GuiButton> pickDefault;
    public GuiButtonElement<GuiButton> pickFiring;
    public GuiTextElement fireCommand;
    public GuiTrackpadElement delay;
    public GuiTrackpadElement projectiles;
    public GuiTrackpadElement accuracy;

    /* Projectile options */
    public GuiElement projectileOptions;
    public GuiButtonElement<GuiButton> pickProjectile;
    public GuiTextElement tickCommand;
    public GuiTrackpadElement ticking;
    public GuiTrackpadElement lifeSpan;
    public GuiButtonElement<GuiCheckBox> yaw;
    public GuiButtonElement<GuiCheckBox> pitch;
    public GuiButtonElement<GuiCheckBox> sequencer;
    public GuiButtonElement<GuiCheckBox> random;
    public GuiTrackpadElement hitboxX;
    public GuiTrackpadElement hitboxY;
    public GuiTrackpadElement speed;
    public GuiTrackpadElement friction;
    public GuiTrackpadElement gravity;

    /* Impact options */
    public GuiElement impactOptions;
    public GuiButtonElement<GuiButton> pickImpact;
    public GuiTextElement impactCommand;
    public GuiTrackpadElement impactDelay;
    public GuiButtonElement<GuiCheckBox> vanish;
    public GuiButtonElement<GuiCheckBox> bounce;
    public GuiTrackpadElement hits;
    public GuiTrackpadElement damage;

    /* Transforms */
    public GuiElement transformOptions;
    public GuiPoseTransformations gun;
    public GuiPoseTransformations projectile;

    public GuiGun(ItemStack stack)
    {
        this.props = Gun.get(stack).getProps();

        Minecraft mc = Minecraft.getMinecraft();
        IMorphing cap = Morphing.get(mc.thePlayer);

        /* Initialization of GUI elements */
        this.gunOptions = new GuiElement(mc);
        this.gunOptions.createChildren();
        this.projectileOptions = new GuiElement(mc);
        this.projectileOptions.createChildren();
        this.transformOptions = new GuiElement(mc);
        this.transformOptions.createChildren();
        this.impactOptions = new GuiElement(mc);
        this.impactOptions.createChildren();

        this.panel = new GuiPanelBase<IGuiElement>(mc);
        this.panel.setPanel(this.gunOptions);
        this.panel.registerPanel(this.gunOptions, GuiDashboard.GUI_ICONS, "Fire properties", 48, 0, 48, 16);
        this.panel.registerPanel(this.projectileOptions, GuiDashboard.GUI_ICONS, "Projectile properties", 32, 96, 32, 112);
        this.panel.registerPanel(this.impactOptions, GuiDashboard.GUI_ICONS, "Impact properties", 80, 64, 80, 80);
        this.panel.registerPanel(this.transformOptions, GuiDashboard.GUI_ICONS, "Transformations", 80, 32, 80, 48);

        this.morphs = new GuiCreativeMorphsMenu(mc, 6, null, cap);
        this.morphs.callback = (morph) -> this.setMorph(morph);
        this.morphs.setVisible(false);

        /* Gun options */
        Area area = this.gunOptions.area;

        this.pickDefault = GuiButtonElement.button(mc, "Default morph", (b) -> this.openMorphs(1));
        this.pickFiring = GuiButtonElement.button(mc, "Firing morph", (b) -> this.openMorphs(2));
        this.fireCommand = new GuiTextElement(mc, 10000, (value) -> this.props.fireCommand = value);
        this.delay = new GuiTrackpadElement(mc, "Delay", (value) -> this.props.delay = value.intValue());
        this.delay.setLimit(0, Integer.MAX_VALUE, true);
        this.projectiles = new GuiTrackpadElement(mc, "Projectiles", (value) -> this.props.projectiles = value.intValue());
        this.projectiles.setLimit(0, Integer.MAX_VALUE, true);
        this.accuracy = new GuiTrackpadElement(mc, "Accuracy", (value) -> this.props.accuracy = value);

        this.pickDefault.resizer().parent(area).set(0, 0, 100, 20).x(0.25F, -50).y(1, -100);
        this.pickFiring.resizer().parent(area).set(0, 0, 100, 20).x(0.75F, -50).y(1, -100);
        this.fireCommand.resizer().parent(area).set(10, 0, 0, 20).w(1, -20).y(1, -30);
        this.delay.resizer().relative(this.pickDefault.resizer()).set(0, 25, 100, 20);
        this.projectiles.resizer().relative(this.pickFiring.resizer()).set(0, 25, 100, 20);
        this.accuracy.resizer().parent(area).set(0, 0, 0, 20).x(0.25F, 55).y(1, -75).w(0.5F, -110);

        this.gunOptions.children.add(this.pickDefault, this.pickFiring, this.fireCommand, this.delay, this.accuracy, this.projectiles);

        /* Projectile options */
        area = this.projectileOptions.area;

        this.pickProjectile = GuiButtonElement.button(mc, "Projectile morph", (b) -> this.openMorphs(3));
        this.tickCommand = new GuiTextElement(mc, 10000, (value) -> this.props.tickCommand = value);
        this.ticking = new GuiTrackpadElement(mc, "Ticking", (value) -> this.props.ticking = value.intValue());
        this.ticking.setLimit(0, Integer.MAX_VALUE, true);
        this.lifeSpan = new GuiTrackpadElement(mc, "Life span", (value) -> this.props.lifeSpan = value.intValue());
        this.lifeSpan.setLimit(0, Integer.MAX_VALUE, true);
        this.yaw = GuiButtonElement.checkbox(mc, "Yaw", false, (b) -> this.props.yaw = b.button.isChecked());
        this.pitch = GuiButtonElement.checkbox(mc, "Pitch", false, (b) -> this.props.pitch = b.button.isChecked());
        this.sequencer = GuiButtonElement.checkbox(mc, "Sequencer", false, (b) -> this.props.sequencer = b.button.isChecked());
        this.random = GuiButtonElement.checkbox(mc, "Random", false, (b) -> this.props.random = b.button.isChecked());
        this.hitboxX = new GuiTrackpadElement(mc, "Hitbox X", (value) -> this.props.hitboxX = value);
        this.hitboxY = new GuiTrackpadElement(mc, "Hitbox Y", (value) -> this.props.hitboxY = value);
        this.speed = new GuiTrackpadElement(mc, "Speed", (value) -> this.props.speed = value);
        this.friction = new GuiTrackpadElement(mc, "Friction", (value) -> this.props.friction = value);
        this.gravity = new GuiTrackpadElement(mc, "Gravity", (value) -> this.props.gravity = value);

        this.pickProjectile.resizer().parent(area).set(0, 0, 100, 20).x(0.5F, -50).y(1, -60);
        this.tickCommand.resizer().parent(area).set(10, 0, 0, 20).w(1, -20).y(1, -30);
        this.ticking.resizer().parent(area).set(0, 10, 100, 20).x(1, -110);
        this.lifeSpan.resizer().relative(this.ticking.resizer()).set(0, 25, 100, 20);
        this.yaw.resizer().parent(area).relative(this.lifeSpan.resizer()).set(0, 25, 100, 11);
        this.pitch.resizer().relative(this.yaw.resizer()).set(0, 16, 100, 11);
        this.sequencer.resizer().relative(this.pitch.resizer()).set(0, 16, 100, 11);
        this.random.resizer().relative(this.sequencer.resizer()).set(0, 16, 100, 11);
        this.hitboxX.resizer().relative(this.gravity.resizer()).set(0, 25, 100, 20);
        this.hitboxY.resizer().relative(this.hitboxX.resizer()).set(0, 25, 100, 20);
        this.speed.resizer().parent(area).set(10, 10, 100, 20);
        this.friction.resizer().relative(this.speed.resizer()).set(0, 25, 100, 20);
        this.gravity.resizer().relative(this.friction.resizer()).set(0, 25, 100, 20);

        this.projectileOptions.children.add(this.pickProjectile, this.tickCommand);
        this.projectileOptions.children.add(this.yaw, this.pitch, this.sequencer, this.random);
        this.projectileOptions.children.add(this.ticking, this.hitboxX, this.hitboxY, this.lifeSpan, this.speed, this.friction, this.gravity);

        /* Impact options */
        area = this.impactOptions.area;

        this.pickImpact = GuiButtonElement.button(mc, "Impact morph", (b) -> this.openMorphs(4));
        this.impactDelay = new GuiTrackpadElement(mc, "Impact delay", (value) -> this.props.impactDelay = value.intValue());
        this.impactDelay.setLimit(0, Integer.MAX_VALUE, true);
        this.impactCommand = new GuiTextElement(mc, 10000, (value) -> this.props.impactCommand = value);
        this.vanish = GuiButtonElement.checkbox(mc, "Vanish", false, (b) -> this.props.vanish = b.button.isChecked());
        this.bounce = GuiButtonElement.checkbox(mc, "Bounce", false, (b) -> this.props.bounce = b.button.isChecked());
        this.hits = new GuiTrackpadElement(mc, "Hits", (value) -> this.props.hits = value.intValue());
        this.hits.setLimit(1, Integer.MAX_VALUE, true);
        this.damage = new GuiTrackpadElement(mc, "Damage", (value) -> this.props.damage = value);

        this.pickImpact.resizer().parent(area).set(0, 0, 100, 20).x(0.5F, -50).y(1, -60);
        this.impactCommand.resizer().parent(area).set(10, 0, 0, 20).w(1, -20).y(1, -30);
        this.impactDelay.resizer().relative(this.hits.resizer()).set(0, 25, 100, 20);
        this.vanish.resizer().relative(this.impactDelay.resizer()).set(0, 25, 100, 11);
        this.bounce.resizer().relative(this.vanish.resizer()).set(0, 16, 100, 11);
        this.hits.resizer().relative(this.damage.resizer()).set(0, 25, 100, 20);
        this.damage.resizer().parent(area).set(0, 10, 100, 20).x(1, -110);

        this.impactOptions.children.add(this.pickImpact, this.impactCommand, this.impactDelay, this.vanish, this.bounce);
        this.impactOptions.children.add(this.damage, this.hits);

        /* Gun transforms */
        area = this.transformOptions.area;

        this.gun = new GuiPoseTransformations(mc);
        this.projectile = new GuiPoseTransformations(mc);

        this.gun.resizer().parent(area).set(0, 30, 190, 70).x(0.5F, -95);
        this.projectile.resizer().parent(area).set(0, 30, 190, 70).x(0.5F, -95).y(1, -80);

        this.transformOptions.children.add(this.gun, this.projectile);

        /* Placement of the elements */
        this.morphs.resizer().parent(this.area).set(0, 0, 1, 1, Measure.RELATIVE);
        this.panel.resizer().parent(this.area).set(0, 35, 0, 0).w(1, 0).h(1, -35);

        /* Gun properties */
        this.fireCommand.setText(this.props.fireCommand);
        this.delay.setValue(this.props.delay);
        this.projectiles.setValue(this.props.projectiles);
        this.accuracy.setValue(this.props.accuracy);

        /* Projectile properties */
        this.tickCommand.setText(this.props.tickCommand);
        this.ticking.setValue(this.props.ticking);
        this.lifeSpan.setValue(this.props.lifeSpan);
        this.yaw.button.setIsChecked(this.props.yaw);
        this.pitch.button.setIsChecked(this.props.pitch);
        this.sequencer.button.setIsChecked(this.props.sequencer);
        this.random.button.setIsChecked(this.props.random);
        this.hitboxX.setValue(this.props.hitboxX);
        this.hitboxY.setValue(this.props.hitboxY);
        this.speed.setValue(this.props.speed);
        this.friction.setValue(this.props.friction);
        this.gravity.setValue(this.props.gravity);

        /* Impact properties */
        this.impactCommand.setText(this.props.impactCommand);
        this.impactDelay.setValue(this.props.impactDelay);
        this.vanish.button.setIsChecked(this.props.vanish);
        this.bounce.button.setIsChecked(this.props.bounce);
        this.hits.setValue(this.props.hits);
        this.damage.setValue(this.props.damage);

        /* Gun transforms */
        this.gun.set(this.props.gunTransform);
        this.projectile.set(this.props.projectileTransform);

        this.elements.add(this.panel, this.morphs);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    public void initGui()
    {
        this.area.set(0, 0, this.width, this.height);
        this.elements.resize(this.width, this.height);
    }

    private void openMorphs(int i)
    {
        AbstractMorph morph = this.props.defaultMorph;

        if (i == 2) morph = this.props.firingMorph;
        else if (i == 3) morph = this.props.projectileMorph;
        else if (i == 4) morph = this.props.impactMorph;

        this.index = i;
        this.morphs.setVisible(true);
        this.morphs.setSelected(morph);
    }

    private void setMorph(AbstractMorph morph)
    {
        if (this.index == 1) this.props.defaultMorph = morph;
        else if (this.index == 2) this.props.firingMorph = morph;
        else if (this.index == 3) this.props.projectileMorph = morph;
        else if (this.index == 4) this.props.impactMorph = morph;
    }

    @Override
    protected void closeScreen()
    {
        super.closeScreen();

        Dispatcher.sendToServer(new PacketGunInfo(this.props.toNBT(), 0));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();

        Gui.drawRect(0, 0, this.width, 35, 0x88000000);
        this.drawGradientRect(0, 35, this.width, 45, 0x88000000, 0x00000000);
        this.fontRendererObj.drawStringWithShadow("Gun configuration", 10, 15, 0xffffffff);

        EntityPlayer player = this.mc.thePlayer;
        int w = this.area.w / 4;

        if (this.panel.view.delegate == this.gunOptions)
        {
            if (this.props.defaultMorph != null)
            {
                this.props.defaultMorph.renderOnScreen(player, this.area.getX(0.5F) - w, this.area.getY(0.5F), w * 0.5F, 1);
            }

            if (this.props.firingMorph != null)
            {
                this.props.firingMorph.renderOnScreen(player, this.area.getX(0.5F) + w, this.area.getY(0.5F), w * 0.5F, 1);
            }

            this.fontRendererObj.drawStringWithShadow("Command on fire", this.fireCommand.area.x, this.fireCommand.area.y - 12, 0xffffff);
        }
        else if (this.panel.view.delegate == this.projectileOptions)
        {
            if (this.props.projectileMorph != null)
            {
                this.props.projectileMorph.renderOnScreen(player, this.area.getX(0.5F), this.area.getY(0.5F), w * 0.5F, 1);
            }

            this.fontRendererObj.drawStringWithShadow("Command on tick", this.tickCommand.area.x, this.tickCommand.area.y - 12, 0xffffff);
        }
        else if (this.panel.view.delegate == this.impactOptions)
        {
            if (this.props.impactMorph != null)
            {
                this.props.impactMorph.renderOnScreen(player, this.area.getX(0.5F), this.area.getY(0.5F), w * 0.5F, 1);
            }

            this.fontRendererObj.drawStringWithShadow("Command on impact", this.impactCommand.area.x, this.impactCommand.area.y - 12, 0xffffff);
        }
        else if (this.panel.view.delegate == this.transformOptions)
        {
            this.drawCenteredString(this.fontRendererObj, "Gun transforms", this.gun.area.getX(0.5F), this.gun.area.y - 28, 0xffffff);
            this.drawCenteredString(this.fontRendererObj, "Projectile transforms", this.projectile.area.getX(0.5F), this.projectile.area.y - 28, 0xffffff);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}