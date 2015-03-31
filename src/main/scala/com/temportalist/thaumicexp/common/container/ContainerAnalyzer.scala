package com.temportalist.thaumicexp.common.container

import com.temportalist.origin.library.common.container.{SlotOutput, SlotValidate}
import com.temportalist.origin.wrapper.common.inventory.ContainerWrapper
import com.temportalist.thaumicexp.common.tile.TEAnalyzer
import net.minecraft.entity.player.EntityPlayer

/**
 *
 *
 * @author TheTemportalist
 */
class ContainerAnalyzer(tile: TEAnalyzer, player: EntityPlayer)
		extends ContainerWrapper(player, tile) {

	/**
	 * Used to register slots for this container
	 * Subclasses SHOULD use this method (that is the reason we have containers),
	 * however, subclasses do not NEED to use this method.
	 */
	override protected def registerSlots(): Unit = {

		this.addSlotToContainer(new SlotValidate(tile, tile.INPUT, 54, 30))
		this.addSlotToContainer(new SlotValidate(tile, tile.slotFuel(), 13, 56))
		this.addSlotToContainer(new SlotOutput(tile, tile.OUTPUT, 88, 56))
		this.addSlotToContainer(new SlotValidate(tile, tile.MODE, 88, 6))

		this.registerPlayerSlots(4, 9)
	}

	override def canInteractWith(player : EntityPlayer): Boolean = true

}
