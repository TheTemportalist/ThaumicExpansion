package com.temportalist.scanner.common.lib;

import com.temportalist.scanner.common.TEC;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketScannedToServer;
import thaumcraft.common.lib.research.ScanManager;

import java.util.UUID;

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

	/**
	 * @param specializedParams Integer currentEnergy, UUID playerUUID
	 * @return
	 */
	@Override
	public boolean canRun(Object... specializedParams) {
		return (Integer) specializedParams[0] >= this.energyCost &&
				TEC.getPlayerOnline((UUID) specializedParams[1]) != null;
	}

	@Override
	public boolean areTicksReady() {
		return this.currentTicks >= this.maxTicks;
	}

	@Override
	public void reset() {
		this.currentTicks = -1;
	}

	/**
	 * Only works if player is online
	 *
	 * @param input
	 * @param specializedParams Expecting: EntityPlayer
	 * @return output
	 */
	@Override
	public Object getOutput(World world, ItemStack input, Object... specializedParams) {
		try {
			EntityPlayer player = (EntityPlayer) specializedParams[0];
			if (player != null) {
				ScanResult scanResult = new ScanResult(
						(byte) 1, Item.getIdFromItem(input.getItem()), input.getItemDamage(), null,
						""
				);
				if (ScanManager.isValidScanTarget(player, scanResult, "@")) {
					ScanManager.completeScan(player, scanResult, "@");
					PacketHandler.INSTANCE.sendToServer(
							new PacketScannedToServer(scanResult, player, "@")
					);
					return input;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getAmountToConsume(ItemStack input) {
		return 0;
	}

	@Override
	public Object getCosts() {
		return new int[] { this.energyCost };
	}

	@Override
	public void writeTo(NBTTagCompound tagCom) {
		tagCom.setInteger("maxTicks", this.maxTicks);
		tagCom.setInteger("energy", this.energyCost);
		tagCom.setInteger("ticks", this.currentTicks);

	}

	@Override
	public void readFrom(NBTTagCompound tagCom) {
		this.maxTicks = tagCom.getInteger("maxTicks");
		this.energyCost = tagCom.getInteger("energy");
		this.currentTicks = tagCom.getInteger("ticks");

	}

}
