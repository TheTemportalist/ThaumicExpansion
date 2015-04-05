package com.temportalist.thaumicexpansion.common.tile

import java.util.UUID

import com.temportalist.origin.library.common.utility.Stacks
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.init.TECItems
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import thaumcraft.api.aspects.{Aspect, AspectList}

/**
 *
 *
 * @author TheTemportalist
 */
class OperationDecomposer(tile: TEAnalyzer)
		extends IOperation {

	private var energyCost: Int = 0
	var secondaryOutput: Boolean = tile.getWorldObj.rand.nextDouble() < 0.1D
	var aspects: AspectList = new AspectList()

	override def start: Unit = {
		super.start
		if (tile.getInput() != null) {
			val localInput: ItemStack = tile.getInput().copy()
			localInput.stackSize = 1
			val localList: AspectList = TEC.getAspects(tile.getScan(localInput))
			val chances: Array[Double] = TEC.complexityTierChance(tile.getTier())
			for (aspect: Aspect <- localList.getAspects) {
				for (i <- 1 to localList.getAmount(aspect)) {
					val tier: Int = TEC.getAspectTier(aspect) - 1
					if (tile.getWorldObj.rand.nextDouble() < chances(tier)) {
						this.aspects.add(aspect, 1)
						this.maxTicks += TEC.timeEnergyPerStats(tier).getKey
						this.energyCost += TEC.timeEnergyPerStats(tier).getValue
					}
				}
			}
		}
	}

	override def canRun(tileEntity: TileEntity, operator: IOperator): Boolean = {
		Stacks.canFit(operator.getInput(), operator.getOutput()) &&
				tileEntity.isInstanceOf[IEnergable] &&
				tileEntity.asInstanceOf[IEnergable].getEnergy() >= this.energyCost
	}

	override def run(tileEntity: TileEntity, operator: IOperator): Unit = {
		if (!this.canRun(tileEntity, operator)) return
		tileEntity match {
			case te: TEAnalyzer =>
				val pUUID: UUID = TECItems.modeItem.getUUID(te.getModeStack())

				// todo hide aspects in gui as ?s if player doesnt know them

				if (pUUID != null) TEC.addAspects(pUUID, this.aspects)
				else te.addAspects(this.aspects)

				var input: ItemStack = operator.getInput
				var output: ItemStack = null
				if (te.getWorldObj.rand.nextDouble < .1d) {
					// you are able to keep the item
					output = input.copy
					output.stackSize = 1
				}
				input.stackSize -= 1
				if (input.stackSize <= 0) input = null
				operator.finishedOperation(input, output, Array(this.energyCost))
			case _ =>
		}
	}

	override protected def readFrom(nbt: NBTTagCompound): Unit = {
		super.readFrom(nbt)
		nbt.setInteger("energy", this.energyCost)
	}

	override protected def writeTo(nbt: NBTTagCompound): Unit = {
		super.writeTo(nbt)
		this.energyCost = nbt.getInteger("energy")
	}

}
