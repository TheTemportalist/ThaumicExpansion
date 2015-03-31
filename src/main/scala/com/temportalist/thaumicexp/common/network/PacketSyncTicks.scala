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
class PacketSyncTicks(tile: TileEntity, ticks: Int) extends PacketTile() {

	this.add(ticks)

	def this() {
		this(null, 0)
	}

	override def handle(player: EntityPlayer, tileEntity: TileEntity, isServer: Boolean): Unit =
		tileEntity.asInstanceOf[TEAnalyzer].setOpTicks(this.get[Int])

}
