package com.temportalist.thaumicexpansion.common.lib;

import net.minecraft.item.ItemStack;

/**
 * @author TheTemportalist
 */
public interface IOperator {

	public ItemStack getInput();

	public ItemStack getOutput();

	public void finishedOperation(ItemStack setInput, ItemStack toOutput);

}
