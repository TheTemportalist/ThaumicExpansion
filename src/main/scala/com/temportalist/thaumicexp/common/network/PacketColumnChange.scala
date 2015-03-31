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
class PacketColumnChange(tile: TileEntity, column: Int) extends PacketTile(tile) {

	this.add(column)

	def this() {
		this(null, 0)
	}

	override def handle(player: EntityPlayer, tileEntity: TileEntity, isServer: Boolean): Unit = {
		tileEntity match {
			case te: TEAnalyzer => if (te == null) return
				te.setColumn(te.getColumnOffset() + this.get[Int])
			case _ =>
		}
	}

}
