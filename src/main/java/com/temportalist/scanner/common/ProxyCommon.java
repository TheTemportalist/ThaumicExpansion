package com.temportalist.scanner.common;

import com.temportalist.scanner.common.inventory.ContainerDecomposer;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author TheTemportalist
 */
public class ProxyCommon implements IGuiHandler {

	public void register() {
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x,
			int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile != null) {
			if (ID == 0 && tile instanceof TEDecomposer) {
				return new ContainerDecomposer((TEDecomposer) tile, player.inventory);
			}
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x,
			int y, int z) {
		return null;
	}

}
