package com.temportalist.thaumicexpansion.common.tile

import java.util
import java.util.UUID

import com.temportalist.origin.api.common.tile.ITileSaver
import com.temportalist.origin.api.common.utility.Stacks
import com.temportalist.origin.foundation.common.network.PacketTileCallback
import com.temportalist.origin.foundation.common.tile.IPacketCallback
import com.temportalist.origin.foundation.common.utility.Players
import com.temportalist.origin.internal.common.Origin
import com.temportalist.thaumicexpansion.api.common.tile.{IEnergable, IOperation}
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.init.TECItems
import com.temportalist.thaumicexpansion.common.item.ItemMode
import com.temportalist.thaumicexpansion.common.lib.ThaumcraftHelper
import cpw.mods.fml.common.FMLLog
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import thaumcraft.api.aspects.{Aspect, AspectList, IAspectContainer}
import thaumcraft.api.research.ScanResult
import thaumcraft.api.{ThaumcraftApi, ThaumcraftApiHelper}
import thaumcraft.common.Thaumcraft
import thaumcraft.common.lib.research.{PlayerKnowledge, ScanManager}

import scala.util.control.Breaks._

/**
 * Created by TheTemportalist on 11/7/2015.
 */
class TEAnalyzer extends TileEntity with TileOperator with ITileSaver with IEnergable
with IAspectContainer with IPacketCallback {

	// ~~~~~~~~~~~~~~~~~~~~~~~~ TileOperator ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private var currentOperation: IOperation = null

	override def getInputSlot: Int = this.SLOT_INPUT

	override def getOutputSlot: Int = this.SLOT_OUTPUT

	def getCurrentModeString: String =
		this.currentOperation match {
			case a: OperationAnalyzer => "analyzer"
			case d: OperationDecomposer => "decomposer"
			case _ => "null"
		}

	override def canOperationRun(operation: IOperation): Boolean = {
		operation match {
			case vis_operation: IVisOperation =>
				val op = this.getInput != null && Stacks.canFit(this.getInput, this.getOutput) &&
						this.getEnergy >= vis_operation.getEnergyCost
				vis_operation match {
					case analyzer: OperationAnalyzer =>
						op && this.canScan(this.getInput)
					case _ => op
				}
			case _ => true
		}
	}

	override def updateOperator(): Unit = {
		this.checkEnergy()

		val modeStack = this.getModeStack
		val input = this.getInput

		if (input == null && this.currentOperation != null) {
			this.stopOperation(this.currentOperation)
			this.currentOperation = null
		}
		if (input != null && this.currentOperation == null) {
			modeStack.getItem match {
				case item: ItemMode =>
					this.currentOperation = item.getOperationForStack(modeStack)
					if (this.canOperationRun(this.currentOperation)) {
						this.currentOperation.setMaxTicks(this.getTicksForItem(input))
						this.startOperation(this.currentOperation)
					}
				case _ =>
			}
		}

		//println(this.currentOperation.getTotalTicks)

	}

	override def onOperationFinished(operation: IOperation): Unit = {
		this.currentOperation = null

		// mode stack SHOULD be non-null, because the operation is running
		val pUUID: UUID = TECItems.modeItem.getUUID(this.getModeStack)
		val pName: String = Players.getUserName(pUUID)

		operation match {
			case analyzer: OperationAnalyzer =>
				if (pName != null) {
					val input: ItemStack = this.getInput
					val scanResult: ScanResult = this.getScan(input)
					if (this.isValidScanTarget(pName, scanResult, "@")) {
						// add the aspects of the item
						TEC.addScan(pUUID, scanResult)
						this.finishedOperation(null, input, Array(analyzer.getEnergyCost))
					}
				}
			case decomposer: OperationDecomposer =>
				if (pUUID != null) TEC.addAspects(pUUID, decomposer.aspects)
				else this.addAspects(decomposer.aspects)

				var input: ItemStack = this.getInput
				var output: ItemStack = null
				if (this.getWorldObj.rand.nextDouble < .1d) {
					// you are able to keep the item
					output = input.copy
					output.stackSize = 1
				}
				input.stackSize -= 1
				if (input.stackSize <= 0) input = null
				this.finishedOperation(input, output, Array(decomposer.getEnergyCost))
			case _ =>
		}

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ IEnergable ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	override def slotFuel(): Int = this.SLOT_FUEL

	override def getMaxEnergy: Int = 1600 * 5

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Self.Scanning ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def getModeStack: ItemStack = this.getStackInSlot(this.SLOT_MODE)

	def getScan(stack: ItemStack): ScanResult =
		new ScanResult(1.toByte, Item.getIdFromItem(stack.getItem), stack.getItemDamage, null, "")

	def canScan(stack: ItemStack): Boolean = {
		val list: AspectList = ThaumcraftApiHelper.getObjectAspects(stack)
		if (list != null && list.size > 0) {
			val stackMeta: Int =
				if (this.getModeStack != null)
					this.getModeStack.getItemDamage
				else -1
			if (stackMeta == 1) return true
			else {
				val playerID: UUID = TECItems.modeItem.getUUID(this.getModeStack)
				if (playerID != null && !TEC.hasScannedOffline(playerID, stack)) {
					val pName: String = Players.getUserName(playerID)
					if (pName != null) {
						val pk: PlayerKnowledge = Thaumcraft.proxy.getPlayerKnowledge
						for (aspect <- list.getAspects) {
							if (aspect != null && !aspect.isPrimal &&
									!pk.hasDiscoveredParentAspects(pName, aspect)) return false
						}
						return this.isValidScanTarget(pName, this.getScan(stack), "@")
					}
				}
			}
		}
		false
	}

	def isValidScanTarget(pName: String, scan: ScanResult, prefix: String): Boolean = {
		if (scan == null) return false
		if (prefix.equals("@") && (!isValidScanTarget(pName, scan, "#"))) return false

		scan.`type` match {
			case 1 =>
				val arg = util.Arrays.asList(Item.getItemById(scan.id), scan.meta)
				if (ThaumcraftApi.groupedObjectTags.containsKey(arg)) {
					scan.meta = ThaumcraftApi.groupedObjectTags.get(arg)(0)
				}
				val list = Thaumcraft.proxy.getScannedObjects.get(pName)
						.asInstanceOf[util.List[_]]
				if (list != null && list.contains(
					prefix + ScanManager.generateItemHash(Item.getItemById(scan.id), scan.meta)
				)) {
					return false
				}
			case 2 =>
				scan.entity match {
					case item: EntityItem =>
						val t: ItemStack = item.getEntityItem.copy
						t.stackSize = 1
						val arg = util.Arrays.asList(t.getItem, t.getItemDamage)
						if (ThaumcraftApi.groupedObjectTags.containsKey(arg)) {
							t.setItemDamage(ThaumcraftApi.groupedObjectTags.get(arg)(0))
						}
						val list = Thaumcraft.proxy.getScannedObjects.get(
							pName).asInstanceOf[util.List[_]]
						if (list != null && list.contains(
							prefix + ScanManager.generateItemHash(t.getItem, t.getItemDamage)
						)) return false
					case _ =>
						val list = Thaumcraft.proxy.getScannedEntities.get(
							pName).asInstanceOf[util.List[_]]
						if (list != null) {
							try {
								val hash: String = classOf[ScanManager]
										.getDeclaredMethod("generateEntityHash", classOf[Entity])
										.invoke(null, scan.entity).asInstanceOf[String]
								if (list.contains(prefix + hash)) return false
							}
							catch {
								case e: Exception => e.printStackTrace()
							}
						}
				}
			case 3 =>
				val list = Thaumcraft.proxy.getScannedPhenomena.get(
					pName).asInstanceOf[util.List[_]]
				if (list != null && list.contains(prefix + scan.phenomena)) {
					return false
				}
			case _ =>
		}

		true
	}

	def finishedOperation(setInput: ItemStack, toOutput: ItemStack,
			data: Array[Any]): Unit = {
		this.setInventorySlotContents(this.SLOT_INPUT, setInput)
		if (this.getOutput == null) this.setInventorySlotContents(this.SLOT_OUTPUT, toOutput)
		else if (toOutput != null) {
			val output = this.getOutput.copy()
			output.stackSize += toOutput.stackSize
			this.setInventorySlotContents(this.SLOT_OUTPUT, output)
		}

		this.reduceEnergy(data(0).asInstanceOf[Int], doOp = true)

		//this.markDirty()

		this.syncEnergyAspectsInv()
	}

	def getTicksForItem(input: ItemStack): Int = {
		if (input != null) {
			val list = ThaumcraftApiHelper.getObjectAspects(input)
			var time = 0
			list.getAspects.foreach(aspect => {
				TEC.getAspectTier(aspect) match {
					case 1 => time += list.getAmount(aspect) * 2
					case 2 => time += list.getAmount(aspect) * 5
					case 3 => time += list.getAmount(aspect) * 10
					case _ => throw new IllegalArgumentException("ERROR: Aspect " + aspect.getName
							+ " does not have a tier accounted for.")
				}
			})
			time
		}
		else 40
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Self.Aspects ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def giveAspect(player: EntityPlayer, index: Int, takeOne: Boolean): Unit = {
		//println("give aspect")
		val sorted: Array[Aspect] = this.aspects.getAspectsSorted
		if (index < sorted.length) {
			val aspect: Aspect = sorted(index)
			val takenAmount = if (takeOne) 1 else this.aspects.getAmount(aspect)
			this.aspects.remove(aspect, takenAmount)
			TEC.addAspects(player, new AspectList().add(aspect, takenAmount))
			this.syncAspects()
		}
		else FMLLog.info("[TEC] Error, out of bounds aspect index of " + index)
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Self.NBT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	override def writeToNBT(tagCom: NBTTagCompound): Unit = {
		super.writeToNBT(tagCom)
		this.writeNBT_Energy(tagCom, "Energable")
		this.aspects.writeToNBT(tagCom, "aspects")
		tagCom.setInteger("offset", this.currentColumnOffset)
	}

	override def readFromNBT(tagCom: NBTTagCompound): Unit = {
		super.readFromNBT(tagCom)
		this.readNBT_Energy(tagCom, "Energable")
		this.aspects.readFromNBT(tagCom, "aspects")
		this.currentColumnOffset = tagCom.getInteger("offset")
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Self.GUI ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	final val hexagonProgressSteps: Int = 13

	private var currentColumnOffset: Int = 0

	def getColumnOffset: Int = this.currentColumnOffset

	def setColumnOffset(i: Int): Unit = this.currentColumnOffset = i

	def isProcessing: Boolean = this.currentOperation != null && this.currentOperation.isRunning

	def getProgress: Int = (
			if (this.currentOperation != null)
				this.currentOperation.getProgress * this.hexagonProgressSteps
			else 0
			).asInstanceOf[Int]

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Inventory ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	this.setSlots(4)
	final val SLOT_INPUT = 0
	final val SLOT_OUTPUT = 2
	final val SLOT_MODE = 3
	final val SLOT_FUEL = 1

	override def getInventoryName: String = "Analyzer"

	override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = {
		if (stack == null || slot == this.SLOT_OUTPUT) true
		else if (slot == this.SLOT_INPUT) this.canScan(stack)
		else if (slot == this.slotFuel()) this.getFuelEnergy(stack) > 0
		else if (slot == this.SLOT_MODE) stack.getItem == TECItems.modeItem
		else false
	}

	override def getAccessibleSlotsFromSide(side: Int): Array[Int] =
		if (side == 0) Array[Int](this.slotFuel())
		else Array[Int](this.SLOT_INPUT, this.SLOT_OUTPUT)

	override def canExtractItem(slot: Int, stack: ItemStack, side: Int): Boolean =
		slot == this.SLOT_OUTPUT

	override def canInsertItem(slot: Int, stack: ItemStack, side: Int): Boolean =
		slot != this.SLOT_OUTPUT && slot != this.SLOT_MODE

	override def onStackChange(slot: Int): Unit = {
		if ((slot == this.SLOT_MODE || slot == this.SLOT_INPUT) && this.currentOperation != null) {
			this.stopOperation(this.currentOperation)
			this.currentOperation = null
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ IAspectContainer ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private var aspects: AspectList = new AspectList

	def addAspects(aspects: AspectList): Unit = {
		this.aspects.add(aspects)
		this.markDirty()
	}

	override def getAspects: AspectList = this.aspects

	override def doesContainerAccept(aspect: Aspect): Boolean = true

	override def doesContainerContain(aspects: AspectList): Boolean = {
		aspects.aspects.keySet().toArray.foreach(aspect => {
			if (this.containerContains(aspect.asInstanceOf[Aspect]) <= 0) return false
		})
		true
	}

	override def addToContainer(aspect: Aspect, amount: Int): Int = {
		this.aspects.add(aspect, amount)
		this.markDirty()
		amount
	}

	override def containerContains(aspect: Aspect): Int = this.aspects.getAmount(aspect)

	override def takeFromContainer(aspect: Aspect, amount: Int): Boolean = {
		val contained: Int = this.aspects.getAmount(aspect)
		if (contained < amount) return false
		else if (contained > amount) this.aspects.reduce(aspect, amount)
		else if (contained == amount) this.aspects.remove(aspect)
		//this.markDirty
		true
	}

	override def takeFromContainer(aspects: AspectList): Boolean = {
		val temp: AspectList = this.aspects.copy
		var successful: Boolean = true

		breakable {
			for (entry <- aspects.aspects.entrySet.toArray) {
				val pair: util.Map.Entry[Aspect, Int] = entry
						.asInstanceOf[util.Map.Entry[Aspect, Int]]
				if (!temp.reduce(pair.getKey, pair.getValue)) {
					successful = false
					break()
				}
			}
		}
		if (successful) this.setAspects(temp)
		successful
	}

	override def setAspects(aspects: AspectList): Unit = {
		this.aspects = aspects
		this.markDirty()
	}

	override def doesContainerContainAmount(aspect: Aspect, amount: Int): Boolean =
		this.containerContains(aspect) == amount

	// ~~~~~~~~~~~~~~~~~~~~~~~~ IPacketCallback ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def syncEnergyAspectsInv(): Unit = {
		if (!this.worldObj.isRemote) {
			val nbt: NBTTagCompound = new NBTTagCompound
			this.writeNBT_Energy(nbt, "energy")
			this.aspects.writeToNBT(nbt, "aspects")
			this.writeNBT_Inv(nbt, "stacks")
			new PacketTileCallback(this).add("EnergyAspects").add(nbt).
					sendToDimension(Origin, this.worldObj.provider.dimensionId)
		}
	}

	def syncAspects(): Unit = {
		if (!this.worldObj.isRemote) {
			val nbt: NBTTagCompound = new NBTTagCompound
			this.aspects.writeToNBT(nbt)
			new PacketTileCallback(this).add("Aspects").add(nbt).
					sendToDimension(Origin, this.worldObj.provider.dimensionId)
		}
	}

	override def packetCallback(packet: PacketTileCallback, side: Side): Unit = {
		packet.get[String] match {
			case "EnergyAspects" =>
				val nbt: NBTTagCompound = packet.get[NBTTagCompound]
				this.readNBT_Energy(nbt, "energy")
				this.aspects.readFromNBT(nbt, "aspects")
				this.readNBT_Inv(nbt, "stacks")
			case "Aspects" =>
				this.aspects.readFromNBT(packet.get[NBTTagCompound])
			case "ColumnChange" =>
				val dir: Int = if (packet.get[Boolean]) 1 else -1
				val newStartIndex: Int = dir * 4
				if (newStartIndex >= 0 && newStartIndex < this.aspects.size())
					this.setColumnOffset(this.getColumnOffset + dir)
			case _ =>
		}
	}

}
