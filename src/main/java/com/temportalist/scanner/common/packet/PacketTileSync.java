package com.temportalist.scanner.common.packet;

import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.io.IOException;

/**
 * @author TheTemportalist
 */
public class PacketTileSync extends PacketCoFHBase {

	public static void init() {
		PacketHandler.instance.registerPacket(PacketTileSync.class);
	}

	public PacketTileSync() {
	}

	public PacketTileSync(TileEntity tile) {
		this.addCoords(tile);
		NBTTagCompound tagCom = new NBTTagCompound();
		tile.writeToNBT(tagCom);
		try {
			this.writeNBT(tagCom);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handlePacket(EntityPlayer player, boolean isServer) {
		int[] coords = this.getCoords();
		NBTTagCompound tag;
		try {
			tag = this.readNBT();
			player.getEntityWorld().getTileEntity(coords[0], coords[1], coords[2]).readFromNBT(tag);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
