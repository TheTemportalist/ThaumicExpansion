package com.temportalist.thaumicexpansion.common

import com.temportalist.origin.api.common.proxy.IProxy
import com.temportalist.thaumicexpansion.common.container.ContainerAnalyzer
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

/**
 *
 *
 * @author TheTemportalist
 */
class ProxyCommon extends IProxy {

	override def register(): Unit = {}

	override def getClientElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int,
			z: Int, tileEntity: TileEntity): AnyRef = null

	override def getServerElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int,
			z: Int, tileEntity: TileEntity): AnyRef = {
		if (tileEntity != null) tileEntity match {
			case te: TEAnalyzer =>
				return new ContainerAnalyzer(te, player)
			case _ =>
		}
		null
	}

}
