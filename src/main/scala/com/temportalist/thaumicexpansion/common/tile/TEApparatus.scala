package com.temportalist.thaumicexpansion.common.tile

import com.temportalist.thaumicexpansion.common.cofh.TraitEnergyReceiver
import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

/**
 *
 *
 * @author TheTemportalist
 */
@Optional.Interface(modid = "CoFHLib", iface = "TraitEnergyReceiver", striprefs = false)
class TEApparatus extends TileEntity with TraitEnergyReceiver {

	private var facingHorizontal: ForgeDirection = ForgeDirection.UNKNOWN
	private var facingVertical: ForgeDirection = ForgeDirection.UNKNOWN

	override def getMaxEnergy(): Int = 16000 // 1 coal

	def setFacing(h: ForgeDirection, v: ForgeDirection): Unit = {
		if (h != null) this.facingHorizontal = h
		if (v != null) this.facingVertical = v
		this.markDirty()
	}

	def getFacingH(): ForgeDirection = this.facingHorizontal

	/**
	 * Rotates the render based on current facing. Assumes that render's front is ForgeDirection.NORTH
	 */
	@SideOnly(Side.CLIENT)
	def rotateViaFacing(): Unit = {
		// todo to V3O
		// Axis:
		//  Lateral axis (NS = Z Axis)
		//  Normal axis (NS = Y OR X)
		// N -> 0° Y axis
		// S -> 180° Y Axis
		// W -> 270° Y Axis
		// E -> 90° Y Axis
		// D -> -90° X Axis
		// U -> 90° X Axis

		/**
				-Z
		    -X      +X
		        +Z

		        N
		    W       E
		        S

			D, U, N, S, W, E
			Angle = {
		        NORTH(2): ?, ?, 000, 180, 270, 090
		        SOUTH(3): ?, ?, 180, 000, 090, 270
		        WEST(4):  ?, ?, 090, 270, 000, 180
		        EAST(5):  ?, ?, 270, 090, 180, 000
		    }
		    AxisOfAngle = {
		        NORTH(2): ?, ?, Y, Y, Y, Y
		    }

			Axis.rotate(this.facing, ForgeDirection.NORTH) {

		    }
		  */

	}

	override def updateEntity(): Unit = {
		super.updateEntity()

	}

}
