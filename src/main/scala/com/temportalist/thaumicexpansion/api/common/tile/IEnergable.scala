package com.temportalist.thaumicexpansion.api.common.tile

import com.temportalist.origin.api.common.inventory.IInv
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.{TileEntityFurnace, TileEntity}

/**
 *
 *
 * @author TheTemportalist
 */
trait IEnergable extends TileEntity with IInv {

	private var currentEnergy: Int = 0
	var loadingEnergy: Int = 0

	def slotFuel(): Int

	def getMaxEnergy: Int

	def getFuel: ItemStack = this.getStackInSlot(this.slotFuel())

	def getEnergy: Int = this.currentEnergy

	def setEnergy(e: Int): Unit = this.currentEnergy = e

	def scaleEnergy(max: Double): Int = {
		((this.currentEnergy.toDouble / this.getMaxEnergy.toDouble) * max).toInt
	}

	def checkEnergy(): Unit = {
		val fuel: ItemStack = this.getFuel
		if (fuel != null) {
			val fuelEnergy: Int = this.getFuelEnergy(fuel)
			if (fuelEnergy > 0 && this.loadingEnergy == 0 &&
					this.currentEnergy + fuelEnergy <= this.getMaxEnergy) {
				this.loadingEnergy += fuelEnergy
				this.decrStackSize(this.slotFuel(), 1)
			}
		}
		if (this.loadingEnergy > 0) {
			val amt: Int = Math.min(50, this.loadingEnergy)
			this.currentEnergy += amt
			this.loadingEnergy -= amt
			if (this.loadingEnergy <= 0) this.markDirty()
		}
	}

	def getFuelEnergy(fuel: ItemStack): Int = {
		TileEntityFurnace.getItemBurnTime(fuel)
	}

	/**
	 * Reduces the energy by amount
	 * @param amt The amount to reduce
	 * @param doOp Whether to actually reduce the energy
	 * @return The amount reduced by (amt if currentEnergy is greater than or equal to amt, currentEnergy otherwise)
	 */
	def reduceEnergy(amt: Int, doOp: Boolean): Int = {
		val amount: Int = Math.min(this.currentEnergy, amt)
		if (!doOp) amount
		else {
			this.currentEnergy -= amount
			this.markDirty()
			amount
		}
	}

	/**
	 * Adds to the energy by amount
	 * @param amt The amount to add
	 * @param doOp Whether to actaully add the energy
	 * @return The amount added (amt if getMaxEnergy >= amt, getMaxEnergy otherwise)
	 */
	def addEnergy(amt: Int, doOp: Boolean): Int = {
		val amount: Int = Math.min(amt, this.getMaxEnergy)
		if (!doOp) amount
		else {
			this.currentEnergy += amount
			this.markDirty()
			amount
		}
	}

	def writeNBT_Energy(tag: NBTTagCompound, key: String): Unit = {
		val nbt: NBTTagCompound = new NBTTagCompound
		nbt.setInteger("currentEnergy", this.currentEnergy)
		nbt.setInteger("loadingEnergy", this.loadingEnergy)
		tag.setTag(key, nbt)
	}

	def readNBT_Energy(tag: NBTTagCompound, key: String): Unit = {
		val nbt: NBTTagCompound = tag.getCompoundTag(key)
		this.currentEnergy = nbt.getInteger("currentEnergy")
		this.loadingEnergy = nbt.getInteger("loadingEnergy")
	}

}
