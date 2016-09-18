package mchorse.blockbuster.common.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import mchorse.blockbuster.actor.Model;
import mchorse.blockbuster.actor.ModelHandler;
import mchorse.blockbuster.common.Blockbuster;
import mchorse.blockbuster.common.GuiHandler;
import mchorse.blockbuster.common.item.ItemActorConfig;
import mchorse.blockbuster.common.item.ItemPlayback;
import mchorse.blockbuster.common.item.ItemRegister;
import mchorse.blockbuster.common.tileentity.TileEntityDirector;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.PacketModifyActor;
import mchorse.blockbuster.recording.Mocap;
import mchorse.blockbuster.recording.actions.Action;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

/**
 * Actor entity class
 *
 * Actor entity class is responsible for recording player's actions and execute
 * them. I'm also thinking about giving them controllable AI settings so they
 * could be used without recording (like during the battles between two or more
 * actors).
 *
 * Also, it would be cool to add something like simple crowd control for bigger
 * scenes (like one from Van Helsing in beginning with big crowd with torches,
 * fire and stuff).
 */
public class EntityActor extends EntityCreature implements IEntityAdditionalSpawnData
{
    /**
     * Event list. Each tick there's might be added an event action which should
     * be performed by this actor. The events are injected by PlayThread.
     */
    public List<Action> eventsList = Collections.synchronizedList(new ArrayList<Action>());

    /**
     * Skin used by the actor. If empty - means default skin provided with this
     * mod.
     */
    public String skin = "";

    /**
     * Model which is used to display. If empty - means default model (steve)
     * provided with this mod.
     */
    public String model = "";

    /**
     * Model instance, used for setting the size of this entity in updateSize
     * method
     */
    private Model modelInstance;

    /**
     * Position of director's block (needed to start the playback of other
     * actors while recording this actor).
     */
    public BlockPos directorBlock;

    /**
     * Temporary solution for disallowing rendering of custom name tag in GUI.
     */
    public boolean renderName = true;

    /**
     * This field is needed to make actors invisible. This is helpful for
     * scenes with different characters, which isn't needed to be seen.
     */
    public boolean invisible = false;

    /**
     * Fake player used in some of methods like onBlockActivated to avoid
     * NullPointerException
     */
    public EntityPlayer fakePlayer;

    public EntityActor(World worldIn)
    {
        super(worldIn);

        this.fakePlayer = new EntityPlayer(worldIn, new GameProfile(null, "xXx_Fake_Player_420_xXx"))
        {
            @Override
            public boolean isSpectator()
            {
                return false;
            }

            @Override
            public boolean isCreative()
            {
                return false;
            }
        };
    }

    /**
     * Returns the Y Offset of this entity.
     *
     * Taken from EntityPlayer.
     */
    @Override
    public double getYOffset()
    {
        return -0.35D;
    }

    /**
     * Can't despawn an actor
     */
    @Override
    protected boolean canDespawn()
    {
        return false;
    }

    /**
     * Brutally stolen from EntityPlayer class
     */
    public void setElytraFlying(boolean isFlying)
    {
        this.setFlag(7, isFlying);
    }

    /**
     * This is also brutally stolen from EntityPlayer class, by the way, I don't
     * think that changing the height while sneaking can save player's life
     */
    protected void updateSize()
    {
        float width = this.width;
        float height = this.height;

        if (this.isElytraFlying())
        {
            width = 0.6F;
            height = 0.6F;

            if (this.modelInstance != null)
            {
                float[] pose = this.modelInstance.poses.get("flying").size;

                width = pose[0];
                height = pose[1];
            }
        }
        else if (this.isSneaking())
        {
            width = 0.6F;
            height = 1.65F;

            if (this.modelInstance != null)
            {
                float[] pose = this.modelInstance.poses.get("sneaking").size;

                width = pose[0];
                height = pose[1];
            }
        }
        else
        {
            width = 0.6F;
            height = 1.8F;

            if (this.modelInstance == null) this.updateModel();

            if (this.modelInstance != null)
            {
                float[] pose = this.modelInstance.poses.get("standing").size;

                width = pose[0];
                height = pose[1];
            }
        }

        this.setSize(width, height);
    }

    /**
     * Adjust the movement, limb swinging, and process action stuff.
     *
     * See process actions method for more information.
     */
    @Override
    public void onLivingUpdate()
    {
        this.updateSize();
        this.pickUpNearByItems();

        if (this.eventsList.size() > 0)
        {
            this.eventsList.remove(0).apply(this);
        }

        if (!this.worldObj.isRemote)
        {
            this.fakePlayer.posX = this.posX;
            this.fakePlayer.posY = this.posY;
            this.fakePlayer.posZ = this.posZ;
            this.fakePlayer.rotationYaw = this.rotationYaw;
            this.fakePlayer.rotationPitch = this.rotationPitch;
        }

        this.updateArmSwingProgress();

        /*
         * Taken from the EntityDragon, IDK what it does, but the same code was
         * in Mocap's EntityMocap (which is serves like an actor in Mocap mod)
         *
         * It looks like position and rotation interpolation, though
         */
        if (this.worldObj.isRemote && this.newPosRotationIncrements > 0)
        {
            double d5 = this.posX + (this.interpTargetX - this.posX) / this.newPosRotationIncrements;
            double d0 = this.posY + (this.interpTargetY - this.posY) / this.newPosRotationIncrements;
            double d1 = this.posZ + (this.interpTargetZ - this.posZ) / this.newPosRotationIncrements;
            double d2 = MathHelper.wrapDegrees(this.interpTargetYaw - this.rotationYaw);

            this.rotationYaw = 360 + (float) (this.rotationYaw + d2 / this.newPosRotationIncrements);
            this.rotationPitch = (float) (this.rotationPitch + (this.interpTargetPitch - this.rotationPitch) / this.newPosRotationIncrements);
            this.newPosRotationIncrements -= 1;

            this.setPosition(d5, d0, d1);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            this.prevRotationYaw = this.rotationYaw;
        }
        else if (!this.isServerWorld())
        {
            this.motionX *= 0.98D;
            this.motionY *= 0.98D;
            this.motionZ *= 0.98D;
        }

        if (Math.abs(this.motionX) < 0.005D) this.motionX = 0.0D;
        if (Math.abs(this.motionY) < 0.005D) this.motionY = 0.0D;
        if (Math.abs(this.motionZ) < 0.005D) this.motionZ = 0.0D;

        if (!this.isServerWorld())
        {
            this.rotationYawHead = this.rotationYaw;
        }

        /* Taken from the EntityOtherPlayerMP, I think */
        this.prevLimbSwingAmount = this.limbSwingAmount;

        double d0 = this.posX - this.prevPosX;
        double d1 = this.posZ - this.prevPosZ;
        float f = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

        if (f > 1.0F) f = 1.0F;

        this.limbSwingAmount += (f - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    /**
     * Destroy near by items
     *
     * Taken from super implementation of onLivingUpdate. You can't use
     * super.onLivingUpdate() in onLivingUpdate(), because it will distort
     * actor's movement (make it more laggy)
     */
    private void pickUpNearByItems()
    {
        if (!this.worldObj.isRemote && !this.dead)
        {
            for (EntityItem entityitem : this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.getEntityBoundingBox().expand(1.0D, 0.0D, 1.0D)))
            {
                if (!entityitem.isDead && entityitem.getEntityItem() != null && !entityitem.cannotPickup())
                {
                    entityitem.setDead();
                }
            }
        }
    }

    /* Processing interaction with player */

    /**
     * Process interact
     *
     * Inject UUID of actor to registering device, open GUI for changing actor's
     * skin, or start recording him
     */
    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand p_184645_2_, ItemStack stack)
    {
        ItemStack item = player.getHeldItemMainhand();

        if (item != null && (this.handleRegisterItem(item, player) || this.handleSkinItem(item, player)))
        {
            return true;
        }
        else if (item == null)
        {
            if (!this.worldObj.isRemote) this.startRecording(player);

            return true;
        }

        return false;
    }

    /**
     * Set actor's id on register item (while using register item on this actor)
     */
    private boolean handleRegisterItem(ItemStack stack, EntityPlayer player)
    {
        boolean holdsRegisterItem = stack.getItem() instanceof ItemRegister;

        if (!this.worldObj.isRemote && holdsRegisterItem)
        {
            ItemRegister item = (ItemRegister) stack.getItem();
            BlockPos pos = item.getBlockPos(stack);

            if (pos == null)
            {
                player.addChatMessage(new TextComponentTranslation("blockbuster.actor.not_attached"));
            }

            TileEntity tile = this.worldObj.getTileEntity(pos);

            if (tile != null && tile instanceof TileEntityDirector)
            {
                TileEntityDirector director = (TileEntityDirector) tile;

                if (!director.add(this))
                {
                    player.addChatMessage(new TextComponentTranslation("blockbuster.director.already_registered"));
                }
                else
                {
                    player.addChatMessage(new TextComponentTranslation("blockbuster.director.was_registered"));
                }
            }
            else
            {
                player.addChatMessage(new TextComponentTranslation("blockbuster.director.missing", pos.getX(), pos.getY(), pos.getZ()));
            }
        }

        return holdsRegisterItem;
    }

    /**
     * Open skin choosing GUI by using skin managing item
     */
    private boolean handleSkinItem(ItemStack stack, EntityPlayer player)
    {
        boolean holdsSkinItem = stack.getItem() instanceof ItemActorConfig;

        if (this.worldObj.isRemote && holdsSkinItem)
        {
            GuiHandler.open(player, GuiHandler.ACTOR, this.getEntityId(), 0, 0);
        }

        return holdsSkinItem;
    }

    /* Public API */

    /**
     * Start the playback, invoked by director block (more specifically by
     * DirectorTileEntity).
     */
    public void startPlaying(String filename)
    {
        if (Mocap.playbacks.containsKey(this))
        {
            Mocap.broadcastMessage(new TextComponentTranslation("blockbuster.actor.playing"));
            return;
        }

        if (filename.isEmpty())
        {
            Mocap.broadcastMessage(new TextComponentTranslation("blockbuster.actor.no_name"));
        }
        else
        {
            Mocap.startPlayback(filename, this, false);
        }
    }

    /**
     * Stop playing
     */
    public void stopPlaying()
    {
        if (!Mocap.playbacks.containsKey(this))
        {
            return;
        }

        Mocap.playbacks.get(this).playing = false;
    }

    /**
     * Start recording the player's actions for this actor
     */
    private void startRecording(EntityPlayer player)
    {
        if (this.directorBlock == null) return;

        TileEntity tile = player.worldObj.getTileEntity(this.directorBlock);

        if (tile != null && tile instanceof TileEntityDirector)
        {
            TileEntityDirector director = (TileEntityDirector) tile;

            if (!Mocap.records.containsKey(player))
            {
                director.startPlayback(this);
            }
            else
            {
                director.stopPlayback(this);
            }

            director.startRecording(this, player);
        }
    }

    /**
     * Configure this actor
     *
     * Takes four properties to modify: filename used as id for recording,
     * displayed name, rendering skin and invulnerability flag
     */
    public void modify(String model, String skin, boolean invisible, boolean notify)
    {
        this.model = model;
        this.skin = skin;
        this.invisible = invisible;

        this.updateModel();

        if (!this.worldObj.isRemote && notify) this.notifyPlayers();
    }

    private void updateModel()
    {
        ModelHandler models = Blockbuster.proxy.models;

        if (models.models.containsKey(this.model))
        {
            this.modelInstance = models.models.get(this.model);
        }
    }

    public void notifyPlayers()
    {
        Dispatcher.updateTrackers(this, new PacketModifyActor(this.getEntityId(), this.model, this.skin, this.invisible));
    }

    /* Reading/writing to disk */

    @Override
    public void readEntityFromNBT(NBTTagCompound tag)
    {
        super.readEntityFromNBT(tag);

        String skin = this.skin;
        String model = this.model;

        this.model = tag.getString("Model");
        this.skin = tag.getString("Skin");
        this.invisible = tag.getBoolean("Invisible");

        this.directorBlock = ItemPlayback.getBlockPos("Dir", tag);

        if ((!skin.equals(this.skin) || !model.equals(this.model)) && !this.worldObj.isRemote)
        {
            this.notifyPlayers();
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag)
    {
        super.writeEntityToNBT(tag);

        if (!this.skin.isEmpty())
        {
            tag.setString("Skin", this.skin);
        }

        if (!this.model.isEmpty())
        {
            tag.setString("Model", this.model);
        }

        if (this.directorBlock != null)
        {
            ItemPlayback.saveBlockPos("Dir", tag, this.directorBlock);
        }

        tag.setBoolean("Invisible", this.invisible);
    }

    /* IEntityAdditionalSpawnData implementation */

    @Override
    public void writeSpawnData(ByteBuf buffer)
    {
        ByteBufUtils.writeUTF8String(buffer, this.skin);
        ByteBufUtils.writeUTF8String(buffer, this.model);
        buffer.writeBoolean(this.invisible);

        /* What a shame, Mojang, why do I need to synchronize your shit?! */
        buffer.writeBoolean(this.isEntityInvulnerable(DamageSource.anvil));
    }

    @Override
    public void readSpawnData(ByteBuf buffer)
    {
        this.skin = ByteBufUtils.readUTF8String(buffer);
        this.model = ByteBufUtils.readUTF8String(buffer);
        this.invisible = buffer.readBoolean();

        this.setEntityInvulnerable(buffer.readBoolean());
    }
}