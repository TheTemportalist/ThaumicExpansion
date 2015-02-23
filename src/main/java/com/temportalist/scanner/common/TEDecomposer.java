package com.temportalist.scanner.common;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyStorage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * @author TheTemportalist
 */
public class TEDecomposer extends TileEntity implements ISidedInventory, IEnergyHandler {

	// todo config max
	protected EnergyStorage energyStorage = new EnergyStorage(8000);
	ItemStack[] stacks = new ItemStack[1];
	AspectList heldAspects = new AspectList();
	int timeUntilNextDecompose = -1;

	EnumDecomposerSide[] sideTypes = new EnumDecomposerSide[6];

	@Override
	public void updateEntity() {
		if (this.stacks[0] != null) {
			// Int energy, AspectList filteredList, Int time
			Object[] variables = this.calculateAndFilter(this.stacks[0]);
			int energyRequired = (Integer) variables[0];
			if (this.energyStorage.getEnergyStored() <= energyRequired) {
				if (this.timeUntilNextDecompose < 0) {
					this.timeUntilNextDecompose = (Integer) variables[2] * 20; // times 20 for ticks
				}
				else if (this.timeUntilNextDecompose > 0) {
					this.timeUntilNextDecompose -= 1;
				}
				else {
					// todo wait time
					AspectList aspectList = (AspectList) variables[1];
					Aspect[] aspects = aspectList.getAspects();
					for (int i = 0; i < aspects.length; i++) {
						this.heldAspects.add(aspects[i], aspectList.getAmount(aspects[i]));
					}
					// decrement the current stack
					this.decrStackSize(0, 1);
					// decrease the energy
					this.energyStorage.extractEnergy(energyRequired, false);
					this.timeUntilNextDecompose = -1;
				}
			}
		}
	}

	private Object[] calculateAndFilter(ItemStack stack) {
		AspectList aspectList = this.filterAspects(
				ThaumcraftApiHelper.getObjectAspects(stack)
		);
		Aspect[] aspects = aspectList.getAspects();
		int energy = 0;
		int time = 0;
		for (int i = 0; i < aspects.length; i++) {
			Aspect aspect = aspects[i];
			// todo wish Azanor had a Aspect.getTier method
			int tier = Scanner.instance.aspectTiers.containsKey(aspect) ?
					Scanner.instance.aspectTiers.get(aspect) :
					3;
			int[] stats = Scanner.instance.decompositionStats.get(tier);
			energy += stats[0];
			time += stats[1];
		}
		return new Object[] { energy, aspectList, time };
	}

	// todo
	private AspectList filterAspects(AspectList aspectList) {
		return aspectList;
	}

	@SideOnly(Side.CLIENT)
	public IIcon[] getIcons(ForgeDirection dir) {
		int side = dir.ordinal();
		IIcon[] icons = new IIcon[2];
		icons[0] = this.getBlockType().getIcon(side, this.getBlockMetadata());
		if (dir.ordinal() != ((BlockDecomposer) this.getBlockType()).getDirection(
				this.getBlockMetadata()
		).ordinal())
			icons[1] = this.sideTypes[side] != null ?
					this.sideTypes[side].getIcon(side) :
					EnumDecomposerSide.EMPTY.getIcon(side);
		return icons;
	}

	// ~~~~~~~~~~~~~~~ Below is background functionality, Tinker at your own risk ~~~~~~~~~~~~~~~

	@Override
	public int getSizeInventory() {
		return this.stacks.length;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public String getInventoryName() {
		return "Decomposer";
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot >= 0 && slot < this.stacks.length)
			return this.stacks[slot];
		return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		return this.getStackInSlot(slot);
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (this.isItemValidForSlot(slot, stack)) {
			this.stacks[slot] = stack.copy();
			this.markDirty();
		}
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
		if (slot >= 0 && slot < this.stacks.length) {
			AspectList list = ThaumcraftApiHelper.getObjectAspects(itemStack);
			return list != null && list.size() > 0;
		}
		return false;
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		if (slot >= 0 && slot < this.stacks.length && this.stacks[slot] != null) {
			ItemStack stack;
			if (this.stacks[slot].stackSize < amount) {
				stack = this.stacks[slot].copy();
				this.stacks[slot] = null;
			}
			else {
				stack = this.stacks[slot].splitStack(amount);
				if (this.stacks[slot].stackSize <= 0)
					this.stacks[slot] = null;
			}
			this.markDirty();
			return stack;
		}
		return null;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return new int[] { 0 };
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return slot >= 0 && slot < this.stacks.length && this.stacks[slot] != null && (
				this.sideTypes[side] == EnumDecomposerSide.INPUT_ITEM
		);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return false;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return true; // todo only true if has experience in thaumcraft
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return true;
	}

	public IEnergyStorage getEnergyStorage() {
		return this.energyStorage;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return this.energyStorage.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return this.energyStorage.getMaxEnergyStored();
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		return this.energyStorage.receiveEnergy(maxReceive, simulate);
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
		return 0;//this.energyStorage.extractEnergy(maxExtract, simulate);
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tagCom = new NBTTagCompound();
		this.writeToNBT(tagCom);
		return new S35PacketUpdateTileEntity(
				this.xCoord, this.yCoord, this.zCoord, this.getBlockMetadata(), tagCom
		);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.func_148857_g());
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCom) {
		super.writeToNBT(tagCom);

		NBTTagList stackTags = new NBTTagList();
		for (int slot = 0; slot < this.getSizeInventory(); slot++) {
			if (this.stacks[slot] != null) {
				NBTTagCompound stackTag = new NBTTagCompound();
				this.stacks[slot].writeToNBT(stackTag);
				stackTag.setInteger("slot", slot);
				stackTags.appendTag(stackTag);
			}
		}
		tagCom.setTag("stackTags", stackTags);

		this.energyStorage.writeToNBT(tagCom);

		tagCom.setInteger("timeToDecompose", this.timeUntilNextDecompose);

		for (int side = 0; side < this.sideTypes.length; side++)
			if (this.sideTypes[side] != null)
				tagCom.setInteger("side" + side, this.sideTypes[side].ordinal());

	}

	@Override
	public void readFromNBT(NBTTagCompound tagCom) {
		super.readFromNBT(tagCom);

		NBTTagList stackTags = tagCom.getTagList("stackTags", 10);
		for (int i = 0; i < stackTags.tagCount(); i++) {
			NBTTagCompound stackTag = stackTags.getCompoundTagAt(i);
			int slot = stackTag.getInteger("slot");
			this.stacks[slot] = ItemStack.loadItemStackFromNBT(stackTag);
		}

		this.energyStorage.readFromNBT(tagCom);

		this.timeUntilNextDecompose = tagCom.getInteger("timeToDecompose");

		for (int side = 0; side < 6; side++)
			this.sideTypes[side] = EnumDecomposerSide.values()[tagCom.getInteger("side" + side)];

	}

}
