package com.temportalist.thaumicexp.common.tile

import java.util
import java.util.{Arrays, List, UUID}

import com.temportalist.origin.api.inventory.IInv
import com.temportalist.origin.api.tile.{IPacketCallback, ITileSaver}
import com.temportalist.origin.library.common.nethandler.{IPacket, PacketHandler}
import com.temportalist.origin.library.common.network.PacketTileCallback
import com.temportalist.origin.library.common.utility.Players
import com.temportalist.thaumicexp.common._
import com.temportalist.thaumicexp.common.init.TECItems
import com.temportalist.thaumicexp.common.network.PacketSyncTicks
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

		val modeStack: ItemStack = this.getModeStack()
		if (modeStack != null) {
			if (this.currentOP == null) this.currentOP =
					if (modeStack.getItemDamage == 0) new OperationAnalyzer(2 * 20, 50)
					else if (modeStack.getItemDamage == 1) new OperationDecomposer(this)
					else null
			if (this.currentOP != null) {
				if (this.getInput() != null) this.currentOP.onUpdate(this, this)
				else this.currentOP.reset
			}
		}
	}

	override def onStackChange(slot: Int): Unit = {
		if (slot == this.MODE || slot == this.INPUT) this.currentOP = null
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
			case p: EntityPlayer => TECItems.modeItem.setUUID(stack, p.getGameProfile.getId)
			case _ =>
		}
		this.setInventorySlotContents(this.MODE, stack)
	}

	def getInput(): ItemStack = this.getStackInSlot(this.INPUT)

	def getOutput(): ItemStack = this.getStackInSlot(this.OUTPUT)

	def getModeStack(): ItemStack = this.getStackInSlot(this.MODE)

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
				if (this.getModeStack() != null)
					this.getModeStack().getItemDamage
				else -1
			if (stackMeta == 1) return true
			else {
				val playerID: UUID = TECItems.modeItem.getUUID(this.getModeStack())
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

		this.currentOP = null
		this.markDirty
		this.syncTile(false)
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
		this.markDirty
		// todo sync only aspects
		this.syncTile(false)
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
		val sorted: Array[Aspect] = this.aspects.getAspectsSorted
		if (index < sorted.length) {
			val aspect: Aspect = sorted(index)
			val takenAmount = if (takeOne) 1 else this.aspects.getAmount(aspect)
			this.aspects.remove(aspect, takenAmount)
			TEC.addAspects(player, new AspectList().add(aspect, takenAmount))
			this.syncTile(false)
		}
		else FMLLog.info("[TEC] Error, out of bounds aspect index of " + index)
	}

	// Data saving and reading

	def syncTile(justTicks: Boolean) {
		var packet: IPacket = null
		if (justTicks)
			packet = new PacketSyncTicks(this,
				if (this.currentOP != null) this.currentOP.getTicks else 0)
		else {
			val data: NBTTagCompound = new NBTTagCompound
			this.writeToNBT(data)
			packet = new PacketTileCallback(this).add("SYNC").add(data)
		}
		PacketHandler.sendToClients(TEC.MODID, packet)
	}

	override def packetCallback(packet: PacketTileCallback, isServer: Boolean): Unit = {
		packet.get[String] match {
			case "SYNC" => this.readFromNBT(packet.get[NBTTagCompound])
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

	override def markChunkModified(): Unit = {
		this.worldObj.markTileEntityChunkModified(this.xCoord, this.yCoord, this.zCoord, this)
	}

}
