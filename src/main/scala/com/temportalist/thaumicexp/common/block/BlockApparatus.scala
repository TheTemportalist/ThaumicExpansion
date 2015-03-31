package com.temportalist.thaumicexp.common.block

import com.temportalist.origin.wrapper.common.block.BlockWrapperTE
import com.temportalist.thaumicexp.common.TEC
import com.temportalist.thaumicexp.common.tile.TEApparatus
import net.minecraft.block.material.Material

/**
 *
 *
 * @author TheTemportalist
 */
class BlockApparatus(name: String) extends BlockWrapperTE(Material.rock, TEC.MODID, name, classOf[TEApparatus]) {

	this.setHardness(15F)
	this.setResistance(25F)

	override def getRenderType: Int = -1

	override def isOpaqueCube: Boolean = false

}
