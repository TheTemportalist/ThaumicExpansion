package com.temportalist.thaumicexpansion.common.item

import java.util.{List, UUID}

import com.temportalist.origin.api.common.item.ItemBase
import com.temportalist.origin.api.common.utility.Generic
import com.temportalist.origin.foundation.common.utility.Players
import com.temportalist.thaumicexpansion.api.common.tile.IOperation
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.tile.{OperationDecomposer, OperationAnalyzer}
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
class ItemMode(name: String, private var names: Array[String]) extends ItemBase(TEC.MODID, name) {

	this.setHasSubtypes(true)

	@SideOnly(Side.CLIENT)
	private var icons: Array[IIcon] = _

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
			val uuid = UUID.fromString(stack.getTagCompound.getString("uuid"))
			val name = Players.getUserName(uuid)
			Generic.addToList(info, "UUID: " + uuid.toString)
			Generic.addToList(info,
				"Player: " + (if (name != null) name else stack.getTagCompound.getString("name")))
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
			var id = player.getGameProfile.getId
			val name = player.getGameProfile.getName

			if (id == null) {
				if (name != null) id = Players.usernameToId.get(name)
				else Players.message(player,
					"ID and Name of your GameProfile are null. Report this to mod author.")
			}
			if (id != null) {
				val copy: ItemStack = stack.copy()
				this.setUUID(copy, player.getGameProfile.getId)
				copy.getTagCompound.setString("name", name)
				player.setCurrentItemOrArmor(0, copy)
			}
		}
		stack
	}

	def getOperationForStack(stack: ItemStack): IOperation = {
		stack.getItemDamage match {
			case 0 => new OperationAnalyzer().setEnergyCost(50)
			case 1 => new OperationDecomposer
			case _ => null
		}
	}

}
