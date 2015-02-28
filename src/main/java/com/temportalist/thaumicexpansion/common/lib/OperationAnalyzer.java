package com.temportalist.thaumicexpansion.common.lib;

import com.temportalist.thaumicexpansion.common.TEC;
import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import thaumcraft.api.research.ScanResult;

import java.util.Set;

/**
 * @author TheTemportalist
 */
public class OperationAnalyzer implements IOperation {

	private int maxTicks, energyCost, currentTicks = -1;
	private boolean hasAdjuster = false;

	public OperationAnalyzer(int time, int energyInput) {
		this.maxTicks = time * 20;
		this.energyCost = energyInput;
	}

	@Override
	public int ticksForOperation() {
		return this.maxTicks;
	}

	@Override
	public boolean isRunning() {
		return this.currentTicks != -1;
	}

	@Override
	public void start() {
		this.currentTicks = 0;
	}

	@Override
	public void tick() {
		this.currentTicks += 1;
	}

	@Override
	public boolean canRun(TileEntity tileEntity, IOperator operator) {
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) tileEntity;
		return tile.getEnergyStorage().getEnergyStored() >= this.energyCost &&
				tile.getCurrentPlayerName() != null && this.canOperateWithIO(operator);
	}

	private boolean canOperateWithIO(IOperator operator) {
		return operator.getOutput() == null ||
				TEC.canInsertBIntoA(operator.getOutput(), operator.getInput());
	}

	@Override
	public double getProgress() {
		return (double) this.currentTicks / (double) this.maxTicks;
	}

	@Override
	public boolean areTicksReady() {
		return this.currentTicks >= this.maxTicks;
	}

	@Override
	public void reset() {
		this.currentTicks = -1;
	}

	@Override
	public void updateAugments(IOperator operator, Set<EnumAugmentTA> augments) {
		this.hasAdjuster = augments.contains(EnumAugmentTA.THAUMIC_ADJUSTER);
	}

	@Override
	public void run(TileEntity tileEntity, IOperator operator) {
		if (!this.canRun(tileEntity, operator))
			return;
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) tileEntity;
		String pName = tile.getCurrentPlayerName();
		if (pName != null) {
			ItemStack input = operator.getInput();
			ScanResult scanResult = tile.getScan(input);
			if (tile.isValidScanTarget(pName, scanResult, "@")) {
				double aspectChance = 0d;
				double aspectPercentChange = 1d;
				if (this.hasAdjuster) {
					aspectChance = .5d;
					aspectPercentChange = 1.1d;
				}
				TEC.addAspect(tile.getCurrentPlayerUUID(), scanResult, aspectChance,
						aspectPercentChange);
				if (tile.getWorldObj().rand.nextDouble() >= tile.getSecondaryChance())
					input = null;
				operator.finishedOperation(null, input);
			}
		}
	}

	@Override
	public Object getCosts() {
		return new int[] { this.energyCost };
	}

	@Override
	public void writeTo(NBTTagCompound tagCom, String key) {
		NBTTagCompound selfTag = new NBTTagCompound();

		selfTag.setInteger("maxTicks", this.maxTicks);
		selfTag.setInteger("energy", this.energyCost);
		selfTag.setInteger("ticks", this.currentTicks);
		selfTag.setBoolean("hasAdjuster", this.hasAdjuster);

		tagCom.setTag(key, selfTag);
	}

	@Override
	public void readFrom(NBTTagCompound tagCom, String key) {
		NBTTagCompound selfTag = tagCom.getCompoundTag(key);

		this.maxTicks = selfTag.getInteger("maxTicks");
		this.energyCost = selfTag.getInteger("energy");
		this.currentTicks = selfTag.getInteger("ticks");
		this.hasAdjuster = selfTag.getBoolean("hasAdjuster");

	}

}
