package com.temportalist.scanner.common;

import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * @author TheTemportalist
 */
public class PacketRecieveAspect extends PacketCoFHBase {

	public static void init() {
		PacketHandler.instance.registerPacket(PacketRecieveAspect.class);
	}

	public PacketRecieveAspect() {
	}

	public PacketRecieveAspect(TEDecomposer tile, int aspectIndex, boolean takeAllVsOne) {
		this.addCoords(tile);
		this.addInt(aspectIndex);
		this.addBool(takeAllVsOne);
	}

	@Override
	public void handlePacket(EntityPlayer player, boolean isServer) {
		int[] tileCoords = this.getCoords();
		int aspectIndex = this.getInt();
		boolean takeAllVsOne = this.getBool();
		TEDecomposer tile = (TEDecomposer) player.getEntityWorld().getTileEntity(
				tileCoords[0], tileCoords[1], tileCoords[2]
		);
		if (player instanceof EntityPlayerMP)
			tile.giveResearch((EntityPlayerMP) player, aspectIndex, takeAllVsOne);
	}

}
