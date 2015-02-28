package com.temportalist.thaumicexpansion.common.item;

import cofh.lib.util.helpers.StringHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import java.util.List;

/**
 * @author TheTemportalist
 */
public class ItemBlockTA extends ItemBlock {

	public ItemBlockTA(Block block) {
		super(block);
		this.setHasSubtypes(true);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return StringHelper.localize(this.getUnlocalizedName(item));
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean something) {
		if (stack.hasTagCompound()) {
			list.add("Augments:");
			NBTTagList augments = stack.getTagCompound().getTagList("augments", 10);
			for (int i = 0; i < augments.tagCount(); i++) {
				list.add("  " + augments.getCompoundTagAt(i).getString("stack"));
			}
		}
	}

	@Override
	public int getMetadata(int i) {
		return i;
	}

}
