package com.temportalist.scanner.common;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyStorage;
import cofh.api.tileentity.*;
import cofh.core.network.PacketHandler;
import cofh.lib.util.position.IRotateableTile;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.playerdata.PacketAspectPool;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author TheTemportalist
 */
public class TEDecomposer extends TileEntity implements ISidedInventory, IEnergyHandler,
		IAugmentable, IRotateableTile, IReconfigurableFacing, IReconfigurableSides, ISidedTexture,
		IRedstoneControl, IAspectContainer, IEssentiaTransport {

	public static final int hexagonProgressSteps = 13;

	protected EnergyStorage energyStorage = new EnergyStorage(Scanner.maxEnergyStorage);
	private ItemStack[] stacks = new ItemStack[2];
	private ItemStack[] augments = new ItemStack[3];
	private List<EnumDecomposerAugment> augmentList = new ArrayList<EnumDecomposerAugment>();
	private AspectList aspects = new AspectList();
	private Object[] outputVars = null;
	private int currentTimeMax = -1;
	private int timeUntilNextDecompose = -1;
	private int currentColumnOffset = 0;

	EnumDecomposerSide[] sideTypes = new EnumDecomposerSide[6];
	ControlMode currentControl = ControlMode.DISABLED;
	boolean powered = false;

	@Override
	public void updateEntity() {
		if (this.canOperate() && this.stacks[0] != null) {
			if (this.timeUntilNextDecompose < 0) {
				// Int energy, AspectList filteredList, Int time
				this.outputVars = this.calculateAndFilter(this.stacks[0]);
				int energyRequired = this.getEnergyRequired((Integer) this.outputVars[0]);
				if (this.energyStorage.getEnergyStored() >= energyRequired) {
					this.currentTimeMax = this.getAdjustedTime(
							(Integer) this.outputVars[2] * 20
					); // times 20 for ticks
					this.timeUntilNextDecompose = 0;
				}
			}
			else if (this.currentTimeMax > -1) {
				if (this.timeUntilNextDecompose >= this.currentTimeMax) {
					int energyRequired = this.getEnergyRequired((Integer) this.outputVars[0]);
					if (this.energyStorage.getEnergyStored() >= energyRequired) {
						AspectList aspectList = (AspectList) this.outputVars[1];
						Aspect[] aspects = aspectList.getAspects();
						for (int i = 0; i < aspects.length; i++) {
							this.aspects.add(aspects[i], aspectList.getAmount(aspects[i]));
						}
						// decrement the current stack
						if (Scanner.consumeItems &&
								!this.augmentList.contains(EnumDecomposerAugment.ITEM_KEEPER)) {
							this.decrStackSize(0, 1);
						}
						// decrease the energy
						this.energyStorage.extractEnergy(energyRequired, false);
					}
					this.outputVars = null;
					this.resetTimer();
				}
				else if (this.timeUntilNextDecompose >= 0) {
					this.timeUntilNextDecompose += 1;
				}
			}
		}
		else {
			if (this.timeUntilNextDecompose > -1) {
				this.resetTimer();
			}
		}
	}

	public boolean canOperate() {
		return this.getControl() == ControlMode.DISABLED ||
				(this.getControl() == ControlMode.LOW && !this.isPowered()) ||
				(this.getControl() == ControlMode.HIGH && this.isPowered());
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
			int tier = Scanner.aspectTiers.containsKey(aspect) ?
					Scanner.aspectTiers.get(aspect) :
					3;
			int[] stats = Scanner.decompositionStats.get(tier);
			energy += stats[0];
			time += stats[1];
		}
		return new Object[] { energy, aspectList, time };
	}

	private AspectList filterAspects(AspectList aspectList) {
		AspectList resultList = new AspectList();
		Random rand = this.getWorldObj().rand;
		int machineTier = this.getBlock().getTier(this.getBlockMetadata());
		Aspect[] aspects = aspectList.getAspectsSorted();
		double genericChance = this.getMachineChance(machineTier);
		for (Aspect aspect : aspects) {
			int amount = aspectList.getAmount(aspect);
			int complexity = Scanner.aspectTiers.containsKey(aspect) ?
					Scanner.aspectTiers.get(aspect) :
					3;
			double complexityChance = this.getComplexityChance(machineTier, complexity);
			int resultAmount = 0;
			for (int each = 1; each <= amount; each++) {
				if (rand.nextDouble() < complexityChance && rand.nextDouble() < genericChance) {
					resultAmount++;
				}
			}
			if (resultAmount > 0)
				resultList.add(aspect, resultAmount);
		}
		return resultList;
	}

	private double getComplexityChance(int machineTier, int aspectComplexity) {
		double base = Scanner.complexityTierChance[machineTier][aspectComplexity - 1];
		for (EnumDecomposerAugment augment : this.augmentList) {
			base *= augment.getOutputMultipliers()[aspectComplexity];
		}
		return base;
	}

	private double getMachineChance(int machineTier) {
		double base = Scanner.tieredChance[machineTier];
		for (EnumDecomposerAugment augment : this.augmentList) {
			base *= augment.getOutputMultipliers()[1];
		}
		return base;
	}

	private void resetTimer() {
		this.currentTimeMax = this.timeUntilNextDecompose = -1;
	}

	private int getAdjustedTime(int baseTimeInTicks) {
		for (EnumDecomposerAugment augment : this.augmentList) {
			baseTimeInTicks *= augment.getTimeMultiplier();
		}
		return baseTimeInTicks;
	}

	private int getEnergyRequired(int baseRequirement) {
		for (EnumDecomposerAugment augment : this.augmentList) {
			baseRequirement *= augment.getEnergyRequirementMultiplier();
		}
		return baseRequirement;
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
					EnumDecomposerSide.NONE.getIcon(side);
		return icons;
	}

	public BlockDecomposer getBlock() {
		return (BlockDecomposer) this.getBlockType();
	}

	public boolean isProcessing() {
		return this.timeUntilNextDecompose > -1;
	}

	public int getProgress() {
		return (int) (
				((double) this.timeUntilNextDecompose / (double) this.currentTimeMax)
						* TEDecomposer.hexagonProgressSteps
		);
	}

	public int getColumnOffset() {
		return this.currentColumnOffset;
	}

	public void giveResearch(EntityPlayerMP player, int aspectIndex, boolean takeAllVsOne) {
		Aspect[] aspects = this.aspects.getAspectsSorted();
		if (aspectIndex < aspects.length) {
			Aspect aspect = aspects[aspectIndex];
			if (aspect == null)
				this.aspects.remove(null);
			else {
				int currentAmount = this.aspects.getAmount(aspect);
				if (takeAllVsOne || currentAmount <= 1)
					this.aspects.remove(aspect);
				else
					this.aspects.reduce(aspect, 1);
				this.addAspect(player, aspect, (short) (takeAllVsOne ? currentAmount : 1));
			}
		}
	}

	private void addAspect(EntityPlayerMP player, Aspect aspect, short amount) {
		Thaumcraft.proxy.playerKnowledge.addAspectPool(
				player.getCommandSenderName(), aspect, amount
		);
		ResearchManager.scheduleSave(player);
		thaumcraft.common.lib.network.PacketHandler.INSTANCE.sendTo(
				new PacketAspectPool(
						aspect.getTag(), amount,
						Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(
								player.getCommandSenderName(), aspect
						)
				), player
		);
	}

	// ~~~~~~~~~~~~~~~ Below is background functionality, Tinker at your own risk ~~~~~~~~~~~~~~~

	private boolean isValidSlot(int slot) {
		return 0 <= slot && slot < this.stacks.length;
	}

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
		return this.isValidSlot(slot) ? this.stacks[slot] : null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		return this.getStackInSlot(slot);
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (this.isValidSlot(slot)) {
			if (stack == null)
				this.stacks[slot] = null;
			else if (this.isItemValidForSlot(slot, stack))
				this.stacks[slot] = stack.copy();
		}
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
		if (itemStack == null)
			return true;
		if (this.isValidSlot(slot)) {
			AspectList list = ThaumcraftApiHelper.getObjectAspects(itemStack);
			return list != null && list.size() > 0;
		}
		return false;
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		ItemStack stack = null;
		if (this.isValidSlot(slot) && this.stacks[slot] != null) {
			if (this.stacks[slot].stackSize <= amount) {
				amount = this.stacks[slot].stackSize;
			}
			stack = this.stacks[slot].splitStack(amount);
			if (this.stacks[slot].stackSize <= 0) {
				this.stacks[slot] = null;
			}
		}
		this.markDirty();
		return stack;
	}

	private boolean canInput_Item(int side) {
		return this.sideTypes[side] == EnumDecomposerSide.INPUT_ITEM ||
				this.sideTypes[side] == EnumDecomposerSide.IO;
	}

	private boolean canOutput_Item(int side) {
		return this.sideTypes[side] == EnumDecomposerSide.IO ||
				this.sideTypes[side] == EnumDecomposerSide.OUTPUT ||
				this.sideTypes[side] == EnumDecomposerSide.OUTPUT_ITEM;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return side == 0 ?
				new int[] { 1 } :
				new int[] { 0 }; // bottom can only access the capacitor slot
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return this.isValidSlot(slot) && this.canInput_Item(side) && this
				.isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return this.isValidSlot(slot) && this.canOutput_Item(side);
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
		int ret = this.energyStorage.receiveEnergy(maxReceive, simulate);
		this.markDirty();
		return ret;
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
	 * Ran when slots are set in container
	 */
	@Override
	public void installAugments() {
		int augmentPreSize = this.augmentList.size();
		this.augmentList.clear();
		for (int slot = 0; slot < this.augments.length; slot++) {
			if (this.augments[slot] != null) {
				//FMLLog.info(this.augments[slot].getDisplayName());
				FMLLog.info(Scanner.getFullName(this.augments[slot]));
				EnumDecomposerAugment augmentEnum = EnumDecomposerAugment.getByName(
						Scanner.getFullName(this.augments[slot])
				);
				if (augmentEnum != null)
					this.augmentList.add(augmentEnum);
			}
		}
		if (augmentPreSize != this.augmentList.size())
			// if you change the augments, you will reset the progress
			this.resetTimer();
	}

	@Override
	public boolean[] getAugmentStatus() {
		return new boolean[0];
	}

	@Override
	public boolean canRotate() {
		return true;
	}

	@Override
	public boolean canRotate(ForgeDirection dir) {
		return dir != ForgeDirection.DOWN && dir != ForgeDirection.UP;
	}

	@Override
	public void rotate(ForgeDirection dir) {
		this.rotateBlock();
	}

	@Override
	public void rotateDirectlyTo(int i) {
		this.setFacing(i);
	}

	@Override
	public ForgeDirection getDirectionFacing() {
		return ForgeDirection.getOrientation(this.getFacing());
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
			this.sync();
			return true;
		}
		return false;
	}

	@Override
	public boolean resetSides() {
		for (int side = 0; side < this.sideTypes.length; side++)
			this.sideTypes[side] = EnumDecomposerSide.NONE;
		return true;
	}

	@Override
	public int getNumConfig(int side) {
		return !this.isSideFront(side) ? EnumDecomposerSide.values().length : 0;
	}

	@Override
	public IIcon getTexture(int side, int pass) {
		return !this.isSideFront(side) ?
				this.sideTypes[side].getIcon(side) :
				this.getBlock().getIcon(side, this.getBlockMetadata());
	}

	@Override
	public void setControl(ControlMode controlMode) {
		this.currentControl = controlMode;
		this.sync();
	}

	@Override
	public ControlMode getControl() {
		return this.currentControl;
	}

	@Override
	public void setPowered(boolean b) {
		this.powered = b;
	}

	@Override
	public boolean isPowered() {
		return this.powered;
	}

	// Thaumcraft things

	@Override
	public AspectList getAspects() {
		return this.aspects;
	}

	@Override
	public void setAspects(AspectList newList) {
		this.aspects = newList;
	}

	@Override
	public boolean doesContainerAccept(Aspect aspect) {
		return this.aspects.aspects.containsKey(aspect);
	}

	@Override
	public int addToContainer(Aspect aspect, int amount) {
		this.aspects.add(aspect, amount);
		return amount;
	}

	@Override
	public boolean takeFromContainer(Aspect aspect, int amount) {
		boolean ret = this.aspects.reduce(aspect, amount);
		return ret;
	}

	@Override
	public boolean takeFromContainer(AspectList aspectList) {
		AspectList temp = this.aspects.copy();
		boolean successful = true;
		for (Map.Entry<Aspect, Integer> entry : aspectList.aspects.entrySet()) {
			if (!temp.reduce(entry.getKey(), entry.getValue())) {
				successful = false;
				break;
			}
		}
		if (successful)
			this.setAspects(temp);
		return successful;
	}

	@Override
	public boolean doesContainerContainAmount(Aspect aspect, int amount) {
		return this.containerContains(aspect) == amount;
	}

	@Override
	public boolean doesContainerContain(AspectList aspectList) {
		for (Map.Entry<Aspect, Integer> entry : aspectList.aspects.entrySet()) {
			if (!this.doesContainerContainAmount(entry.getKey(), entry.getValue()))
				return false;
		}
		return true;
	}

	@Override
	public int containerContains(Aspect aspect) {
		return this.aspects.getAmount(aspect);
	}

	@Override
	public boolean isConnectable(ForgeDirection dir) {
		return this.canOutputTo(dir);
	}

	@Override
	public boolean canInputFrom(ForgeDirection dir) {
		return false;
	}

	@Override
	public boolean canOutputTo(ForgeDirection dir) {
		EnumDecomposerSide side = this.sideTypes[dir.ordinal()];
		return side == EnumDecomposerSide.IO
				|| side == EnumDecomposerSide.OUTPUT
				|| side == EnumDecomposerSide.OUTPUT_THAUM
				|| side == EnumDecomposerSide.OUTPUT_ESSENTIA;
	}

	@Override
	public void setSuction(Aspect paramAspect, int paramInt) {
	}

	@Override
	public Aspect getSuctionType(ForgeDirection dir) {
		return null;
	}

	@Override
	public int getSuctionAmount(ForgeDirection dir) {
		return 0;
	}

	@Override
	public int takeEssentia(Aspect aspect, int amount, ForgeDirection dir) {
		return this.canOutputTo(dir) && this.takeFromContainer(aspect, amount) ? amount : 0;
	}

	@Override
	public int addEssentia(Aspect paramAspect, int paramInt, ForgeDirection dir) {
		return 0;
	}

	@Override
	public Aspect getEssentiaType(ForgeDirection dir) {
		if (this.canOutputTo(dir) && this.aspects.size() > 0) {
			return this.aspects.getAspects()[0];
		}
		return null;
	}

	@Override
	public int getEssentiaAmount(ForgeDirection dir) {
		if (this.canOutputTo(dir) && this.aspects.size() > 0) {
			return this.aspects.getAmount(this.getEssentiaType(dir));
		}
		return 0;
	}

	@Override
	public int getMinimumSuction() {
		return 0;
	}

	@Override
	public boolean renderExtendedTube() {
		return true;
	}

	// data

	public void sendGuiNetworkData(Container container, ICrafting player) {
	}

	public void receiveGuiNetworkData(int i, int j) {
	}

	private void sync() {
		PacketHandler.sendToServer(new PacketTileSync(this));
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
				stackTag.setInteger("slot", (byte) slot);
				stackTags.appendTag(stackTag);
			}
		}
		tagCom.setTag("stackTags", stackTags);

		tagCom.setInteger("augmentSize", this.augments.length);
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

		tagCom.setInteger("currentTimeMax", this.currentTimeMax);
		tagCom.setInteger("timeToDecompose", this.timeUntilNextDecompose);

		for (int side = 0; side < this.sideTypes.length; side++)
			if (this.sideTypes[side] != null)
				tagCom.setInteger("side" + side, this.sideTypes[side].ordinal());

		NBTTagList augmentList = new NBTTagList();
		for (EnumDecomposerAugment augment : this.augmentList) {
			NBTTagCompound augmentTag = new NBTTagCompound();
			augmentTag.setString("name", augment.getStackName());
			augmentList.appendTag(augmentTag);
		}
		tagCom.setTag("augmentList", augmentList);

		this.aspects.writeToNBT(tagCom, "aspects");

		tagCom.setInteger("controlMode", this.currentControl.ordinal());
		tagCom.setBoolean("powered", this.powered);

		if (this.outputVars != null) {
			tagCom.setBoolean("hasOutput", true);
			tagCom.setInteger("outputEnergy", (Integer) this.outputVars[0]);
			((AspectList) this.outputVars[1]).writeToNBT(tagCom, "outputList");
			tagCom.setInteger("outputTime", (Integer) this.outputVars[2]);
		}

	}

	@Override
	public void readFromNBT(NBTTagCompound tagCom) {
		super.readFromNBT(tagCom);

		NBTTagList stackTags = tagCom.getTagList("stackTags", 10);
		for (int i = 0; i < stackTags.tagCount(); i++) {
			NBTTagCompound stackTag = stackTags.getCompoundTagAt(i);
			int slot = stackTag.getInteger("slot") & 255;
			this.stacks[slot] = ItemStack.loadItemStackFromNBT(stackTag);
		}

		this.augments = new ItemStack[tagCom.getInteger("augmentSize")];
		NBTTagList augmentTags = tagCom.getTagList("augmentTags", 10);
		for (int i = 0; i < augmentTags.tagCount(); i++) {
			NBTTagCompound augmentTag = augmentTags.getCompoundTagAt(i);
			int slot = augmentTag.getInteger("slot");
			this.augments[slot] = ItemStack.loadItemStackFromNBT(augmentTag);
		}

		this.energyStorage.readFromNBT(tagCom);

		this.currentTimeMax = tagCom.getInteger("currentTimeMax");
		this.timeUntilNextDecompose = tagCom.getInteger("timeToDecompose");

		for (int side = 0; side < 6; side++) {
			this.sideTypes[side] = EnumDecomposerSide.values()
					[tagCom.getInteger("side" + side)];
		}

		this.augmentList.clear();
		NBTTagList augmentList = tagCom.getTagList("augmentList", 10);
		for (int i = 0; i < augmentList.tagCount(); i++) {
			this.augmentList.add(EnumDecomposerAugment.getByName(
					augmentList.getCompoundTagAt(i).getString("name")
			));
		}

		this.aspects.readFromNBT(tagCom, "aspects");

		this.currentControl = ControlMode.values()[tagCom.getInteger("controlMode")];
		this.powered = tagCom.getBoolean("powered");

		if (tagCom.getBoolean("hasOutput")) {
			this.outputVars = new Object[3];
			this.outputVars[0] = tagCom.getInteger("outputEnergy");
			AspectList output = new AspectList();
			output.readFromNBT(tagCom, "outputList");
			this.outputVars[1] = output;
			this.outputVars[2] = tagCom.getInteger("outputTime");
		}

	}

}
