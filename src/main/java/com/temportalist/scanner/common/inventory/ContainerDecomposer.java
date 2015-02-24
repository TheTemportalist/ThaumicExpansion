package com.temportalist.scanner.common.inventory;

import cofh.core.gui.slot.SlotAugment;
import cofh.lib.gui.container.IAugmentableContainer;
import cofh.lib.gui.slot.SlotFalseCopy;
import cofh.lib.util.helpers.AugmentHelper;
import cofh.lib.util.helpers.ItemHelper;
import com.temportalist.scanner.common.TEDecomposer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * http://pastebin.com/UGcqGN1n
 *
 * @author TheTemportalist
 */
public class ContainerDecomposer extends Container implements IAugmentableContainer {

	public final TEDecomposer tile;
	private Slot[] augments;
	protected boolean augmentLock = true;

	public ContainerDecomposer(TEDecomposer tile, InventoryPlayer playerInv) {
		this.tile = tile;

		tile.openInventory();

		// player inv
		int playerInvHeight = 0;
		for (int row = 0; row < 3; ++row) {
			for (int column = 0; column < 9; ++column) {
				this.addSlotToContainer(new Slot(
						playerInv,
						column + row * 9 + 9,
						8 + column * 18,
						84 + row * 18 + playerInvHeight
				));
			}
		}
		for (int column = 0; column < 9; ++column) {
			this.addSlotToContainer(new Slot(
					playerInv, column, 8 + column * 18, 142 + playerInvHeight
			));
		}

		// tile inv
		this.addSlotToContainer(new Slot(tile, 0, 45 + 8, 19 + 8)); // todo

		this.augments = new Slot[this.tile.getAugmentSlots().length];
		for (int i = 0; i < this.augments.length; i++) {
			this.augments[i] = addSlotToContainer(new SlotAugment(this.tile, null, i, 0, 0));
		}

	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return this.tile.isUseableByPlayer(player);
	}

	@Override
	public void setAugmentLock(boolean b) {
		this.augmentLock = b;
		/* todo
		if (ServerHelper.isClientWorld(this.tile.getWorldObj())) {
			PacketTEBase.sendTabAugmentPacketToServer(lock);
		}
		*/
	}

	@Override
	public Slot[] getAugmentSlots() {
		return this.augments;
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		for (int i = 0; i < crafters.size(); i++) {
			this.tile.sendGuiNetworkData(this, (ICrafting) crafters.get(i));
		}
	}

	@Override
	public void updateProgressBar(int i, int j) {
		this.tile.receiveGuiNetworkData(i, j);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {

		ItemStack stack = null;
		Slot slot = (Slot) inventorySlots.get(slotIndex);

		int invAugment = 0;
		int invPlayer = invAugment + 27;
		int invFull = invPlayer + 9;
		int invTile = invFull + 1; // the slots in the container from the inventory

		if (slot != null && slot.getHasStack()) {
			ItemStack stackInSlot = slot.getStack();
			stack = stackInSlot.copy();

			if (slotIndex < invAugment) {
				if (!this.mergeItemStack(stackInSlot, invAugment, invFull, true)) {
					return null;
				}
			}
			else if (slotIndex < invFull) {
				if (!augmentLock && invAugment > 0 && AugmentHelper.isAugmentItem(stackInSlot)) {
					if (!this.mergeItemStack(stackInSlot, 0, invAugment, false)) {
						return null;
					}
				}
				else if (!this.mergeItemStack(stackInSlot, invFull, invTile, false)) {
					return null;
				}
			}
			else if (!this.mergeItemStack(stackInSlot, invAugment, invFull, true)) {
				return null;
			}
			if (stackInSlot.stackSize <= 0) {
				slot.putStack((ItemStack) null);
			}
			else {
				slot.onSlotChanged();
			}
			if (stackInSlot.stackSize == stack.stackSize) {
				return null;
			}
		}
		return stack;
	}

	@Override
	public ItemStack slotClick(int slotId, int mouseButton, int modifier, EntityPlayer player) {

		Slot slot = slotId < 0 ? null : (Slot) this.inventorySlots.get(slotId);
		if (slot instanceof SlotFalseCopy) {
			if (mouseButton == 2) {
				slot.putStack(null);
				slot.onSlotChanged();
			}
			else {
				slot.putStack(player.inventory.getItemStack() == null ?
						null :
						player.inventory.getItemStack().copy());
			}
			return player.inventory.getItemStack();
		}
		return super.slotClick(slotId, mouseButton, modifier, player);
	}

	@Override
	protected boolean mergeItemStack(ItemStack stack, int slotMin, int slotMax, boolean ascending) {

		boolean slotFound = false;
		int k = ascending ? slotMax - 1 : slotMin;

		Slot slot;
		ItemStack stackInSlot;

		if (stack.isStackable()) {
			while (stack.stackSize > 0 && (!ascending && k < slotMax
					|| ascending && k >= slotMin)) {
				slot = (Slot) this.inventorySlots.get(k);
				stackInSlot = slot.getStack();

				if (slot.isItemValid(stack) && ItemHelper
						.itemsEqualWithMetadata(stack, stackInSlot, true)) {
					int l = stackInSlot.stackSize + stack.stackSize;
					int slotLimit = Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit());

					if (l <= slotLimit) {
						stack.stackSize = 0;
						stackInSlot.stackSize = l;
						slot.onSlotChanged();
						slotFound = true;
					}
					else if (stackInSlot.stackSize < slotLimit) {
						stack.stackSize -= slotLimit - stackInSlot.stackSize;
						stackInSlot.stackSize = slotLimit;
						slot.onSlotChanged();
						slotFound = true;
					}
				}
				k += ascending ? -1 : 1;
			}
		}
		if (stack.stackSize > 0) {
			k = ascending ? slotMax - 1 : slotMin;

			while (!ascending && k < slotMax || ascending && k >= slotMin) {
				slot = (Slot) this.inventorySlots.get(k);
				stackInSlot = slot.getStack();

				if (slot.isItemValid(stack) && stackInSlot == null) {
					slot.putStack(ItemHelper.cloneStack(stack,
							Math.min(stack.stackSize, slot.getSlotStackLimit())));
					slot.onSlotChanged();

					if (slot.getStack() != null) {
						stack.stackSize -= slot.getStack().stackSize;
						slotFound = true;
					}
					break;
				}
				k += ascending ? -1 : 1;
			}
		}
		return slotFound;
	}

}
