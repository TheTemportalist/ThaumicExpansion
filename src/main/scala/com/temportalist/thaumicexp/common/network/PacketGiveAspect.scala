package com.temportalist.thaumicexp.common.network

import com.temportalist.origin.library.common.network.PacketTile
import com.temportalist.thaumicexp.common.tile.TEAnalyzer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity

/**
 *
 *
 * @author TheTemportalist
 */
class PacketGiveAspect(tile: TileEntity, aspectIndex: Int, takeOne: Boolean) extends PacketTile(tile) {

	this.add(aspectIndex)
	this.add(takeOne)

	def this() {
		this(null, 0, false)
	}

	override def handle(player: EntityPlayer, tileEntity: TileEntity, isServer: Boolean): Unit = {
		tileEntity match {
			case te: TEAnalyzer => if (te == null) return
				te.giveAspect(player, this.get[Int], this.get[Boolean])
			case _ =>
		}
	}

}
