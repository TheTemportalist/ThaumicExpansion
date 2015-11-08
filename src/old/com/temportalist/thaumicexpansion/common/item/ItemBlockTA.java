package com.temportalist.thaumicexpansion.common.item;

import cofh.lib.util.helpers.StringHelper;
import com.temportalist.thaumicexpansion.common.block.BlockThaumicAnalyzer;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * @author TheTemportalist
 */
public class ItemBlockTA extends ItemBlock {

	public ItemBlockTA(Block block) {
		super(block);
		this.setHasSubtypes(true);
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return StringHelper.localize(this.getUnlocalizedName(stack) + ":" +
						((BlockThaumicAnalyzer) this.field_150939_a).getTier(stack.getItemDamage())
		);
	}

	@Override
	public int getMetadata(int i) {
		return i;
	}

}
