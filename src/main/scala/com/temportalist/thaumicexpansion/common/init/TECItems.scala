package com.temportalist.thaumicexpansion.common.init

import com.temportalist.origin.library.common.register.ItemRegister
import com.temportalist.thaumicexpansion.common.item.ItemMode
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack

/**
 *
 *
 * @author TheTemportalist 3/30/15
 */
object TECItems extends ItemRegister {

	var modeItem: ItemMode = null
	var playerTracker: ItemStack = null
	var decomposer: ItemStack = null

	override def register(): Unit = {
		this.modeItem = new ItemMode("mode", Array[String]("playerTracker", "decomposer"))
		this.modeItem.setCreativeTab(CreativeTabs.tabRedstone)

		this.initStacks()
	}

	def initStacks(): Unit = {
		this.playerTracker = new ItemStack(this.modeItem, 1, 0)
		this.decomposer = new ItemStack(this.modeItem, 1, 1)
	}

}
