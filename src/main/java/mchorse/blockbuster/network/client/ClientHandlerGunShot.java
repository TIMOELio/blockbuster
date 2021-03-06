package mchorse.blockbuster.network.client;

import mchorse.blockbuster.capabilities.gun.Gun;
import mchorse.blockbuster.capabilities.gun.IGun;
import mchorse.blockbuster.network.common.guns.PacketGunShot;
import mchorse.mclib.network.ClientMessageHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class ClientHandlerGunShot extends ClientMessageHandler<PacketGunShot>
{
    @Override
    public void run(EntityPlayerSP player, PacketGunShot message)
    {
        Entity entity = player.worldObj.getEntityByID(message.entity);

        if (entity instanceof EntityLivingBase)
        {
            EntityLivingBase base = (EntityLivingBase) entity;
            IGun gun = Gun.get(base.getHeldItemMainhand());

            if (gun != null)
            {
                gun.getProps().shot();
            }
        }
    }
}