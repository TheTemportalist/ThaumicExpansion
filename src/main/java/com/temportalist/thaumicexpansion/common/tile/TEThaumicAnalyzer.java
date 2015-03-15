package com.temportalist.thaumicexpansion.common.tile;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.energy.IEnergyStorage;
import cofh.api.tileentity.*;
import cofh.api.transport.IItemDuct;
import cofh.core.network.ITileInfoPacketHandler;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.lib.util.helpers.EnergyHelper;
import cofh.lib.util.position.IRotateableTile;
import com.temportalist.thaumicexpansion.common.TEC;
import com.temportalist.thaumicexpansion.common.block.BlockThaumicAnalyzer;
import com.temportalist.thaumicexpansion.common.lib.*;
import com.temportalist.thaumicexpansion.common.packet.PacketTileSync;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.playerdata.PacketAspectPool;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ScanManager;

import java.util.*;

/**
 * @author TheTemportalist
 */
public class TEThaumicAnalyzer extends TileEntity implements ISidedInventory,
		IOperator,
		ITileInfoPacketHandler,
		IEnergyReceiver, IEnergyConnection,
		IAugmentable, IRedstoneControl,
		IRotateableTile, IReconfigurableFacing, IReconfigurableSides, ISidedTexture,
		IAspectContainer, IEssentiaTransport {

	public static final int hexagonProgressSteps = 13;

	public final int INPUT_MAIN = 0, INPUT_CAPACITOR = 1, OUTPUT_MAIN = 2;

	protected EnergyStorage energyStorage = new EnergyStorage(TEC.maxEnergyStorage);
	private ItemStack[] stacks = new ItemStack[3];
	private ItemStack[] augments = new ItemStack[3];
	private HashMap<EnumAugmentTA, Integer> augmentList = new HashMap<EnumAugmentTA, Integer>();
	private AspectList aspects = new AspectList();
	private int currentColumnOffset = 0;

	private IOperation currentOperation = null;

	EnumSideTA[] sideTypes = new EnumSideTA[6];
	ControlMode currentControl = ControlMode.DISABLED;
	boolean powered = false;

	public void updateForTier(int tier) {
		this.energyStorage.setCapacity(TEC.maxEnergyStorage * (tier + 1));
		this.augments = new ItemStack[tier + 3];
	}

	@Override
	public void updateEntity() {
		if (this.augmentList.containsKey(EnumAugmentTA.DECOMPOSER)) {
			if (this.currentOperation instanceof OperationAnalyzer)
				this.currentOperation = null;
			this.decomposerUpdate();
		}
		else {
			if (this.currentOperation instanceof OperationDecomposer)
				this.currentOperation = null;
			this.analyzerUpdate();
		}
	}

	private void analyzerUpdate() {
		// todo time energy of operation
		if (this.currentOperation == null) {
			this.currentOperation = new OperationAnalyzer(
					this.getAugmentedTime(10),
					this.getAugmentedEnergy(20)
			);
			this.currentOperation.updateAugments(this, this.augmentList.keySet());
		}
		if (this.canOperate() && this.getInput() != null) {
			if (!this.currentOperation.isRunning() &&
					this.currentOperation.canRun(this, this)) {
				this.currentOperation.start();
			}
			else {
				if (!this.currentOperation.canRun(this, this)) {
					this.currentOperation.reset();
				}
				else {
					this.currentOperation.tick();
					if (this.currentOperation.areTicksReady()) {
						if (!this.getWorldObj().isRemote) {
							this.currentOperation.run(this, this);

							int[] costs = (int[]) this.currentOperation.getCosts();
							this.energyStorage.extractEnergy(costs[0], false);
						}
						this.currentOperation = null;
						this.markDirty();
						PacketHandler.sendToAll(new PacketTileSync(this)); // todo narrow syncing
					}
				}
			}
		}
		else if (this.currentOperation.isRunning())
			this.currentOperation.reset();
	}

	private void decomposerUpdate() {
		if (this.currentOperation == null) {
			this.currentOperation = new OperationDecomposer(
					this.getBlock().getTier(this.getBlockMetadata()), this
			);
		}
		if (this.canOperate() && this.getInput() != null) {
			if (!this.currentOperation.isRunning() &&
					this.currentOperation.canRun(this, this)) {
				this.currentOperation.updateAugments(this, this.augmentList.keySet());
				this.currentOperation.start();
			}
			else {
				if (!this.currentOperation.canRun(this, this)) {
					this.currentOperation.reset();
				}
				else {
					this.currentOperation.tick();
					if (this.currentOperation.areTicksReady()) {
						if (!this.getWorldObj().isRemote) {
							this.currentOperation.run(this, this);

							int[] costs = (int[]) this.currentOperation.getCosts();
							this.energyStorage.extractEnergy(costs[0], false);
						}
						this.currentOperation = null;
						this.markDirty();
						PacketHandler.sendToAll(new PacketTileSync(this)); // todo narrow syncing
					}
				}
			}
		}
		else if (this.currentOperation.isRunning())
			this.currentOperation.reset();
	}

	public boolean canOperate() {
		return this.getControl() == ControlMode.DISABLED ||
				(this.getControl() == ControlMode.LOW && !this.isPowered()) ||
				(this.getControl() == ControlMode.HIGH && this.isPowered());
	}

	@Override
	public ItemStack getInput() {
		return this.stacks[this.INPUT_MAIN];
	}

	@Override
	public ItemStack getOutput() {
		return this.stacks[this.OUTPUT_MAIN];
	}

	@Override
	public void finishedOperation(ItemStack setInput, ItemStack toOutput) {
		this.stacks[this.INPUT_MAIN] = setInput;
		if (this.stacks[this.OUTPUT_MAIN] == null)
			this.stacks[this.OUTPUT_MAIN] = toOutput;
		else if (toOutput != null)
			this.stacks[this.OUTPUT_MAIN].stackSize += toOutput.stackSize;
		this.markDirty();
		System.out.println("finished");
	}

	public int getAugmentedTime(int baseTimeInTicks) {
		for (EnumAugmentTA augment : this.augmentList.keySet()) {
			baseTimeInTicks *= augment.getTimeMultiplier();
		}
		return baseTimeInTicks;
	}

	public int getAugmentedEnergy(int baseRequirement) {
		for (EnumAugmentTA augment : this.augmentList.keySet()) {
			baseRequirement *= augment.getEnergyRequirementMultiplier();
		}
		return baseRequirement;
	}

	public double getSecondaryChance() {
		double chance = 1d;
		for (EnumAugmentTA augment : this.augmentList.keySet()) {
			chance += augment.getOutputMultipliers()[0];
		}
		return chance;
	}

	@SideOnly(Side.CLIENT)
	public IIcon[] getIcons(ForgeDirection dir) {
		int side = dir.ordinal();
		IIcon[] icons = new IIcon[2];
		icons[0] = this.getBlock().getIcon(side, this.getBlockMetadata());
		if (dir.ordinal() != this.getBlock().getRotation(this.getBlockMetadata()))
			icons[1] = this.sideTypes[side] != null ?
					this.sideTypes[side].getIcon(side, false) :
					EnumSideTA.NONE.getIcon(side, false);
		return icons;
	}

	public BlockThaumicAnalyzer getBlock() {
		return (BlockThaumicAnalyzer) this.getBlockType();
	}

	public boolean isProcessing() {
		return this.currentOperation != null && this.currentOperation.isRunning();
	}

	public int getProgress() {
		return this.currentOperation != null ? (int) (
				this.currentOperation.getProgress() * TEThaumicAnalyzer.hexagonProgressSteps
		) : 0;
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

	public boolean isConnected(ForgeDirection dir) {
		TileEntity tile = this.getWorldObj().getTileEntity(
				this.xCoord + dir.offsetX,
				this.yCoord + dir.offsetY,
				this.zCoord + dir.offsetZ
		);
		return tile != null && (
				(tile instanceof IInventory) || (
						tile instanceof IEssentiaTransport &&
								((IEssentiaTransport) tile).renderExtendedTube()
				) || (
						tile instanceof IEnergyConnection &&
								((IEnergyConnection) tile).canConnectEnergy(dir.getOpposite())
				) || (tile instanceof IItemDuct)
		);
	}

	public UUID getCurrentPlayerUUID() {
		ItemStack playerTracker = this.getAugment(EnumAugmentTA.PLAYER_TRACKER);
		//System.out.println("getting player");
		if (playerTracker != null) {
			//System.out.println("non null player stack");
			NBTTagCompound tag = playerTracker.getTagCompound();
			if (tag != null) {
				//System.out.println("non null tag");
				try {
					//System.out.println("fetching tag");
					return UUID.fromString(tag.getString("playerUUID"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public String getCurrentPlayerName() {
		return TEC.idToUsername.get(this.getCurrentPlayerUUID());
	}

	public void setAugments(ItemStack[] augments) {
		for (int i = 0; i < augments.length; i++) {
			if (i < this.augments.length) {
				this.augments[i] = augments[i];
			}
		}
		this.installAugments();
	}

	public ItemStack getAugment(EnumAugmentTA augment) {
		return this.augmentList.containsKey(augment) ?
				this.augments[this.augmentList.get(augment)] :
				null;
	}

	public void addAspects(AspectList aspectList) {
		this.aspects.add(aspectList);
	}

	public void setPlacedBy(EntityPlayer player) {
		//System.out.println("Placed");
		//System.out.println(this.augmentList.containsKey(EnumAugmentTA.PLAYER_TRACKER));
		ItemStack playerTracker = this.getAugment(EnumAugmentTA.PLAYER_TRACKER);
		if (playerTracker != null) {
			NBTTagCompound stackTag = playerTracker.hasTagCompound() ?
					playerTracker.getTagCompound() :
					new NBTTagCompound();
			stackTag.setString("playerUUID", player.getGameProfile().getId().toString());
			playerTracker.setTagCompound(stackTag);
			//System.out.println("Set ID");
		}
	}

	public ScanResult getScan(ItemStack stack) {
		return new ScanResult(
				(byte) 1, Item.getIdFromItem(stack.getItem()), stack.getItemDamage(),
				null, ""
		);
	}

	public boolean canScan(ItemStack stack) {
		AspectList list = ThaumcraftApiHelper.getObjectAspects(stack);
		if (list != null && list.size() > 0) {
			if (this.augmentList.containsKey(EnumAugmentTA.DECOMPOSER))
				return true;
			else {
				String pName = this.getCurrentPlayerName();
				if (pName != null) {
					PlayerKnowledge pk = Thaumcraft.proxy.getPlayerKnowledge();

					for (Aspect aspect : list.getAspects()) {
						if (aspect != null && !aspect.isPrimal()
								&& !pk.hasDiscoveredParentAspects(pName, aspect))
							return false;
					}
					return this.isValidScanTarget(pName, this.getScan(stack), "@");
				}
			}
		}
		return false;
	}

	public boolean isValidScanTarget(String pName, ScanResult scan, String prefix) {
		if (scan == null)
			return false;

		if ((prefix.equals("@")) && (!isValidScanTarget(pName, scan, "#")))
			return false;

		if (scan.type == 1) {
			List arg = Arrays.asList(Item.getItemById(scan.id), scan.meta);
			if (ThaumcraftApi.groupedObjectTags.containsKey(arg)) {
				scan.meta = ThaumcraftApi.groupedObjectTags.get(arg)[0];
			}
			List list = (List) Thaumcraft.proxy.getScannedObjects().get(pName);
			if (list != null && list.contains(prefix + ScanManager.generateItemHash(
					Item.getItemById(scan.id),
					scan.meta
			))) {
				return false;
			}
		}
		else if (scan.type == 2) {
			if ((scan.entity instanceof EntityItem)) {
				EntityItem item = (EntityItem) scan.entity;
				ItemStack t = item.getEntityItem().copy();
				t.stackSize = 1;
				List arg = Arrays.asList(t.getItem(), t.getItemDamage());
				if (ThaumcraftApi.groupedObjectTags.containsKey(arg)) {
					t.setItemDamage(ThaumcraftApi.groupedObjectTags.get(arg)[0]);
				}
				List list = (List) Thaumcraft.proxy.getScannedObjects().get(pName);
				if (list != null && list.contains(
						prefix + ScanManager.generateItemHash(t.getItem(), t.getItemDamage())
				))
					return false;
			}
			else {
				List list = (List) Thaumcraft.proxy.getScannedEntities().get(pName);
				if (list != null) {
					try {
						String hash = (String) ScanManager.class.getDeclaredMethod(
								"generateEntityHash", Entity.class
						).invoke(null, scan.entity);
						if (list.contains(prefix + hash))
							return false;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		else if (scan.type == 3) {
			List list = (List) Thaumcraft.proxy.getScannedPhenomena().get(pName);
			if ((list != null) && (list.contains(prefix + scan.phenomena))) {
				return false;
			}
		}
		return true;
	}

	// ~~~~~~~~~~~~~~~ Below is background functionality, Tinker at your own risk ~~~~~~~~~~~~~~~

	private boolean isValidSlot(int slot) {
		return 0 <= slot && slot < this.stacks.length;
	}

	public void dropAllInventory(World world, int x, int y, int z) {
		for (ItemStack stack : this.stacks)
			if (stack != null)
				this.dropStack(world, x, y, z, stack);
		for (ItemStack stack : this.augments)
			if (stack != null)
				this.dropStack(world, x, y, z, stack);
		// todo essentia
	}

	private void dropStack(World world, int x, int y, int z, ItemStack stack) {
		float xVar = world.rand.nextFloat() * 0.8F + 0.1F;
		float yVar = world.rand.nextFloat() * 0.8F + 0.1F;
		EntityItem entityitem;

		for (float zVar = world.rand.nextFloat() * 0.8F + 0.1F;
			 stack.stackSize > 0;
			 world.spawnEntityInWorld(entityitem)) {
			int stackSplitVar = world.rand.nextInt(21) + 10;

			if (stackSplitVar > stack.stackSize) {
				stackSplitVar = stack.stackSize;
			}

			stack.stackSize -= stackSplitVar;
			entityitem = new EntityItem(
					world, (double) ((float) x + xVar),
					(double) ((float) y + yVar),
					(double) ((float) z + zVar),
					new ItemStack(stack.getItem(), stackSplitVar, stack.getItemDamage())
			);
			float motionVar = 0.05F;
			entityitem.motionX = (double) ((float) world.rand.nextGaussian() * motionVar);
			entityitem.motionY = (double) ((float) world.rand.nextGaussian() * motionVar + 0.2F);
			entityitem.motionZ = (double) ((float) world.rand.nextGaussian() * motionVar);

			if (stack.hasTagCompound()) {
				entityitem.getEntityItem().setTagCompound(
						(NBTTagCompound) stack.getTagCompound().copy()
				);
			}
		}
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
			else
				this.stacks[slot] = stack.copy();
			//this.markDirty();
		}
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
		if (itemStack == null || slot == this.OUTPUT_MAIN)
			return true;
		if (this.isValidSlot(slot)) {
			if (slot == this.INPUT_MAIN) {
				return this.canScan(itemStack);
			}
			else if (slot == this.INPUT_CAPACITOR) {
				return EnergyHelper.isEnergyContainerItem(itemStack);
			}
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
		return this.sideTypes[side] == EnumSideTA.INPUT_ITEM ||
				this.sideTypes[side] == EnumSideTA.IO;
	}

	private boolean canOutput_Item(int side) {
		return this.sideTypes[side] == EnumSideTA.IO ||
				this.sideTypes[side] == EnumSideTA.OUTPUT ||
				this.sideTypes[side] == EnumSideTA.OUTPUT_ITEM;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		if (this.sideTypes[side] == EnumSideTA.IO)
			return new int[] { this.INPUT_MAIN, this.INPUT_CAPACITOR, this.OUTPUT_MAIN };
		else if (this.canInput_Item(side))
			return new int[] { this.INPUT_MAIN, this.INPUT_CAPACITOR };
		else if (this.canOutput_Item(side))
			return new int[] { this.OUTPUT_MAIN };
		else
			return new int[0];
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return this.isValidSlot(slot) && this.canInput_Item(side)
				&& this.isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return this.isValidSlot(slot) && slot == this.OUTPUT_MAIN && this.canOutput_Item(side);
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
				//System.out.println(slot + "-> " + TEC.getFullName(this.augments[slot]));
				EnumAugmentTA augmentEnum = EnumAugmentTA.getByName(
						TEC.getFullName(this.augments[slot])
				);
				//System.out.println("Enum: " + augmentEnum);
				if (augmentEnum != null)
					this.augmentList.put(augmentEnum, slot);
			}
		}
		if (augmentPreSize != this.augmentList.size() && this.currentOperation != null) {
			// if you change the augments, you will reset the progress
			this.currentOperation.reset();
			this.currentOperation.updateAugments(this, this.augmentList.keySet());
		}
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
			this.sideTypes[side] = EnumSideTA.values()[index];
			PacketHandler.sendToServer(
					new PacketTileInfo(this).
							addString("SIDE").addInt(side).addInt(this.sideTypes[side].ordinal())
			);
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			return true;
		}
		return false;
	}

	@Override
	public boolean resetSides() {
		for (int side = 0; side < this.sideTypes.length; side++)
			this.sideTypes[side] = EnumSideTA.NONE;
		return true;
	}

	@Override
	public int getNumConfig(int side) {
		return !this.isSideFront(side) ? EnumSideTA.values().length : 0;
	}

	@Override
	public IIcon getTexture(int side, int pass) {
		if (pass > 0) {
			//System.out.println(side + ": " + this.sideTypes[side] + ":" + sideTex);
			return this.sideTypes[side].getIcon(
					side, side == this.getBlock().getRotation(this.getBlockMetadata())
			);
		}
		return this.getBlock().getIcon(side, this.getBlockMetadata());
	}

	@Override
	public void setControl(ControlMode controlMode) {
		this.currentControl = controlMode;
		PacketHandler.sendToServer(
				new PacketTileInfo(this).
						addString("CONTROLMODE").addInt(this.currentControl.ordinal())
		);
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
		this.markDirty();
	}

	@Override
	public boolean doesContainerAccept(Aspect aspect) {
		return false;
	}

	@Override
	public int addToContainer(Aspect aspect, int amount) {
		this.aspects.add(aspect, amount);
		this.markDirty();
		return amount;
	}

	@Override
	public boolean takeFromContainer(Aspect aspect, int amount) {
		int contained = this.aspects.getAmount(aspect);
		if (contained < amount)
			return false;
		else if (contained > amount)
			this.aspects.reduce(aspect, amount);
		else if (contained == amount)
			this.aspects.remove(aspect);
		this.markDirty();
		PacketHandler.sendToAll(new PacketTileSync(this)); // todo
		return true;
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
		if (dir == ForgeDirection.UNKNOWN)
			return true;
		EnumSideTA side = this.sideTypes[dir.ordinal()];
		return side == EnumSideTA.IO
				|| side == EnumSideTA.OUTPUT
				|| side == EnumSideTA.OUTPUT_THAUM
				|| side == EnumSideTA.OUTPUT_ESSENTIA;
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
		PacketHandler.sendToAll(
				new PacketTileInfo(this).
						addString("ENERGY").
						addInt(this.energyStorage.getEnergyStored())
		);
	}

	public void receiveGuiNetworkData(int i, int j) {
	}

	@Override
	public void handleTileInfoPacket(PacketCoFHBase packet, boolean isServer, EntityPlayer player) {
		String type = packet.getString();
		if (type.equals("ENERGY")) {
			this.energyStorage.setEnergyStored(packet.getInt());
		}
		else if (type.equals("ADDASPECT")) {
			int index = packet.getInt();
			boolean takeSingleVAll = packet.getBool();
			Aspect[] aspects = this.aspects.getAspectsSorted();
			//for (int i = 0; i < aspects.length; i++)
			//	System.out.println(i + " : " + aspects[i].getName());
			if (index < aspects.length) {
				Aspect aspect = aspects[index];
				int amount = this.aspects.getAmount(aspect);
				if (takeSingleVAll && amount > 1)
					this.aspects.reduce(aspect, 1);
				else
					this.aspects.remove(aspect);
				if (player instanceof EntityPlayerMP) {
					this.addAspect(
							(EntityPlayerMP) player, aspect, (short) (takeSingleVAll ? 1 : amount)
					);
				}
			}
		}
		else if (type.equals("COLUMN")) {
			int nextCol = this.currentColumnOffset + packet.getInt();
			if (nextCol >= 0 && nextCol < this.aspects.size() / 4) {
				this.currentColumnOffset = nextCol;
			}
		}
		else if (type.equals("CONTROLMODE")) {
			this.currentControl = ControlMode.values()[packet.getInt()];
		}
		else if (type.equals("SIDE")) {
			this.sideTypes[packet.getInt()] = EnumSideTA.values()[packet.getInt()];
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		}
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

		if (this.currentOperation != null) {
			tagCom.setString("operationClass", this.currentOperation.getClass().getCanonicalName());
			this.currentOperation.writeTo(tagCom, "operation");
		}

		for (int side = 0; side < this.sideTypes.length; side++)
			if (this.sideTypes[side] != null)
				tagCom.setInteger("side" + side, this.sideTypes[side].ordinal());

		NBTTagList augmentList = new NBTTagList();
		for (EnumAugmentTA augment : this.augmentList.keySet()) {
			NBTTagCompound augmentTag = new NBTTagCompound();
			augmentTag.setString("name", augment.getStackName());
			augmentTag.setInteger("slot", this.augmentList.get(augment));
			augmentList.appendTag(augmentTag);
		}
		tagCom.setTag("augmentList", augmentList);

		this.aspects.writeToNBT(tagCom, "aspects");

		tagCom.setInteger("controlMode", this.currentControl.ordinal());
		tagCom.setBoolean("powered", this.powered);

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

		if (tagCom.hasKey("operationClass")) {
			try {
				IOperation obj = (IOperation) Class.forName(
						tagCom.getString("operationClass")
				).newInstance();
				obj.readFrom(tagCom, "operation");
			} catch (Exception e) {
			}
		}

		for (int side = 0; side < 6; side++) {
			this.sideTypes[side] = EnumSideTA.values()
					[tagCom.getInteger("side" + side)];
		}

		this.augmentList.clear();
		NBTTagList augmentList = tagCom.getTagList("augmentList", 10);
		for (int i = 0; i < augmentList.tagCount(); i++) {
			this.augmentList.put(
					EnumAugmentTA.getByName(
							augmentList.getCompoundTagAt(i).getString("name")
					),
					augmentList.getCompoundTagAt(i).getInteger("slot")
			);
		}

		this.aspects.readFromNBT(tagCom, "aspects");

		this.currentControl = ControlMode.values()[tagCom.getInteger("controlMode")];
		this.powered = tagCom.getBoolean("powered");

	}

}
