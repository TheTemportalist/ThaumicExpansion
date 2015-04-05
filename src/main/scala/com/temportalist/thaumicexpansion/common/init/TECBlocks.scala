package com.temportalist.thaumicexpansion.common.init

import com.temportalist.origin.library.common.register.BlockRegister
import com.temportalist.origin.library.common.utility.Stacks
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.block.{BlockAnalyzer, BlockApparatus}
import com.temportalist.thaumicexpansion.common.tile.{TEAnalyzer, TEApparatus}
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack

/**
 *
 *
 * @author TheTemportalist 3/30/15
 */
object TECBlocks extends BlockRegister {

	var analyzer: BlockAnalyzer = null
	var analyzerStacks: Array[ItemStack] = null
	var apparatus: BlockApparatus = null

	/**
	 * This method is used to register TileEntities.
	 * Recommendation: Use GameRegistry.registerTileEntity
	 */
	override def registerTileEntities(): Unit = {
		GameRegistry.registerTileEntity(classOf[TEAnalyzer], TEC.MODID + "_Analyzer")
		GameRegistry.registerTileEntity(classOf[TEApparatus], TEC.MODID + "_Apparatus")

	}

	override def register(): Unit = {

		this.analyzer = new BlockAnalyzer("thaumicanalyzer")
		this.analyzer.setCreativeTab(CreativeTabs.tabRedstone)

		this.apparatus = new BlockApparatus("arcaneApparatus")
		this.apparatus.setCreativeTab(CreativeTabs.tabRedstone)

	}

	override def registerOther(): Unit = {
		this.analyzerStacks = new Array[ItemStack](4)
		for (i <- 0 until this.analyzerStacks.length) {
			this.analyzerStacks(i) = Stacks.createStack(this.analyzer, Map[String, Any](
				"tier" -> i, "mode" -> TECItems.playerTracker
			))
		}
	}

}
