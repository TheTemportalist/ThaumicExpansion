package com.temportalist.thaumicexpansion.client

import java.util

import com.temportalist.origin.api.client.gui.GuiContainerBase
import com.temportalist.origin.api.client.utility.Rendering
import com.temportalist.origin.foundation.client.gui.GuiButtonImg
import com.temportalist.origin.foundation.common.network.PacketTileCallback
import com.temportalist.thaumicexpansion.common._
import com.temportalist.thaumicexpansion.common.container.ContainerAnalyzer
import com.temportalist.thaumicexpansion.common.network.PacketGiveAspect
import com.temportalist.thaumicexpansion.common.tile.TEAnalyzer
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.gui.{Gui, GuiButton}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import thaumcraft.api.aspects.{Aspect, AspectList}
import thaumcraft.client.lib.UtilsFX
import thaumcraft.common.Thaumcraft

import scala.util.control.Breaks.{break, breakable}

/**
 *
 *
 * @author TheTemportalist
 */
@SideOnly(Side.CLIENT)
class GuiAnalyzer(c: ContainerAnalyzer) extends GuiContainerBase(c) {

	private final val background: ResourceLocation = new ResourceLocation(
		TEC.MODID, "textures/gui/thaumicAnalyzer.png")
	private final val energy: ResourceLocation = new ResourceLocation(TEC.MODID,
		"textures/gui/power.png"
	)
	private final val hexagon: ResourceLocation = new ResourceLocation(
		TEC.MODID, "textures/gui/progress.png")
	private final val hexagonHeight: Int = this.getTile().hexagonProgressSteps * 54

	private var aspectSlots: Array[Array[Int]] = null

	this.xSize = 184
	this.ySize = 176

	private var aspectLeft: GuiButtonImg = null
	private var aspectRight: GuiButtonImg = null

	private def container: ContainerAnalyzer =
		this.inventorySlots.asInstanceOf[ContainerAnalyzer]

	def getTile(): TEAnalyzer = this.container.getTileEntity.asInstanceOf[TEAnalyzer]

	override def initGui(): Unit = {
		super.initGui()

		val centerX: Int = this.width / 2
		val centerY: Int = this.height / 2
		this.constructBoundsOfAspects(centerX + 42, centerY - 76, 3, 4)

		this.aspectLeft = new GuiButtonImg(0, "AspectLeft", centerX + 41, centerY - 11, 24, 8,
			185, 0, this.background.toString
		)
		this.addButton(this.aspectLeft)
		this.aspectRight = new GuiButtonImg(1, "AspectRight", centerX + 65, centerY - 11, 24, 8,
			209, 0, this.background.toString
		)
		this.addButton(this.aspectRight)

	}

	private def constructBoundsOfAspects(startX: Int, startY: Int, qX: Int, qY: Int) {
		this.aspectSlots = new Array[Array[Int]](qX * qY)
		val slotWidth: Int = 16
		val slotHeight: Int = 16
		val slotBreakX: Int = 0
		val slotBreakY: Int = 0
		for (i <- 0 until this.aspectSlots.length) {
			val x: Int = startX + (i / qY) * (slotWidth + slotBreakX)
			val y: Int = startY + (i % qY) * (slotHeight + slotBreakY)
			this.aspectSlots(i) = Array[Int](x, y, slotWidth, slotHeight)
		}
	}

	/*
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		// -1 released, 0 left, 1 right, 2 center
		if (mouseButton == 0 || mouseButton == 1) {
			for (int i = 0; i < this.aspectSlots.length; i++) {
				if (this.isMouseOverArea(mouseX, mouseY, this.aspectSlots[i])) {
					//FMLLog.info((i + this.getAspectOffset()) + "");
					PacketCoFHBase packet = new PacketTileInfo(
							this.container().tile
					).addString("ADDASPECT").addInt(
							i + this.getAspectOffset()
					).addBool(mouseButton == 0);
					PacketHandler.sendToServer(packet);
					PacketHandler.sendToAll(packet);
					break;
				}
			}
		}
	}
	 */

	override def mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Unit = {
		super.mouseClicked(mouseX, mouseY, mouseButton)
		// -1 released, 0 left, 1 right, 2 center
		// todo primarily check if inside aspect area bounds
		if (mouseButton == 0 || mouseButton == 1) {
			val aspects: Array[Aspect] = this.getTile().getAspects.getAspectsSorted
			breakable {
				for (i <- this.aspectSlots.indices) {
					val index: Int = i + this.getAspectOffset
					if (index >= 0 && index < aspects.length && aspects(index) != null &&
							this.isMouseOverArea(mouseX, mouseY, this.aspectSlots(i))) {
						if (this.doesPlayerKnow(Rendering.mc.thePlayer, aspects(index)))
							new PacketGiveAspect(this.getTile(),
								index, mouseButton == 0).sendToServer(TEC)
						break()
					}
				}
			}
		}
	}

	override def actionPerformed(button: GuiButton): Unit = {
		if (button.id == this.aspectLeft.id) {
			//packet = new PacketColumnChange(this.getTile(), 1)
			new PacketTileCallback(this.getTile()).add("ColumnChange").add(true).sendToServer(TEC)
		}
		else if (button.id == this.aspectRight.id) {
			//packet = new PacketColumnChange(this.getTile(), -1)
			new PacketTileCallback(this.getTile()).add("ColumnChange").add(false).sendToServer(TEC)
		}
	}

	private def isMouseOverArea(mouseX: Int, mouseY: Int, xywh: Array[Int]): Boolean = {
		mouseX >= xywh(0) && mouseX <= xywh(0) + xywh(2) && mouseY >= xywh(1) &&
				mouseY <= xywh(1) + xywh(3)
	}

	private def getAspectOffset: Int = this.getTile().getColumnOffset * 4

	override def drawGuiContainerBackgroundLayer(partialTicks: Float, mX: Int, mY: Int): Unit = {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
		this.bindTexture(this.background)
		drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)

		val centerX: Int = this.width / 2
		val centerY: Int = this.height / 2

		this.bindTexture(this.energy)
		// x y u v wImg hImg wTex hTex
		// todo detectProgressBar for energy instead of packets
		Gui.func_146110_a(this.guiLeft + 14, this.guiTop + 11,
			0, 0, 14, 42, 28, 42)
		if (this.getTile().getEnergy > 0) {
			val h: Int = this.getTile().scaleEnergy(40)
			Gui.func_146110_a(this.guiLeft + 14, this.guiTop + 11 + (41 - h),
				14, 41 - h, 14, h + 1, 28, 42)
		}

		// 30x11 for hexagon
		if (this.getTile().isProcessing) {
			val progress: Int = this.container.getProgress()
			//println(progress)
			this.bindTexture(this.hexagon)
			Gui.func_146110_a(centerX - 58, centerY - 72, 0,
				progress * 54, 64, 54, 64, this.hexagonHeight)
		}

		val indexOffset: Int = this.getAspectOffset
		val aspectList: AspectList = this.getTile().getAspects
		val aspects: Array[Aspect] = aspectList.getAspectsSorted
		for (i <- 0 until this.aspectSlots.length) {
			val aspectI: Int = i + indexOffset
			if (aspectI < aspects.length) {
				val x: Int = this.aspectSlots(i)(0)
				val y: Int = this.aspectSlots(i)(1)
				val aspect = aspects(aspectI)
				if (this.doesPlayerKnow(Rendering.mc.thePlayer, aspect)) {
					UtilsFX.drawTag(x, y, aspects(aspectI),
						aspectList.getAmount(aspects(aspectI)), 0, this.zLevel)
				}
				else {
					UtilsFX.bindTexture("textures/aspects/_unknown.png")
					GL11.glPushMatrix()
					GL11.glColor4f(0.8F, 0.8F, 0.8F, 0.5F)
					GL11.glEnable(3042)
					GL11.glBlendFunc(770, 771)
					GL11.glTranslated(x, y, 0.0D)
					UtilsFX.drawTexturedQuadFull(0, 0, this.zLevel)
					GL11.glDisable(3042)
					GL11.glPopMatrix()
				}
			}
		}

	}

	private def doesPlayerKnow(player: EntityPlayer, aspect: Aspect): Boolean = {
		Thaumcraft.proxy.getPlayerKnowledge.hasDiscoveredAspect(
			player.getCommandSenderName, aspect)
	}

	override protected def addInformationOnHover(mouseX: Int, mouseY: Int,
			renderPartialTicks: Float, hoverInfo: util.List[String]): Unit = {
		if (this.isMouseOverArea(mouseX, mouseY,
			Array[Int](this.guiLeft + 14, this.guiTop + 11, 14, 42))) {
			hoverInfo.add("Energy: " + this.getTile().getEnergy +
					" / " + this.getTile().getMaxEnergy)
			hoverInfo.add("Buffer: " + this.getTile().loadingEnergy)
		}
	}

}
