package com.temportalist.thaumicexpansion.common.network

import com.temportalist.origin.foundation.common.network.PacketTile
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import cpw.mods.fml.relauncher.Side
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

	override def handle(player: EntityPlayer, tileEntity: TileEntity, side: Side): Unit = {
		tileEntity match {
			case tile: TEAnalyzer =>
				tile.giveAspect(player, this.get[Int], this.get[Boolean])
			case _ =>
		}
	}

}
