package com.temportalist.thaumicexpansion.common.container

import com.temportalist.origin.api.common.container.{SlotOutput, SlotValidate}
import com.temportalist.origin.api.common.inventory.ContainerBase
import com.temportalist.origin.api.common.utility.Scala
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.entity.player.EntityPlayer
import java.util

import net.minecraft.inventory.ICrafting

/**
 *
 *
 * @author TheTemportalist
 */
class ContainerAnalyzer(tile: TEAnalyzer, player: EntityPlayer)
		extends ContainerBase(player, tile) {

	/**
	 * Used to register slots for this container
	 * Subclasses SHOULD use this method (that is the reason we have containers),
	 * however, subclasses do not NEED to use this method.
	 */
	override protected def registerSlots(): Unit = {

		this.addSlotToContainer(new SlotValidate(tile, tile.SLOT_INPUT, 54, 30))
		this.addSlotToContainer(new SlotValidate(tile, tile.slotFuel(), 13, 56))
		this.addSlotToContainer(new SlotOutput(tile, tile.SLOT_OUTPUT, 88, 56))
		this.addSlotToContainer(new SlotValidate(tile, tile.SLOT_MODE, 88, 6))

		this.registerPlayerSlots(0, 0)

	}

	override def canInteractWith(player: EntityPlayer): Boolean = true

	private var localProgress: Int = 0

	def getProgress(): Int =this.localProgress

	override def detectAndSendChanges(): Unit = {
		super.detectAndSendChanges()
		this.getTileEntity match {
			case tile: TEAnalyzer =>
				val progress: Int = tile.getProgress
				if (this.localProgress != progress) {
					Scala.foreach(this.crafters.asInstanceOf[util.List[ICrafting]],
						(index: Int, player: ICrafting) => {
							player.sendProgressBarUpdate(this, 0, progress)
						}: Unit
					)
				}
				this.localProgress = progress
			case _ =>
		}
	}

	@SideOnly(Side.CLIENT)
	override def updateProgressBar(id: Int, value: Int): Unit = {
		id match {
			case 0 =>
				this.localProgress = value
			case _ =>
		}
	}
}
