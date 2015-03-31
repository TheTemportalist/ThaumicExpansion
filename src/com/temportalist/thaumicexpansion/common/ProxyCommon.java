package com.temportalist.thaumicexpansion.common;

import com.temportalist.thaumicexpansion.common.inventory.ContainerThaumicAnalyzer;
import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
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
			if (ID == 0 && tile instanceof TEThaumicAnalyzer) {
				return new ContainerThaumicAnalyzer((TEThaumicAnalyzer) tile, player.inventory);
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
