package com.temportalist.thaumicexp.common.item

import java.util.{List, UUID}

import com.temportalist.origin.library.common.utility.Players
import com.temportalist.origin.wrapper.common.item.ItemWrapper
import com.temportalist.thaumicexp.common.TEC
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.IIcon
import net.minecraft.world.World

/**
 *
 *
 * @author TheTemportalist
 */
class ItemMode(name: String, private var names: Array[String]) extends ItemWrapper(TEC.MODID, name) {

	this.setHasSubtypes(true)

	@SideOnly(Side.CLIENT)
	private var icons: Array[IIcon] = null

	override def getUnlocalizedName(stack: ItemStack): String = {
		"item." + TEC.MODID + ":" + this.names(stack.getItemDamage)
	}

	override def getSubItems(item: Item, tab: CreativeTabs, list: List[_]): Unit = {
		for (i <- 0 until this.names.length)
			list.asInstanceOf[List[ItemStack]].add(new ItemStack(item, 1, i))
	}

	override def addInformation(stack: ItemStack, player: EntityPlayer, info: List[_],
			bool: Boolean): Unit = {
		if (stack.hasTagCompound) {
			info.asInstanceOf[List[String]].add("Player: " + Players.getUserName(
				stack.getTagCompound.getString("uuid")
			))
		}
	}

	@SideOnly(Side.CLIENT)
	override def registerIcons(reg: IIconRegister): Unit = {
		this.icons = new Array[IIcon](this.names.length)
		for (i <- 0 until this.names.length)
			this.icons(i) = reg.registerIcon(TEC.MODID + ":" + this.names(i))
	}

	@SideOnly(Side.CLIENT)
	override def getIconFromDamage(damage: Int): IIcon = this.icons(damage)

	def getUUID(stack: ItemStack): UUID = {
		if (stack != null && stack.hasTagCompound && stack.getTagCompound.hasKey("uuid"))
			UUID.fromString(stack.getTagCompound.getString("uuid"))
		else null
	}

	def setUUID(stack: ItemStack, uuid: UUID): Unit = {
		if (!stack.hasTagCompound) stack.setTagCompound(new NBTTagCompound)
		stack.getTagCompound.setString("uuid", uuid.toString)
	}

	override def onItemRightClick(stack: ItemStack, world: World,
			player: EntityPlayer): ItemStack = {
		if (player.isSneaking) {
			val copy: ItemStack = stack.copy()
			this.setUUID(copy, player.getGameProfile.getId)
			player.setCurrentItemOrArmor(0, copy)
			stack
		}
		else
			stack
	}

}
