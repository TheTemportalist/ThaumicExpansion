package com.temportalist.thaumicexpansion.api.common.tile

import net.minecraft.item.ItemStack

/**
 *
 *
 * @author TheTemportalist
 */
trait IOperator {

	def getInput: ItemStack

	def getOutput: ItemStack

	def onOperationCompletion(operation: IOperation): Unit

}
