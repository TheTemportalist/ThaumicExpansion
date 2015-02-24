package com.temportalist.scanner.client;

import com.temportalist.scanner.client.gui.GuiDecomposer;
import com.temportalist.scanner.common.ProxyCommon;
import com.temportalist.scanner.common.TEDecomposer;
import com.temportalist.scanner.common.inventory.ContainerDecomposer;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author TheTemportalist
 */
public class ProxyClient extends ProxyCommon {

	@Override
	public void register() {
		RenderingRegistry.registerBlockHandler(new ThermalHandler());
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x,
			int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile != null) {
			if (ID == 0 && tile instanceof TEDecomposer) {
				return new GuiDecomposer(
						(ContainerDecomposer) this.getServerGuiElement(ID, player, world, x, y, z)
				);
			}
		}
		return super.getClientGuiElement(ID, player, world, x, y, z);
	}

}
