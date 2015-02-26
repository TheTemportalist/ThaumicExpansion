package com.temportalist.scanner.common.lib;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * @author TheTemportalist
 */
public interface IOperation {

	public int ticksForOperation();

	public boolean isRunning();

	public void start();

	public void tick();

	public boolean canRun(Object... specializedParams);

	public boolean areTicksReady();

	public void reset();

	public Object getOutput(World world, ItemStack input, Object... specializedParams);

	public int getAmountToConsume(ItemStack input);

	public Object getCosts();

	// todo see AspectList for nice nbt methods for INBTSaver

	public void writeTo(NBTTagCompound tagCom);

	public void readFrom(NBTTagCompound tagCom);

}
