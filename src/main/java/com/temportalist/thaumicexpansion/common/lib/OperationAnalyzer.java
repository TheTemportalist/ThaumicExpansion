package com.temportalist.thaumicexpansion.common.lib;

import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketScannedToServer;
import thaumcraft.common.lib.research.ScanManager;

/**
 * @author TheTemportalist
 */
public class OperationAnalyzer implements IOperation {

	private int maxTicks, energyCost, currentTicks = -1;

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
				tile.getCurrentPlayer() != null && this.canOperateWithIO(operator);
	}

	private boolean canOperateWithIO(IOperator operator) {
		return operator.getOutput() == null || (
				operator.getInput().getItem() == operator.getOutput().getItem() &&
						operator.getInput().getItemDamage() == operator.getOutput().stackSize &&
						ItemStack.areItemStackTagsEqual(operator.getInput(), operator.getOutput())
						&& operator.getOutput().stackSize + operator.getInput().stackSize <=
						operator.getOutput().getMaxStackSize()
		);
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
	public void run(TileEntity tileEntity, IOperator operator) {
		if (!this.canRun(tileEntity, operator))
			return;
		TEThaumicAnalyzer tile = (TEThaumicAnalyzer) tileEntity;
		EntityPlayer player = tile.getCurrentPlayer();
		if (player != null) {
			ItemStack input = operator.getInput();
			ScanResult scanResult = new ScanResult(
					(byte) 1, Item.getIdFromItem(input.getItem()), input.getItemDamage(), null,
					""
			);
			if (ScanManager.isValidScanTarget(player, scanResult, "@")) {
				ScanManager.completeScan(player, scanResult, "@");
				PacketHandler.INSTANCE.sendToServer(
						new PacketScannedToServer(scanResult, player, "@")
				);
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

		tagCom.setTag(key, selfTag);
	}

	@Override
	public void readFrom(NBTTagCompound tagCom, String key) {
		NBTTagCompound selfTag = tagCom.getCompoundTag(key);

		this.maxTicks = selfTag.getInteger("maxTicks");
		this.energyCost = selfTag.getInteger("energy");
		this.currentTicks = selfTag.getInteger("ticks");

	}

}
