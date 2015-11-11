package com.temportalist.thaumicexpansion.common.block

import java.util

import com.temportalist.origin.api.common.block.BlockTile
import com.temportalist.origin.api.common.lib.{BlockState, V3O}
import com.temportalist.origin.api.common.utility.Stacks
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MathHelper
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection
import thaumcraft.api.aspects.AspectList

/**
 *
 *
 * @author TheTemportalist
 */
class BlockAnalyzer(name: String) extends BlockTile(
	Material.rock, TEC.MODID, name, classOf[TEAnalyzer]) {

	this.setHardness(15F)
	this.setResistance(25F)

	override def getRenderType: Int = -1

	override def isOpaqueCube: Boolean = false

	/*
	override def getSubBlocks(item: Item, tab: CreativeTabs, list: util.List[_]): Unit = {
		for (stack: ItemStack <- TECBlocks.analyzerStacks)
			list.asInstanceOf[util.List[ItemStack]].add(stack)
	}
	*/

	override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int,
			side: ForgeDirection): Boolean = side == ForgeDirection.DOWN

	override def removedByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int,
			willHarvest: Boolean): Boolean = {
		if (!player.capabilities.isCreativeMode) {
			val pos: V3O = new V3O(x, y, z)
			pos.getTile(world) match {
				case tile: TEAnalyzer =>
					val tagCom = new NBTTagCompound
					tile.writeNBT_Inv(tagCom, "Inventory")
					tile.writeNBT_Energy(tagCom, "Energable")
					tile.getAspects.writeToNBT(tagCom, "aspects")

					val stack = new ItemStack(this)
					stack.setTagCompound(tagCom)
					Stacks.spawnItemStack(world, pos, stack, world.rand, 10)
				case _ =>
			}
		}
		super.removedByPlayer(world, player, x, y, z, willHarvest)
	}

	override def getDrops_Pre(world: World, pos: V3O, state: BlockState,
			tile: TileEntity): util.List[ItemStack] = new util.ArrayList[ItemStack]()

	override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase,
			stack: ItemStack): Unit = {
		world.getTileEntity(x, y, z) match {
			case tile: TEAnalyzer =>
				if (tile == null || stack.getTagCompound == null) return
				val tagCom = stack.getTagCompound
				if (tagCom.hasKey("Inventory")) tile.readNBT_Inv(tagCom, "Inventory")
				if (tagCom.hasKey("Energable")) tile.readNBT_Energy(tagCom, "Energable")
				if (tagCom.hasKey("aspects")) {
					val aspects = new AspectList()
					aspects.readFromNBT(tagCom, "aspects")
					tile.setAspects(aspects)
				}
			case _ =>
		}
	}

	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
			side: Int, offsetX: Float, offsetY: Float, offsetZ: Float): Boolean = {
		if (!player.isSneaking) {
			player.openGui(TEC, 0, world, x, y, z)
			true
		}
		else false
	}

}
