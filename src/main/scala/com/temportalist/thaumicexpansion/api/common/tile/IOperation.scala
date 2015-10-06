package com.temportalist.thaumicexpansion.api.common.tile

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

/**
 *
 *
 * @author TheTemportalist
 */
trait IOperation {

	protected var maxTicks: Int = 0
	protected var currentTicks: Int = -1

	def ticksForOperation: Int = this.maxTicks

	def isRunning: Boolean = this.currentTicks >= 0

	def start(): Unit = this.currentTicks = 0

	def tick(tile: TileEntity): Unit = this.currentTicks += 1

	def getTicks: Int = this.currentTicks

	def setTicks(t: Int): Unit = this.currentTicks = t

	def setMaxTicks(t: Int): Unit = this.maxTicks = t

	def canRun(tileEntity: TileEntity, operator: IOperator): Boolean

	def getProgress: Double = this.currentTicks.toDouble / this.maxTicks.toDouble

	def areTicksReady: Boolean = this.currentTicks >= this.maxTicks

	def reset(): Unit = this.currentTicks = -1

	def run(tileEntity: TileEntity, operator: IOperator)

	final def writeTo(tagCom: NBTTagCompound, key: String): Unit = {
		val selfTag: NBTTagCompound = new NBTTagCompound
		this.writeTo(selfTag)
		tagCom.setTag(key, selfTag)
	}

	protected def writeTo(nbt: NBTTagCompound): Unit = {
		nbt.setInteger("maxTicks", this.maxTicks)
		nbt.setInteger("ticks", this.currentTicks)
	}

	final def readFrom(tagCom: NBTTagCompound, key: String): Unit = {
		val selfTag: NBTTagCompound = tagCom.getCompoundTag(key)
		this.readFrom(selfTag)
	}

	protected def readFrom(nbt: NBTTagCompound): Unit = {
		this.maxTicks = nbt.getInteger("maxTicks")
		this.currentTicks = nbt.getInteger("ticks")
	}

	def onUpdate(tile: TileEntity, op: IOperator): Unit = {
		if (!this.isRunning) {
			if (this.canRun(tile, op)) this.start()
		}
		else {
			if (!this.canRun(tile, op)) {
				this.reset()
			}
			else {
				this.tick(tile)
				if (this.areTicksReady) {
					if (!tile.getWorldObj.isRemote) {
						this.run(tile, op)
					}
				}
				/*
				else tile match {
					case callback: IPacketCallback =>
						new PacketTileCallback(tile).add("Ticks").add(this.ticksForOperation).sendToClients()
					case _ =>
				}
				*/
			}
		}
	}

}
