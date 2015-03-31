package com.temportalist.thaumicexpansion.common.inventory;

import cofh.thermalexpansion.gui.container.ContainerTEBase;
import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * http://pastebin.com/UGcqGN1n
 *
 * @author TheTemportalist
 */
public class ContainerThaumicAnalyzer extends ContainerTEBase {

	public final TEThaumicAnalyzer tile;

	public ContainerThaumicAnalyzer(final TEThaumicAnalyzer tile, InventoryPlayer playerInv) {
		super(playerInv, tile);

		this.tile = tile;

		// tile inv
		this.addSlotToContainer(new Slot(tile, tile.INPUT_MAIN, 54, 30) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return tile.isItemValidForSlot(tile.INPUT_MAIN, stack);
			}
		});
		this.addSlotToContainer(new Slot(tile, tile.INPUT_CAPACITOR, 13, 56) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return tile.isItemValidForSlot(tile.INPUT_CAPACITOR, stack);
			}
		});
		this.addSlotToContainer(new Slot(tile, tile.OUTPUT_MAIN, 88, 56) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return false;
			}
		});

	}

	@Override
	protected void addPlayerInventory(InventoryPlayer playerInv) {
		int playerInvWidth = 4, playerInvHeight = 9;
		for (int row = 0; row < 3; ++row) {
			for (int column = 0; column < 9; ++column) {
				this.addSlotToContainer(new Slot(
						playerInv,
						column + row * 9 + 9,
						8 + column * 18 + playerInvWidth,
						84 + row * 18 + playerInvHeight
				));
			}
		}
		for (int column = 0; column < 9; ++column) {
			this.addSlotToContainer(new Slot(
					playerInv, column,
					8 + column * 18 + playerInvWidth,
					142 + playerInvHeight
			));
		}
	}

}
