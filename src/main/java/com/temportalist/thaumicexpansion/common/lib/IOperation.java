package com.temportalist.thaumicexpansion.common.lib;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.Set;

/**
 * @author TheTemportalist
 */
public interface IOperation {

	public int ticksForOperation();

	public boolean isRunning();

	public void start();

	public void tick();

	public boolean canRun(TileEntity tileEntity, IOperator operator);

	public double getProgress();

	public boolean areTicksReady();

	public void reset();

	public void updateAugments(IOperator operator, Set<EnumAugmentTA> augments);

	public void run(TileEntity tileEntity, IOperator operator);

	public Object getCosts();

	// todo warning see AspectList for nice nbt methods for INBTSaver

	public void writeTo(NBTTagCompound tagCom, String key);

	public void readFrom(NBTTagCompound tagCom, String key);

}
