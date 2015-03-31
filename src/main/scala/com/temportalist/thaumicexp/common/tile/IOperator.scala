package com.temportalist.thaumicexp.common.tile

import net.minecraft.item.ItemStack

/**
 *
 *
 * @author TheTemportalist
 */
trait IOperator {

	def getInput(): ItemStack

	def getOutput(): ItemStack

	def finishedOperation(setInput: ItemStack, toOutput: ItemStack, data: Array[Any]): Unit

}
