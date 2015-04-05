package com.temportalist.thaumicexpansion.common.network

import com.temportalist.origin.library.common.network.PacketTile
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity

/**
 *
 *
 * @author TheTemportalist 4/3/15
 */
class PacketGiveAspect(tile: TEAnalyzer, index: Int, takeOne: Boolean) extends PacketTile(tile) {

	this.add(index)
	this.add(takeOne)

	def this() {
		this(null, 0, false)
	}

	override def getChannel(): String = TEC.MODID

	override def handle(player: EntityPlayer, tileEntity: TileEntity, isServer: Boolean): Unit = {
		tileEntity match {
			case tile: TEAnalyzer =>
				tile.giveAspect(player, this.get[Int], this.get[Boolean])
			case _ =>
		}
	}

}
