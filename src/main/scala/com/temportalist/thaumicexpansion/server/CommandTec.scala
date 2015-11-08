package com.temportalist.thaumicexpansion.server

import com.temportalist.origin.foundation.common.utility.Players
import com.temportalist.thaumicexpansion.common.TEC
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.world.biome.BiomeGenBase
import thaumcraft.api.aspects.{Aspect, AspectList}
import thaumcraft.api.nodes.{NodeModifier, NodeType}
import thaumcraft.common.Thaumcraft
import thaumcraft.common.lib.research.PlayerKnowledge
import thaumcraft.common.lib.world.ThaumcraftWorldGenerator
import thaumcraft.common.lib.world.biomes.BiomeHandler

import scala.util.control.Breaks._

/**
 * Created by TheTemportalist on 11/7/2015.
 */
object CommandTEC extends CommandBase {

	override def getCommandName: String = "tec"

	override def getCommandUsage(sender: ICommandSender): String = "command.tec.clearKnow"

	override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
		if (args.length > 0) {
			val subCommand: String = args(0)
			subCommand match {
				case "clearKnow" =>
					var player: EntityPlayer = null
					if (args.length > 1) {
						val players: List[EntityPlayer] = MinecraftServer.getServer
								.getConfigurationManager.playerEntityList
								.asInstanceOf[List[EntityPlayer]]

						breakable(for (inGamePlayer <- players) {
							if (inGamePlayer.getCommandSenderName == args(1)) {
								player = inGamePlayer
								break()
							}
						})
					}
					else if (sender.isInstanceOf[EntityPlayer]) {
						player = sender.asInstanceOf[EntityPlayer]
					}

					if (player != null) {
						val knowledge: PlayerKnowledge = Thaumcraft.proxy.getPlayerKnowledge
						val pName: String = player.getCommandSenderName
						knowledge.aspectsDiscovered.put(pName, new AspectList)
						knowledge.objectsScanned.put(pName, null)
						knowledge.entitiesScanned.put(pName, null)
						knowledge.phenomenaScanned.put(pName, null)
					}
				case "spawnNode" =>
					if (args.length <= 1) return

					var nodeType: NodeType = NodeType.NORMAL
					try {
						nodeType = NodeType.valueOf(args(1).toUpperCase)
					}
					catch {
						case e: Exception =>
							this.chat(sender,
								"Could not parse node type " + args(1) + ". Using NORMAL.")
					}

					var modifier: NodeModifier = null
					try {
						modifier = NodeModifier.valueOf(args(2).toUpperCase)
					}
					catch {
						case e: Exception =>
					}

					val world = sender.getEntityWorld
					val coords = sender.getPlayerCoordinates

					val biome: BiomeGenBase = world.getBiomeGenForCoords(coords.posX, coords.posZ)
					var biomeAura: Int = BiomeHandler.getBiomeAura(biome)
					if (nodeType != NodeType.PURE &&
							biome.biomeID == ThaumcraftWorldGenerator.biomeTaint.biomeID) {
						biomeAura = (biomeAura * 1.5D).toInt
						if (nodeType == NodeType.TAINTED) biomeAura = (biomeAura * 1.5D).toInt
					}

					val aspects = new AspectList()
					for (i <- 3 until args.length) {
						val aspectQuantity = args(i).split('=')
						val aspectName = aspectQuantity(0)
						if (Aspect.aspects.containsKey(aspectName)) {
							var quantity = 0
							try {
								quantity = aspectQuantity(1).toInt
							}
							catch {
								case e: Exception =>
									this.chat(sender, "Could not parse quantity "
											+ aspectQuantity(1) + " for aspect " + aspectName)
									return
							}
							aspects.add(Aspect.aspects.get(aspectName), quantity)
						}
						else {
							this.chat(sender, "Could not parse aspect name " + aspectName)
							return
						}
					}

					var aspectListString = "{"
					aspects.getAspects.foreach(aspect =>
						aspectListString += aspect.getName + ":" + aspects.getAmount(aspect) + "|")
					aspectListString += "}"

					val y = coords.posY + (if (sender.isInstanceOf[EntityPlayer]) 0 else 1)

					this.chat(sender, "Creating node at {" +
							coords.posX + "," + y + "," + coords.posZ +
							"} with type:" + nodeType + " modifier:" + modifier +
							" and aspects " + aspectListString)
					ThaumcraftWorldGenerator.createNodeAt(world, coords.posX, y, coords.posZ,
						nodeType, modifier, aspects)

				case _ =>
			}
			if (subCommand == "clearKnow") {

			}
		}
	}

	def chat(sender: ICommandSender, output: String): Unit = {
		sender match {
			case player: EntityPlayer => this.chat(player, output)
			case _ => TEC.log(output)
		}
	}

	def chat(p: EntityPlayer, output: String): Unit = {
		Players.message(p, output)
	}

}
