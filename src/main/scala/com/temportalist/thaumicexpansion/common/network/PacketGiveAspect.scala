package com.temportalist.thaumicexpansion.common.network

import com.temportalist.origin.foundation.common.network.IPacket
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import cpw.mods.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import cpw.mods.fml.relauncher.Side

/**
 *
 *
 * @author TheTemportalist 4/3/15
 */
class PacketGiveAspect extends IPacket {

	def this(tile: TEAnalyzer, index: Int, takeOne: Boolean) {
		this()
		this.add(tile)
		this.add(index)
		this.add(takeOne)
	}

	override def getReceivableSide: Side = Side.SERVER
}
object PacketGiveAspect {
	class Handler extends IMessageHandler[PacketGiveAspect, IMessage] {
		override def onMessage(message: PacketGiveAspect, ctx: MessageContext): IMessage = {
			val player = ctx.getServerHandler.playerEntity
			message.getTile(player.worldObj) match {
				case tile: TEAnalyzer =>
					tile.giveAspect(player, message.get[Int], message.get[Boolean])
				case _ =>
			}
			null
		}
	}
}
