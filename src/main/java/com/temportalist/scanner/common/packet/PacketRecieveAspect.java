package com.temportalist.scanner.common.packet;

import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import com.temportalist.scanner.common.tile.TEThaumicAnalyzer;
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

	public PacketRecieveAspect(TEThaumicAnalyzer tile, int aspectIndex, boolean takeAllVsOne) {
		this.addCoords(tile);
		this.addInt(aspectIndex);
		this.addBool(takeAllVsOne);
	}

	@Override
	public void handlePacket(EntityPlayer player, boolean isServer) {
		int[] tileCoords = this.getCoords();
		int aspectIndex = this.getInt();
		boolean takeAllVsOne = this.getBool();
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) player.getEntityWorld().getTileEntity(
				tileCoords[0], tileCoords[1], tileCoords[2]
		);
		if (player instanceof EntityPlayerMP)
			tile.giveResearch((EntityPlayerMP) player, aspectIndex, takeAllVsOne);
	}

}
