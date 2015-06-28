package com.temportalist.thaumicexpansion.common.tile

import java.util
import java.util.{Arrays, List, UUID}

import com.temportalist.origin.api.common.inventory.IInv
import com.temportalist.origin.api.common.tile.ITileSaver
import com.temportalist.origin.foundation.common.network.PacketTileCallback
import com.temportalist.origin.foundation.common.tile.IPacketCallback
import com.temportalist.origin.foundation.common.utility.Players
import com.temportalist.thaumicexpansion.api.common.tile.{IOperator, IOperation, IEnergable}
import com.temportalist.thaumicexpansion.common._
import com.temportalist.thaumicexpansion.common.init.TECItems
import cpw.mods.fml.common.FMLLog
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import thaumcraft.api.aspects.{Aspect, AspectList, IAspectContainer}
import thaumcraft.api.research.ScanResult
import thaumcraft.api.{ThaumcraftApi, ThaumcraftApiHelper}
import thaumcraft.common.Thaumcraft
import thaumcraft.common.lib.research.{PlayerKnowledge, ScanManager}

import scala.util.control.Breaks.{break, breakable}
import cpw.mods.fml.relauncher.Side

/**
 *
 *
 * @author TheTemportalist
 */
class TEAnalyzer()
		extends TileEntity with IInv with IEnergable with IOperator with ISidedInventory with
		IAspectContainer with ITileSaver with IPacketCallback {

	override def getInventoryName: String = "Thaumic Analyzer"

	private var tier: Int = 0
	/**
	 * The direction the block is facing.
	 * Default is EAST item rendering
	 * Set when block is placed
	 */
	private var facing: Int = ForgeDirection.EAST.ordinal()

	var currentMode: String = ""

	private var currentOP: IOperation = null
	final val hexagonProgressSteps: Int = 13

	private var currentColumnOffset: Int = 0

	final val INPUT = 0
	final val OUTPUT = 2
	final val MODE = 3
	this.setSlots(4)

	private var aspects: AspectList = new AspectList

	override def updateEntity(): Unit = {
		this.checkEnergy()

		val modeStack: ItemStack = this.getModeStack
		if (modeStack != null) {
			modeStack.getItemDamage match {
				case 0 =>
					this.currentMode = "analyzer"
					if (this.currentOP == null)
						this.currentOP = new OperationAnalyzer(2 * 20, 50)
				case 1 =>
					this.currentMode = "decomposer"
					if (this.currentOP == null)
						this.currentOP = new OperationDecomposer(this)
			}
			if (this.currentOP != null) {
				if (this.getInput != null) this.currentOP.onUpdate(this, this)
				else this.currentOP.reset()
			}
		}
		else this.currentMode = ""

		/*
		println(
			(if (this.worldObj.isRemote) "Client: " else "Server: ") +
					(if (this.currentOP == null) 0 else this.currentOP.getTicks)
		)
		*/

	}

	override def onStackChange(slot: Int): Unit = {
		if (slot == this.MODE || slot == this.INPUT) this.currentOP = null
		//println(slot + "|" + this.getStackInSlot(slot))
		//this.syncTile(false)
	}

	// Data manipulation

	override def slotFuel(): Int = 1

	override def getMaxEnergy(): Int = 1600 * 5

	def getColumnOffset(): Int = this.currentColumnOffset

	def setColumn(i: Int): Unit = this.currentColumnOffset = i

	def getTier(): Int = this.tier

	def setTier(t: Int): Unit = this.tier = t

	def getFacing(): ForgeDirection = ForgeDirection.getOrientation(this.facing)

	def setFacing(i: Int): Unit = this.facing = i

	def setMode(stack: ItemStack, placer: EntityLivingBase): Unit = {
		placer match {
			case p: EntityPlayer =>
				var id = p.getGameProfile.getId
				if (id == null && p.getGameProfile.getName != null)
					id = UUID.fromString(p.getGameProfile.getName)
				else throw new IllegalArgumentException(
					"Name and ID cannot both be blank in GameProfile")
				TECItems.modeItem.setUUID(stack, id)
			case _ =>
		}
		this.setInventorySlotContents(this.MODE, stack)
	}

	def getInput: ItemStack = this.getStackInSlot(this.INPUT)

	def getOutput: ItemStack = this.getStackInSlot(this.OUTPUT)

	def getModeStack: ItemStack = this.getStackInSlot(this.MODE)

	def isProcessing: Boolean = this.currentOP != null && this.currentOP.isRunning

	def getProgress: Int = (
			if (this.currentOP != null)
				this.currentOP.getProgress * this.hexagonProgressSteps
			else 0
			).asInstanceOf[Int]

	def setOpTicks(ticks: Int): Unit = if (this.currentOP != null) this.currentOP.setTicks(ticks)

	// Scanning

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
				val arg: List[_] = Arrays.asList(Item.getItemById(scan.id), scan.meta)
				if (ThaumcraftApi.groupedObjectTags.containsKey(arg)) {
					scan.meta = ThaumcraftApi.groupedObjectTags.get(arg)(0)
				}
				val list: List[_] = Thaumcraft.proxy.getScannedObjects.get(pName)
						.asInstanceOf[List[_]]
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
						val arg: List[_] = Arrays.asList(t.getItem, t.getItemDamage)
						if (ThaumcraftApi.groupedObjectTags.containsKey(arg)) {
							t.setItemDamage(ThaumcraftApi.groupedObjectTags.get(arg)(0))
						}
						val list: List[_] = Thaumcraft.proxy.getScannedObjects.get(
							pName).asInstanceOf[List[_]]
						if (list != null && list.contains(
							prefix + ScanManager.generateItemHash(t.getItem, t.getItemDamage)
						)) return false
					case _ =>
						val list: List[_] = Thaumcraft.proxy.getScannedEntities.get(
							pName).asInstanceOf[List[_]]
						if (list != null) {
							try {
								val hash: String = classOf[ScanManager]
										.getDeclaredMethod("generateEntityHash", classOf[Entity])
										.invoke(null, scan.entity).asInstanceOf[String]
								if (list.contains(prefix + hash)) return false
							}
							catch {
								case e: Exception => e.printStackTrace
							}
						}
				}
			case 3 =>
				val list: List[_] = Thaumcraft.proxy.getScannedPhenomena.get(
					pName).asInstanceOf[List[_]]
				if (list != null && list.contains(prefix + scan.phenomena)) {
					return false
				}
			case _ =>
		}

		true
	}

	// Operations

	override def finishedOperation(setInput: ItemStack, toOutput: ItemStack,
			data: Array[Any]): Unit = {
		this.slots(this.INPUT) = setInput
		if (this.slots(this.OUTPUT) == null) this.slots(this.OUTPUT) = toOutput
		else if (toOutput != null) this.slots(this.OUTPUT).stackSize += toOutput.stackSize

		this.reduceEnergy(data(0).asInstanceOf[Int], true)

		this.currentOP = null // todo will not syncing this reset the op on sync or save?
		this.markDirty()

		this.syncEnergyAspectsInv()
	}

	// Inventory

	override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = {
		if (stack == null || slot == this.OUTPUT) true
		else if (slot == this.INPUT) this.canScan(stack)
		else if (slot == this.slotFuel()) this.getFuelEnergy(stack) > 0
		else if (slot == this.MODE) stack.getItem == TECItems.modeItem
		else false
	}

	override def getAccessibleSlotsFromSide(side: Int): Array[Int] =
		if (side == 0) Array[Int](this.slotFuel())
		else Array[Int](this.INPUT, this.OUTPUT)

	override def canExtractItem(slot: Int, stack: ItemStack, side: Int): Boolean =
		slot == this.OUTPUT

	override def canInsertItem(slot: Int, stack: ItemStack, side: Int): Boolean =
		slot != this.OUTPUT && slot != this.MODE

	// Aspects

	def addAspects(aspects: AspectList): Unit = {
		this.aspects.add(aspects)
		this.markDirty()
	}

	override def getAspects: AspectList = this.aspects

	override def doesContainerAccept(aspect: Aspect): Boolean = true

	override def doesContainerContain(aspects: AspectList): Boolean = {
		val aspectSet: Array[AnyRef] = aspects.aspects.keySet().toArray
		for (i <- 0 until aspectSet.length) {
			if (this.containerContains(aspectSet(i).asInstanceOf[Aspect]) <= 0) return false
		}
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
					break
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

	def syncEnergyAspectsInv(): Unit = {
		if (!this.worldObj.isRemote) {
			val nbt: NBTTagCompound = new NBTTagCompound
			this.writeNBT_Energy(nbt, "energy")
			this.aspects.writeToNBT(nbt, "aspects")
			this.writeNBT_Inv(nbt, "stacks")
			new PacketTileCallback(this).add("EnergyAspects").add(nbt).sendToClients()
		}
	}

	def syncAspects(): Unit = {
		if (!this.worldObj.isRemote) {
			val nbt: NBTTagCompound = new NBTTagCompound
			this.aspects.writeToNBT(nbt)
			new PacketTileCallback(this).add("Aspects").add(nbt).sendToClients()
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
					this.setColumn(this.getColumnOffset() + dir)
			case "Ticks" =>
				//if (this.currentOP != null) this.currentOP.setTicks(packet.get[Int])
			case _ =>
		}
	}

	override def writeToNBT(tagCom: NBTTagCompound): Unit = {
		super.writeToNBT(tagCom)
		this.writeNBT_Inv(tagCom, "Inventory")
		this.writeNBT_Energy(tagCom, "Energable")
		tagCom.setInteger("tier", this.tier)
		tagCom.setInteger("facing", this.facing)
		this.aspects.writeToNBT(tagCom, "aspects")
		tagCom.setInteger("offset", this.currentColumnOffset)

	}

	override def readFromNBT(tagCom: NBTTagCompound): Unit = {
		super.readFromNBT(tagCom)
		this.readNBT_Inv(tagCom, "Inventory")
		this.readNBT_Energy(tagCom, "Energable")
		this.tier = tagCom.getInteger("tier")
		this.facing = tagCom.getInteger("facing")
		this.aspects.readFromNBT(tagCom, "aspects")
		this.currentColumnOffset = tagCom.getInteger("offset")

	}

}
