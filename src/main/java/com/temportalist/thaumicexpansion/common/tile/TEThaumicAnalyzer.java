package com.temportalist.thaumicexpansion.common.tile;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.tileentity.*;
import cofh.api.transport.IItemDuct;
import cofh.core.network.ITileInfoPacketHandler;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.EnergyHelper;
import cofh.lib.util.position.IRotateableTile;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import cofh.thermalexpansion.util.Utils;
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
public class TEThaumicAnalyzer extends TileMachineBase implements ISidedInventory,
		IOperator,
		ITileInfoPacketHandler,
		IEnergyReceiver, IEnergyConnection,
		IAugmentable, IRedstoneControl,
		IRotateableTile, IReconfigurableFacing, IReconfigurableSides, ISidedTexture,
		IAspectContainer, IEssentiaTransport {

	public static final int hexagonProgressSteps = 13;

	public final int INPUT_MAIN = 0, INPUT_CAPACITOR = 1, OUTPUT_MAIN = 2;

	//protected EnergyStorage energyStorage = new EnergyStorage(TEC.maxEnergyStorage);
	//private ItemStack[] stacks = new ItemStack[3];
	//private ItemStack[] augments = new ItemStack[3];
	private HashMap<EnumAugmentTA, Integer> augmentList = new HashMap<EnumAugmentTA, Integer>();
	private AspectList aspects = new AspectList();
	private int currentColumnOffset = 0;

	private IOperation currentOperation = null;

	EnumSideTA[] sideTypes = new EnumSideTA[6];

	public TEThaumicAnalyzer() {
		this.inventory = new ItemStack[3];

		this.sideConfig = new SideConfig();
		this.sideConfig.numGroup = EnumSideTA.values().length;
		this.sideConfig.slotGroups = EnumSideTA.getSlotGroups();
		this.sideConfig.allowInsertion = EnumSideTA.getInputs();
		this.sideConfig.allowExtraction = EnumSideTA.getOutputs();
		this.sideConfig.sideTex = new int[] { 0, 0, 0 };
		this.sideConfig.defaultSides = EnumSideTA.getDefaultSiding();

		this.energyConfig = new EnergyConfig();
		this.energyConfig.setParamsPower(20);
		this.energyStorage = new EnergyStorage(this.energyConfig.maxEnergy);

		this.setDefaultSides();

	}

	@Override
	public String getName() {
		return "Thaumic Analyzer";
	}

	@Override
	public int getType() {
		return BlockMachine.Types.FURNACE.ordinal();
	}

	private int getTier() {
		return this.getBlock().getTier(this.getBlockMetadata());
	}

	public void updateForTier(int tier) {
		this.energyStorage.setCapacity(TEC.maxEnergyStorage * (tier + 1));
		this.energyStorage.setMaxTransfer(this.energyConfig.maxPower * ENERGY_TRANSFER[tier]);
		this.augments = new ItemStack[AUGMENT_COUNT[tier]];
		this.augmentStatus = new boolean[this.augments.length];

	}

	@Override
	protected void onLevelChange() {
		if (this.worldObj != null)
			this.updateForTier(this.getTier());
	}

	@Override
	public void updateEntity() {
		this.chargeEnergy();
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
		this.checkOutput();
	}

	private void analyzerUpdate() {
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
						// todo narrow syncing: stacks, energy, operation
						PacketHandler.sendToAll(new PacketTileSync(this));
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
		return this.inventory[this.INPUT_MAIN];
	}

	@Override
	public ItemStack getOutput() {
		return this.inventory[this.OUTPUT_MAIN];
	}

	@Override
	public int getChargeSlot() {
		return this.INPUT_CAPACITOR;
	}

	@Override
	public void finishedOperation(ItemStack setInput, ItemStack toOutput) {
		System.out.println("finished"); // todo remove the print
		this.inventory[this.INPUT_MAIN] = setInput;
		if (this.inventory[this.OUTPUT_MAIN] == null)
			this.inventory[this.OUTPUT_MAIN] = toOutput;
		else if (toOutput != null)
			this.inventory[this.OUTPUT_MAIN].stackSize += toOutput.stackSize;
		this.markDirty();

	}

	public void checkOutput() {
		if (this.augmentAutoTransfer && this.getOutput() != null) {
			for (int side = 0; side < this.sideTypes.length; side++) {
				if (this.sideConfig.allowExtraction[side]) {
					if (this.transfer(this.OUTPUT_MAIN, AUTO_EJECT[this.getTier()], side)) {
						this.markDirty();
						break;
					}
				}
			}
		}
	}

	private boolean transfer(int slot, int maxAmount, int side) {
		ItemStack stackToTransfer = this.inventory[slot].copy();
		int amountToTransfer = Math.min(maxAmount, stackToTransfer.stackSize);
		stackToTransfer.stackSize = amountToTransfer;

		TileEntity tileInDirection = BlockHelper.getAdjacentTileEntity(this, side);

		if (Utils.isAccessibleInventory(tileInDirection, side)) {
			int leftoverSize = Utils.addToInventory(tileInDirection, side, stackToTransfer);
			if (leftoverSize >= maxAmount)
				return false;
			this.inventory[slot].stackSize = leftoverSize;
			if (this.inventory[slot].stackSize <= 0)
				this.inventory[slot] = null;
			return true;
		}

		if (Utils.isPipeTile(tileInDirection)) {
			int i = Utils.addToPipeTile(tileInDirection, side, stackToTransfer);
			if (i <= 0)
				return false;
			this.inventory[slot].stackSize -= i;
			if (this.inventory[slot].stackSize <= 0)
				this.inventory[slot] = null;
			return true;
		}

		return false;
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
		if (playerTracker != null) {
			return TEC.getUUIDForPlayerTracker(playerTracker);
		}
		return null;
	}

	public String getCurrentPlayerName() {
		return TEC.idToUsername.get(this.getCurrentPlayerUUID());
	}

	public void setAugments(ItemStack[] augments) {
		this.augments = augments.clone();
		/*
		for (int i = 0; i < augments.length; i++) {
			if (i < this.augments.length) {
				this.augments[i] = augments[i];
			}
		}
		*/
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
			TEC.setPlayerForTracker(playerTracker, player);
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
				ItemStack playerTracker = this.getAugment(EnumAugmentTA.PLAYER_TRACKER);
				if (playerTracker != null) {
					UUID playerID = TEC.getUUIDForPlayerTracker(playerTracker);
					if (!TEC.hasScannedOffline(playerID, stack)) {
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
		return 0 <= slot && slot < this.inventory.length;
	}

	public void dropAllInventory(World world, int x, int y, int z) {
		for (ItemStack stack : this.inventory)
			if (stack != null)
				this.dropStack(world, x, y, z, stack);
		for (ItemStack stack : this.augments)
			if (stack != null)
				this.dropStack(world, x, y, z, stack);
		// todo warning essentia?
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
	public boolean isItemValid(ItemStack stack, int slot, int side) {
		return this.isItemValidForSlot(slot, stack);
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
	protected boolean installAugment(int augmentSlot) {
		if (this.currentOperation != null && this.currentOperation.isRunning())
			this.currentOperation.reset();
		this.augmentList.clear();
		for (int slot = 0; slot < this.augments.length; slot++) {
			if (this.augments[slot] != null) {
				EnumAugmentTA augmentEnum = EnumAugmentTA.getByName(
						TEC.getFullName(this.augments[slot])
				);
				if (augmentEnum != null)
					this.augmentList.put(augmentEnum, slot);
			}
		}
		return super.installAugment(augmentSlot);
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

	@Override
	public boolean setFacing(int dirOrdinal) {
		this.getBlock().setTierAndDir(
				this.worldObj, this.xCoord, this.yCoord, this.zCoord,
				this.getBlock().getTier(this.getBlockMetadata()),
				ForgeDirection.values()[dirOrdinal]
		);
		return super.setFacing(dirOrdinal);
	}

	@Override
	public boolean decrSide(int face) {
		boolean ret = super.decrSide(face);
		this.sideTypes[face] = EnumSideTA.values()[this.sideCache[face]];
		PacketHandler.sendToServer(
				new PacketTileInfo(this).addString("SIDE").addInt(face).addInt(
						(int) this.sideCache[face]
				)
		);
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		return ret;
	}

	@Override
	public boolean incrSide(int face) {
		boolean ret = super.incrSide(face);
		this.sideTypes[face] = EnumSideTA.values()[this.sideCache[face]];
		PacketHandler.sendToServer(
				new PacketTileInfo(this).addString("SIDE").addInt(face).addInt(
						(int) this.sideCache[face]
				)
		);
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		return ret;
	}

	@Override
	public boolean setSide(int face, int next) {
		boolean ret = super.setSide(face, next);
		this.sideTypes[face] = EnumSideTA.values()[this.sideCache[face]];
		PacketHandler.sendToServer(
				new PacketTileInfo(this).addString("SIDE").addInt(face).addInt(
						(int) this.sideCache[face]
				)
		);
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		return ret;
	}

	@Override
	public boolean resetSides() {
		boolean ret = super.resetSides();
		this.fetchSides(this.sideCache);
		PacketHandler.sendToServer(
				new PacketTileInfo(this).addString("SIDE").addInt(-1).addInt(-1)
						.addByteArray(this.sideCache)
		);
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		return ret;
	}

	private void fetchSides(byte[] cache) {
		for (int i = 0; i < 6; i++) {
			this.sideTypes[i] = EnumSideTA.values()[cache[i]];
		}
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
		// todo sync only aspects
		PacketHandler.sendToAll(new PacketTileSync(this));
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
			this.rsMode = ControlMode.values()[packet.getInt()];
		}
		else if (type.equals("SIDE")) {
			int side = packet.getInt();
			int state = packet.getInt();
			if (side == -1) {
				byte[] cache = new byte[6];
				packet.getByteArray(cache);
				this.fetchSides(cache);
			}
			else {
				this.sideCache[side] = (byte) state;
				this.sideTypes[side] = EnumSideTA.values()[state];
			}
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

		// todo warning write auto thing in TEWrapper for Origin inventories
		// todo warning to automate the reading/writing of arrays

		//this.energyStorage.writeToNBT(tagCom);

		if (this.currentOperation != null) {
			tagCom.setString("operationClass", this.currentOperation.getClass().getCanonicalName());
			this.currentOperation.writeTo(tagCom, "operation");
		}

		/*
		for (int side = 0; side < this.sideTypes.length; side++)
			if (this.sideTypes[side] != null)
				tagCom.setInteger("side" + side, this.sideTypes[side].ordinal());
		*/

		NBTTagList augmentList = new NBTTagList();
		for (EnumAugmentTA augment : this.augmentList.keySet()) {
			NBTTagCompound augmentTag = new NBTTagCompound();
			augmentTag.setString("name", augment.getStackName());
			augmentTag.setInteger("slot", this.augmentList.get(augment));
			augmentList.appendTag(augmentTag);
		}
		tagCom.setTag("augmentList", augmentList);

		this.aspects.writeToNBT(tagCom, "aspects");

	}

	@Override
	public void readFromNBT(NBTTagCompound tagCom) {
		super.readFromNBT(tagCom);

		//this.energyStorage.readFromNBT(tagCom);

		if (tagCom.hasKey("operationClass")) {
			try {
				IOperation obj = (IOperation) Class.forName(
						tagCom.getString("operationClass")
				).newInstance();
				obj.readFrom(tagCom, "operation");
			} catch (Exception e) {
			}
		}

		/*
		for (int side = 0; side < 6; side++) {
			this.sideTypes[side] = EnumSideTA.values()
					[tagCom.getInteger("side" + side)];
		}
		*/
		this.fetchSides(this.sideCache);

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

	}

	@Override
	public void writeInventoryToNBT(NBTTagCompound tagCom) {
		super.writeInventoryToNBT(tagCom);
		tagCom.setInteger("InventorySize", this.inventory.length);
	}

	@Override
	public void readInventoryFromNBT(NBTTagCompound tagCom) {
		if (tagCom.hasKey("InventorySize"))
			this.inventory = new ItemStack[tagCom.getInteger("InventorySize")];
		super.readInventoryFromNBT(tagCom);
	}

	@Override
	public void writeAugmentsToNBT(NBTTagCompound tagCom) {
		super.writeAugmentsToNBT(tagCom);
		tagCom.setInteger("AugmentsSize", this.augments.length);
	}

	@Override
	public void readAugmentsFromNBT(NBTTagCompound tagCom) {
		if (tagCom.hasKey("AugmentsSize")) {
			this.augments = new ItemStack[tagCom.getInteger("AugmentsSize")];
			this.augmentStatus = new boolean[this.augments.length];
		}
		super.readAugmentsFromNBT(tagCom);
	}

}
