package com.temportalist.thaumicexpansion.client

import com.temportalist.thaumicexpansion.common.ProxyCommon
import com.temportalist.thaumicexpansion.common.container.ContainerAnalyzer
import com.temportalist.thaumicexpansion.common.init.TECBlocks
import com.temportalist.thaumicexpansion.common.tile.{TEAnalyzer, TEApparatus}
import cpw.mods.fml.client.registry.ClientRegistry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.client.MinecraftForgeClient

/**
 *
 *
 * @author TheTemportalist
 */
class ProxyClient extends ProxyCommon {

	override def register(): Unit = {
		val ran: RenderAnalyzer = new RenderAnalyzer()
		ClientRegistry.bindTileEntitySpecialRenderer(classOf[TEAnalyzer], ran)
		MinecraftForgeClient.registerItemRenderer(
			Item.getItemFromBlock(TECBlocks.analyzer), ran)
		ClientRegistry.bindTileEntitySpecialRenderer(classOf[TEApparatus], RenderApparatus)
		MinecraftForgeClient.registerItemRenderer(
			Item.getItemFromBlock(TECBlocks.apparatus), RenderApparatus)
	}

	override def getClientElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int,
			z: Int, tileEntity: TileEntity): AnyRef = {
		if (tileEntity != null) tileEntity match {
			case te: TEAnalyzer =>
				return new GuiAnalyzer(new ContainerAnalyzer(te, player))
			case _ =>
		}
		null
	}

}
