package com.temportalist.thaumicexpansion.client

import com.temportalist.origin.api.client.render.TERenderItem
import com.temportalist.origin.api.common.utility.Scala
import com.temportalist.thaumicexpansion.client.model.ModelApparatus
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.tile.TEApparatus
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

/**
 *
 *
 * @author TheTemportalist 4/5/15
 */
object RenderApparatus extends TERenderItem(new ResourceLocation(TEC.MODID,
	"textures/models/apparatus.png"
)) {

	val model: ModelApparatus = new ModelApparatus

	val dummy: TEApparatus = new TEApparatus

	override def getRenderingTileItem(): TileEntity = this.dummy

	override protected def render(tileEntity: TileEntity, renderPartialTicks: Float,
			f5: Float): Unit = {
		tileEntity match {
			case tile: TEApparatus =>
				val facingSide: Int = tile.getFacing

				val switchedSideArray: Array[Int] = this.getSwitchedSidesForFacing(facingSide)
				Scala.foreach(tile.getActiveSides, (index: Int, isActive: Boolean) => {
					switchedSideArray(index) match {
						case 1 => // up-top
							if (this.model.Arch_T.showModel != isActive)
								this.model.Arch_T.showModel = isActive
						case 2 => // north-front
							if (this.model.Arch_F.showModel != isActive)
								this.model.Arch_F.showModel = isActive
						case 3 => // south-back
							if (this.model.Arch_B.showModel != isActive)
								this.model.Arch_B.showModel = isActive
						case 4 => // west-left
							if (this.model.Arch_L.showModel != isActive)
								this.model.Arch_L.showModel = isActive
						case 5 => // east-right
							if (this.model.Arch_R.showModel != isActive)
								this.model.Arch_R.showModel = isActive
						case _ =>
					}
				}: Unit)

				this.rotateModelForFacing(facingSide)

				GL11.glRotated(180, 0, 0, 1)
				GL11.glTranslated(0, -1, 0)
				this.model.render(null, 0, 0, 0, 0, 0, f5)

			case _ =>
		}
	}

	/*
	def getAxis(dir: ForgeDirection): ForgeDirection => dir match {
		case
	}
	*/

	def getSwitchedSidesForFacing(facing: Int): Array[Int] = {
		facing match {
			// Facing up
			case 1 => Array[Int](1, 0, 2, 3, 5, 4)
			// Facing North
			case 2 => Array[Int](3, 2, 0, 1, 4, 5)
			// Facing SOUTH
			case 3 => Array[Int](2, 3, 1, 0, 4, 5)
			// Facing West
			case 4 => Array[Int](5, 4, 2, 3, 0, 1)
			// Facing East
			case 5 => Array[Int](4, 5, 2, 3, 1, 0)
			// default Down (0)
			case _ => Array[Int](0, 1, 2, 3, 4, 5)
		}
	}

	def rotateModelForFacing(facing: Int): Unit = {
		// this defaults FROM down to facing
		val rotArray: Array[Float] = facing match {
			case 1 => Array[Float](180, 0, 0, 1)
			case 2 => Array[Float](90, 1, 0, 0)
			case 3 => Array[Float](270, 1, 0, 0)
			case 4 => Array[Float](270, 0, 0, 1)
			case 5 => Array[Float](90, 0, 0, 1)
			case _ => return
		}
		GL11.glRotatef(rotArray(0), rotArray(1), rotArray(2), rotArray(3))
	}

}
