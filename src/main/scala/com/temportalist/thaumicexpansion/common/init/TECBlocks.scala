package com.temportalist.thaumicexpansion.common.init

import com.temportalist.origin.foundation.common.register.BlockRegister
import com.temportalist.thaumicexpansion.common.block.{BlockAnalyzer, BlockApparatus}
import com.temportalist.thaumicexpansion.common.tile.{TEAnalyzer, TEApparatus}
import com.temportalist.thaumicexpansion.common.{JavaHelper, TEC}
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.oredict.ShapedOreRecipe
import thaumcraft.api.aspects.{Aspect, AspectList}
import thaumcraft.api.research.ResearchPage
import thaumcraft.common.config.{ConfigBlocks, ConfigItems}

/**
 *
 *
 * @author TheTemportalist 3/30/15
 */
object TECBlocks extends BlockRegister {

	var analyzer: BlockAnalyzer = null
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

	}

}
