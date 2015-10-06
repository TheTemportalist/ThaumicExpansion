package com.temportalist.thaumicexpansion.common.tile

import java.util.UUID

import com.temportalist.origin.api.common.lib.V3O
import com.temportalist.origin.api.common.utility.Stacks
import com.temportalist.origin.foundation.common.utility.Players
import com.temportalist.thaumicexpansion.api.common.tile.{IOperator, IOperation, IEnergable}
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.init.TECItems
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import thaumcraft.api.aspects.Aspect
import thaumcraft.api.research.ScanResult

/**
 *
 *
 * @author TheTemportalist
 */
class OperationAnalyzer(private var energyCost: Int) extends IVisOperation {

	private var pos: (World, V3O) = null

	this.addCentiVisDemandPerTick(Aspect.SENSES, 1)

	override def getPosition: (World, V3O) = this.pos

	override def canRun(tileEntity: TileEntity, operator: IOperator): Boolean = {
		this.pos = (tileEntity.getWorldObj, new V3O(tileEntity))
		Stacks.canFit(operator.getInput, operator.getOutput) &&
				tileEntity.isInstanceOf[IEnergable] &&
				tileEntity.asInstanceOf[IEnergable].getEnergy >= this.energyCost
	}

	override def run(tileEntity: TileEntity, operator: IOperator): Unit = {
		if (!this.canRun(tileEntity, operator)) return
		tileEntity match {
			case te: TEAnalyzer =>
				val pUUID: UUID = TECItems.modeItem.getUUID(te.getModeStack)
				val pName: String = Players.getUserName(pUUID)
				if (pName != null) {
					val input: ItemStack = operator.getInput
					val scanResult: ScanResult = te.getScan(input)
					if (te.isValidScanTarget(pName, scanResult, "@")) {
						// add the aspects of the item
						TEC.addScan(pUUID, scanResult)
						operator.finishedOperation(null, input, Array(this.energyCost))
					}
				}
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
