package com.temportalist.scanner.common;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyStorage;
import cofh.api.tileentity.IAugmentable;
import cofh.api.tileentity.IReconfigurableFacing;
import cofh.api.tileentity.IReconfigurableSides;
import cofh.api.tileentity.ISidedTexture;
import cofh.core.network.PacketHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author TheTemportalist
 */
public class TEDecomposer extends TileEntity implements ISidedInventory, IEnergyHandler,
		IAugmentable, IReconfigurableFacing, IReconfigurableSides, ISidedTexture {

	// todo config max
	protected EnergyStorage energyStorage = new EnergyStorage(8000);
	private ItemStack[] stacks = new ItemStack[1];
	private ItemStack[] augments = new ItemStack[1];
	private List<String> hasAugments = new ArrayList<String>();
	private AspectList heldAspects = new AspectList();
	private int timeUntilNextDecompose = -1;

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
		icons[0] = this.getBlock().getIcon(side, this.getBlockMetadata());
		if (dir.ordinal() != ((BlockDecomposer) this.getBlock()).getDirection(
				this.getBlockMetadata()
		).ordinal())
			icons[1] = this.sideTypes[side] != null ?
					this.sideTypes[side].getIcon(side) :
					EnumDecomposerSide.EMPTY.getIcon(side);
		return icons;
	}

	public BlockDecomposer getBlock() {
		return (BlockDecomposer) this.getBlockType();
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
	public ItemStack[] getAugmentSlots() {
		return this.augments;
	}

	/**
	 * Ran whenever augments change
	 */
	@Override
	public void installAugments() {
		this.hasAugments.clear();
		for (int slot = 0; slot < this.augments.length; slot++) {
			if (this.augments[slot] != null) {
				this.hasAugments.add(this.augments[slot].getItem().getUnlocalizedName());
			}
		}
		this.markDirty();
	}

	@Override
	public boolean[] getAugmentStatus() {
		return new boolean[0];
	}

	/**
	 * @return the direction the block is facing
	 */
	@Override
	public int getFacing() {
		return this.getBlock().getRotation(this.getBlockMetadata());
	}

	/**
	 * @return whether this block can face up/down
	 */
	@Override
	public boolean allowYAxisFacing() {
		return false;
	}

	/*
	// todo to origin
	private ForgeDirection[] clockwise = new ForgeDirection[] {
			ForgeDirection.NORTH,
			ForgeDirection.EAST,
			ForgeDirection.SOUTH,
			ForgeDirection.WEST
	};
	*/

	/**
	 * Rotate clockwise
	 *
	 * @return
	 */
	@Override
	public boolean rotateBlock() { // todo is this broken?
		return this.setFacing(
				this.getBlock().getDirection(
						this.getBlockMetadata()
				).getRotation(ForgeDirection.UP).ordinal()
		);
	}

	@Override
	public boolean setFacing(int i) {
		this.getBlock().setTierAndDir(
				this.getWorldObj(), this.xCoord, this.yCoord, this.zCoord,
				this.getBlock().getTier(this.getBlockMetadata()),
				ForgeDirection.getOrientation(i)
		);
		this.markDirty();
		return true;
	}

	private boolean isSideFront(int side) {
		return side == this.getBlock().getRotation(this.getBlockMetadata());
	}

	@Override
	public boolean decrSide(int side) {
		return this.setSide(side, this.sideTypes[side].last().ordinal());
	}

	@Override
	public boolean incrSide(int side) {
		return this.setSide(side, this.sideTypes[side].next().ordinal());
	}

	@Override
	public boolean setSide(int side, int index) {
		if (!this.isSideFront(side)) {
			this.sideTypes[side] = EnumDecomposerSide.values()[index];
			PacketHandler.sendToServer(new PacketTileSync(this));
			PacketHandler.sendToAll(new PacketTileSync(this));
			//this.markDirty();
			return true;
		}
		return false;
	}

	@Override
	public boolean resetSides() {
		for (int side = 0; side < this.sideTypes.length; side++)
			this.sideTypes[side] = EnumDecomposerSide.EMPTY;
		this.markDirty();
		return true;
	}

	@Override
	public int getNumConfig(int side) {
		return !this.isSideFront(side) ? EnumDecomposerSide.values().length : 0;
	}

	@Override
	public IIcon getTexture(int side, int type) {
		return !this.isSideFront(side) ?
				EnumDecomposerSide.values()[type].getIcon(side) :
				this.getBlock().getIcon(side, this.getBlockMetadata());
	}

	public void sendGuiNetworkData(Container container, ICrafting player) {
	}

	public void receiveGuiNetworkData(int i, int j) {
	}

	@Override
	public void markDirty() {
		super.markDirty();
		this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
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

		// todo write auto thing in TEWrapper for Origin inventories
		// todo to automate the reading/writing of arrays
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

		NBTTagList augmentTags = new NBTTagList();
		for (int slot = 0; slot < this.augments.length; slot++) {
			if (this.augments[slot] != null) {
				NBTTagCompound augmentTag = new NBTTagCompound();
				this.augments[slot].writeToNBT(augmentTag);
				augmentTag.setInteger("slot", slot);
				augmentTags.appendTag(augmentTag);
			}
		}
		tagCom.setTag("augmentTags", augmentTags);

		this.energyStorage.writeToNBT(tagCom);

		tagCom.setInteger("timeToDecompose", this.timeUntilNextDecompose);

		for (int side = 0; side < this.sideTypes.length; side++)
			if (this.sideTypes[side] != null)
				tagCom.setInteger("side" + side, this.sideTypes[side].ordinal());

		NBTTagList augmentList = new NBTTagList();
		for (String augment : this.hasAugments) {
			NBTTagCompound augmentTag = new NBTTagCompound();
			augmentTag.setString("name", augment);
			augmentList.appendTag(augmentTag);
		}
		tagCom.setTag("augmentList", augmentList);

		this.heldAspects.writeToNBT(tagCom, "heldAspects");

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

		NBTTagList augmentTags = tagCom.getTagList("augmentTags", 10);
		for (int i = 0; i < augmentTags.tagCount(); i++) {
			NBTTagCompound augmentTag = augmentTags.getCompoundTagAt(i);
			int slot = augmentTag.getInteger("slot");
			this.augments[slot] = ItemStack.loadItemStackFromNBT(augmentTag);
		}

		this.energyStorage.readFromNBT(tagCom);

		this.timeUntilNextDecompose = tagCom.getInteger("timeToDecompose");

		for (int side = 0; side < 6; side++)
			this.sideTypes[side] = EnumDecomposerSide.values()[tagCom.getInteger("side" + side)];

		this.hasAugments.clear();
		NBTTagList augmentList = tagCom.getTagList("augmentList", 10);
		for (int i = 0; i < augmentList.tagCount(); i++) {
			this.hasAugments.add(augmentList.getCompoundTagAt(i).getString("name"));
		}

		this.heldAspects.readFromNBT(tagCom, "heldAspects");

	}

}
