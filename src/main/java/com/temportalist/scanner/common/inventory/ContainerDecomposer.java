package com.temportalist.scanner.common.inventory;

import cofh.lib.gui.container.IAugmentableContainer;
import com.temportalist.scanner.common.TEDecomposer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/**
 * @author TheTemportalist
 */
public class ContainerDecomposer extends Container implements IAugmentableContainer {

	public final TEDecomposer tile;
	private Slot[] augments;

	public ContainerDecomposer(TEDecomposer tile, IInventory playerInv) {
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
						103 + row * 18 + playerInvHeight
				));
			}
		}
		for (int column = 0; column < 9; ++column) {
			this.addSlotToContainer(new Slot(
					playerInv, column, 8 + column * 18, 161 + playerInvHeight
			));
		}

		// tile inv
		this.addSlotToContainer(new Slot(tile, 0, 20, 20));

		this.augments = new Slot[] {
				new Slot(this.tile, 1, 40, 40)
		};
		this.addSlotToContainer(this.augments[0]);


	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return this.tile.isUseableByPlayer(player);
	}

	@Override
	public void setAugmentLock(boolean b) {
	} // todo

	@Override
	public Slot[] getAugmentSlots() {
		return new Slot[0];
	}

}
