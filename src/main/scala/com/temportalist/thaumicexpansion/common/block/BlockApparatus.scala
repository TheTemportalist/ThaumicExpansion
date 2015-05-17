package com.temportalist.thaumicexpansion.common.block

import com.temportalist.origin.api.common.block.BlockTile
import com.temportalist.origin.api.common.lib.V3O
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.tile.TEApparatus
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

/**
 *
 *
 * @author TheTemportalist
 */
class BlockApparatus(name: String)
		extends BlockTile(Material.rock, TEC.MODID, name, classOf[TEApparatus]) {

	this.setHardness(15F)
	this.setResistance(25F)

	override def getRenderType: Int = -1

	override def isOpaqueCube: Boolean = false

	override def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: Int): Boolean = {
		TEApparatus.isValidConnectorTile(
			(new V3O(x, y, z) + ForgeDirection.getOrientation(side).getOpposite).getTile(world)
		)
	}

	override def onBlockPlaced(world: World, x: Int, y: Int, z: Int, side: Int, subX: Float,
			subY: Float, subZ: Float, metadata: Int): Int =
		ForgeDirection.getOrientation(side).getOpposite.ordinal()

	override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase,
			stack: ItemStack): Unit = this.checkTileSides(world, x, y, z)

	override def onNeighborBlockChange(worldIn: World, x: Int, y: Int, z: Int,
			neighbor: Block): Unit = this.checkTileSides(worldIn, x, y, z)

	override def onNeighborChange(world: IBlockAccess, x: Int, y: Int, z: Int, tileX: Int,
			tileY: Int, tileZ: Int): Unit = world.getTileEntity(x, y, z) match {
		case tile: TEApparatus => tile.checkSide(new V3O(tile), new V3O(tileX, tileY, tileZ))
		case _ =>
	}

	private def checkTileSides(world: World, x: Int, y: Int, z: Int): Unit = {
		world.getTileEntity(x, y, z) match {
			case tile: TEApparatus => tile.checkAllSides()
			case _ =>
		}
	}

}
