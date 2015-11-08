package com.temportalist.thaumicexpansion.common.tile

import com.temportalist.thaumicexpansion.common.TEC
import net.minecraft.item.ItemStack
import thaumcraft.api.aspects.{Aspect, AspectList}

/**
 *
 *
 * @author TheTemportalist
 */
class OperationDecomposer extends IVisOperation {

	this.addCentiVisDemandPerTick(Aspect.MINE, 3)

	var secondaryOutput: Boolean = false
	var aspects: AspectList = new AspectList()

	override def start(tile: TileOperator): Unit = {
		super.start(tile)

		this.secondaryOutput = tile.getWorldObj.rand.nextDouble() < 0.1D
		if (tile.getInput != null) {
			val localInput: ItemStack = tile.getInput.copy()
			localInput.stackSize = 1
			val localList: AspectList =
				TEC.getAspects(tile.asInstanceOf[TEAnalyzer].getScan(localInput))
			val chances: Array[Double] = TEC.complexityTierChance(0) // TODO
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

}
