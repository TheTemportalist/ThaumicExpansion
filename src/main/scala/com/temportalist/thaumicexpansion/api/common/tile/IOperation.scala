package com.temportalist.thaumicexpansion.api.common.tile

import com.temportalist.thaumicexpansion.common.tile.TileOperator
import net.minecraft.nbt.NBTTagCompound

/**
 *
 *
 * @author TheTemportalist
 */
class IOperation {

	protected var maxTicks: Int = 0
	protected var currentTicks: Int = -1
	protected var energyCost: Int = 0

	final def setMaxTicks(ticks: Int): Unit = this.maxTicks = ticks

	final def setEnergyCost(energy: Int): IOperation = {
		this.energyCost = energy
		this
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Getters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	def getTotalTicks: Int = this.maxTicks

	def getTicks: Int = this.currentTicks

	def getProgress: Double = this.currentTicks.toDouble / this.maxTicks.toDouble

	def getEnergyCost: Int = this.energyCost

	// ~~~~~~~~~~~~~~~~~~~~~~~~ Checks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def isRunning: Boolean = this.currentTicks >= 0

	def isDone: Boolean = this.currentTicks >= this.maxTicks
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~ Operators ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def stop(): Unit = this.currentTicks = -1

	def start(tile: TileOperator): Unit = this.currentTicks = 0

	def tick(tile: TileOperator): Unit = this.currentTicks += 1

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def onUpdate(tile: TileOperator): Unit = {
		if (!tile.getWorldObj.isRemote) {
			if (this.maxTicks <= 0) return
			if (this.isRunning) this.tick(tile)
			if (this.isDone) {
				this.stop()
				tile.onOperationCompletion(this)
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ NBT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	final def writeTo(tagCom: NBTTagCompound, key: String): Unit = {
		val selfTag: NBTTagCompound = new NBTTagCompound
		this.writeTo(selfTag)
		tagCom.setTag(key, selfTag)
	}

	def writeTo(nbt: NBTTagCompound): Unit = {
		nbt.setInteger("maxTicks", this.maxTicks)
		nbt.setInteger("ticks", this.currentTicks)
		nbt.setInteger("energyCost", this.energyCost)
	}

	final def readFrom(tagCom: NBTTagCompound, key: String): Unit = {
		val selfTag: NBTTagCompound = tagCom.getCompoundTag(key)
		this.readFrom(selfTag)
	}

	def readFrom(nbt: NBTTagCompound): Unit = {
		this.maxTicks = nbt.getInteger("maxTicks")
		this.currentTicks = nbt.getInteger("ticks")
		this.energyCost = nbt.getInteger("energyCost")
	}

}
