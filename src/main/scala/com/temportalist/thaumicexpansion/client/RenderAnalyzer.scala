package com.temportalist.thaumicexpansion.client

import com.temportalist.origin.api.client.render.TERenderItem
import com.temportalist.thaumicexpansion.client.model.ModelAnalyzer
import com.temportalist.thaumicexpansion.common.TEC
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.entity.{RenderItem, RenderManager}
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{MathHelper, ResourceLocation}
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11
import thaumcraft.common.config.ConfigItems

/**
 *
 *
 * @author TheTemportalist
 */
@SideOnly(Side.CLIENT)
class RenderAnalyzer extends TERenderItem(new ResourceLocation(TEC.MODID,
	"textures/models/analyzer.png"
)) {

	val model: ModelAnalyzer = new ModelAnalyzer()
	val renderItem: RenderItem = new RenderItem() {
		override def shouldBob(): Boolean = false
	}
	this.renderItem.setRenderManager(RenderManager.instance)

	val scanner: ItemStack = new ItemStack(ConfigItems.itemThaumometer)

	val dummy: TEAnalyzer = new TEAnalyzer

	override def getRenderingTileItem(): TileEntity = this.dummy

	override protected def render(tileEntity: TileEntity, renderPartialTicks: Float,
			f5: Float): Unit = {
		tileEntity match {
			case tile: TEAnalyzer =>

				// 0 -> north
				// 1 -> west
				// 2 -> south
				// 3 -> east

				//GL11.glEnable(GL11.GL_BLEND)

				/*
				val block: Block = TEC.analyzer
				val colorMult: Int = block.colorMultiplier(
					tile.getWorldObj, tile.xCoord, tile.yCoord, tile.zCoord)
				var red: Float = (colorMult >> 16 & 255).asInstanceOf[Float] / 255.0F
				var green: Float = (colorMult >> 8 & 255).asInstanceOf[Float] / 255.0F
				var blue: Float = (colorMult & 255).asInstanceOf[Float] / 255.0F
				if (EntityRenderer.anaglyphEnable) {
					val r1: Float = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F
					val g1: Float = (red * 30.0F + green * 70.0F) / 100.0F
					val b1: Float = (red * 30.0F + blue * 70.0F) / 100.0F
					red = r1
					green = g1
					blue = b1
				}
				if (Minecraft.isAmbientOcclusionEnabled && block.getLightValue == 0) {
					if (RenderBlocks.getInstance().partialRenderBounds) {

					}
				}
				*/
				val ticks: Float = Minecraft.getMinecraft.renderViewEntity.ticksExisted + f5

				/*
				GL11.glRotatef((tile.getFacing() match {
					case ForgeDirection.WEST => 1
					case ForgeDirection.SOUTH => 2
					case ForgeDirection.EAST => 3
					case _ => 0 // North, or anything else
				}) * 90, 0, 1, 0)
				*/

				GL11.glPushMatrix()
				GL11.glRotatef(180, 0, 0, 1)
				GL11.glTranslated(0, -1, 0)
				model.render(null, 0, 0, 0, 0, 0, f5)
				GL11.glPopMatrix()


				GL11.glScaled(0.8, 0.8, 0.8)
				this.renderScanner(tile.getWorldObj, tile.getCurrentModeString, ticks)

				GL11.glPushMatrix()
				this.renderInput(tile, ticks)
				//this.renderFuel(tile, ticks)
				GL11.glPopMatrix()


			case _ =>
		}
	}

	def renderBlock(tile: TEAnalyzer): Unit = {
		GL11.glPushMatrix()
		GL11.glDisable(GL11.GL_LIGHTING)
		for (side: ForgeDirection <- ForgeDirection.VALID_DIRECTIONS) {
			val side_x: Int = tile.xCoord + side.offsetX
			val side_y: Int = tile.yCoord + side.offsetY
			val side_z: Int = tile.zCoord + side.offsetZ
			if (tile.getBlockType.shouldSideBeRendered(tile.getWorldObj,
				side_x, side_y, side_z, side.ordinal())
			) {
				val sideAdjusted: Int =
					if (side == ForgeDirection.NORTH) 3
					else
					if (side != ForgeDirection.DOWN && side != ForgeDirection.UP) 2
					else side.ordinal()
				val sidePackage: String = "textures/blocks/thaumicAnalyzer/side" +
						sideAdjusted + "/"
				//String.format("textures/blocks/thaumicAnalyzer/side%d/", sideAdjusted)
				/*
				GL11.glPushMatrix()
				//this.renderSide(tile, side, side_x, side_y, side_z)
				this.bindTexture(new ResourceLocation(TEC.MODID,
					sidePackage + this.getSideTex(tile, side) + ".png"
				))
				this.drawSide(tile, side_x, side_y, side_z, side)
				GL11.glPopMatrix()
				*/
				GL11.glPushMatrix()
				//this.renderPort(tile, side, side_x, side_y, side_z)
				this.bindTexture(new ResourceLocation(TEC.MODID,
					sidePackage + this.getPortTex(tile, side) + ".png"
				))
				this.drawSide(tile, side_x, side_y, side_z, side)
				GL11.glPopMatrix()
			}
		}
		GL11.glEnable(GL11.GL_LIGHTING)
		GL11.glPopMatrix()
	}

	def getPortTex(tile: TEAnalyzer, side: ForgeDirection): Any = "colours/default"

	def drawSide(tile: TEAnalyzer, x: Int, y: Int, z: Int, side: ForgeDirection): Unit = {
		val half: Double = 0.5
		val tess: Tessellator = Tessellator.instance
		//GL11.glEnable(GL11.GL_BLEND)
		//OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f)
		tess.startDrawingQuads()
		///*
		/*
		tess.setBrightness(
			tile.getBlockType.getMixedBrightnessForBlock(tile.getWorldObj, x, y, z)
			//15 << 20 | 15 << 4
		)
		*/
		//*/
		tess.setBrightness(15 << 20 | 15 << 4)
		//tess.setBrightness(983055)
		//tess.setColorOpaque_F(1, 1, 1)
		//GL11.glColor4f(1, 1, 1, 1)
		side match {
			case ForgeDirection.DOWN =>
				tess.addVertexWithUV(-half, -half, +half, 0, 0)
				tess.addVertexWithUV(-half, -half, -half, 0, 1)
				tess.addVertexWithUV(+half, -half, -half, 1, 1)
				tess.addVertexWithUV(+half, -half, +half, 1, 0)
			case ForgeDirection.UP =>
				tess.addVertexWithUV(+half, +half, +half, 0, 0)
				tess.addVertexWithUV(+half, +half, -half, 0, 1)
				tess.addVertexWithUV(-half, +half, -half, 1, 1)
				tess.addVertexWithUV(-half, +half, +half, 1, 0)
			case ForgeDirection.NORTH =>
				tess.addVertexWithUV(+half, +half, -half, 0, 0)
				tess.addVertexWithUV(+half, -half, -half, 0, 1)
				tess.addVertexWithUV(-half, -half, -half, 1, 1)
				tess.addVertexWithUV(-half, +half, -half, 1, 0)
			case ForgeDirection.SOUTH =>
				tess.addVertexWithUV(-half, +half, +half, 0, 0)
				tess.addVertexWithUV(-half, -half, +half, 0, 1)
				tess.addVertexWithUV(+half, -half, +half, 1, 1)
				tess.addVertexWithUV(+half, +half, +half, 1, 0)
			case ForgeDirection.WEST =>
				tess.addVertexWithUV(-half, +half, -half, 0, 0)
				tess.addVertexWithUV(-half, -half, -half, 0, 1)
				tess.addVertexWithUV(-half, -half, +half, 1, 1)
				tess.addVertexWithUV(-half, +half, +half, 1, 0)
			case ForgeDirection.EAST =>
				tess.addVertexWithUV(+half, +half, +half, 0, 0)
				tess.addVertexWithUV(+half, -half, +half, 0, 1)
				tess.addVertexWithUV(+half, -half, -half, 1, 1)
				tess.addVertexWithUV(+half, +half, -half, 1, 0)
			case _ =>
		}
		tess.draw()
	}

	def renderScanner(world: World, mode: String, ticks: Float): Unit = {
		val entity: EntityItem = new EntityItem(world)
		entity.setEntityItemStack(this.scanner)
		entity.hoverStart = 0.0F
		GL11.glPushMatrix()

		mode match {
			case "analyzer" =>
				// centering the scanner
				GL11.glTranslated(0, 0.2, 0)
				val speed: Double = 6D
				val range: Double = 0.25D
				// rotation speed
				GL11.glRotated((ticks % 360D) * speed, 0, 1, 0)
				// rotate scanner vertically
				GL11.glRotated(90, 0, 0, 1)
				// radius of circle
				GL11.glTranslated(0, range, 0)
				this.renderEnt(entity)
				// inner scanner
				GL11.glTranslated(0, range - 0.05, 0)
				GL11.glRotated(180, 0, 0, 1)
				this.renderEnt(entity)
			case "decomposer" =>
				GL11.glTranslated(0, -0.2, 0)
				val speedMult: Double = 7D
				val rangeMult: Double = 0.33D
				GL11.glTranslated(0, Math.sin(ticks / speedMult) * rangeMult + rangeMult, 0)
				this.renderEnt(entity)
			case _ =>
		}

		GL11.glPopMatrix()
	}

	def renderEnt(ent: Entity): Unit =
		RenderManager.instance.renderEntityWithPosYaw(ent, 0, 0, 0, 0, 0)

	def renderInput(tile: TEAnalyzer, ticks: Float): Unit = {
		if (tile.getInput != null) {
			GL11.glPushMatrix()
			GL11.glRotatef(ticks % 360.0F, 0.0F, 1.0F, 0.0F)
			GL11.glEnable(3042)
			GL11.glBlendFunc(770, 1) // todo fade as progress nears finishing if decomposing
			val stack: ItemStack = tile.getInput.copy()
			stack.stackSize = 1
			val entity: EntityItem = new EntityItem(tile.getWorldObj, 0.0D, 0.0D, 0.0D, stack)
			entity.hoverStart = MathHelper.sin(ticks / 14.0F) * 0.2F
			RenderItem.renderInFrame = true
			RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F)
			RenderItem.renderInFrame = false
			GL11.glDisable(3042)
			GL11.glPopMatrix()
		}
	}

	def renderFuel(tile: TEAnalyzer, ticks: Float): Unit = {
		if (tile.getFuel != null) {
			val stack: ItemStack = tile.getFuel.copy()
			val q: Int = Math.min(stack.stackSize, 6)
			val angle: Double = 19.9
			val r: Double = .29
			stack.stackSize = 1
			GL11.glRotatef(ticks % 360F, 0, 1, 0)
			for (i <- 0 until q) {
				GL11.glPushMatrix()
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
				GL11.glTranslated(
					Math.sin(angle * i) * r, 0, Math.cos(angle * i) * r
				)
				GL11.glScalef(0.25F, 0.25F, 0.25F)
				GL11.glRotatef(ticks % 360.0F * -5, 0.0F, 1.0F, 0.0F)
				val rot: Float = MathHelper.sin(ticks / 14.0F) * 0.2F + 0.2F
				val entity: EntityItem = new EntityItem(tile.getWorldObj, 0D, 0D, 0D, stack)
				entity.hoverStart = rot
				RenderItem.renderInFrame = true
				RenderManager.instance.renderEntityWithPosYaw(entity,
					0, 0.0D, 0, 0.0F, 0.0F)
				RenderItem.renderInFrame = false
				GL11.glPopMatrix()
			}
		}
	}

}
