package com.temportalist.thaumicexpansion.client.gui;

import cofh.core.gui.GuiBaseAdv;
import cofh.core.gui.element.TabAugment;
import cofh.core.gui.element.TabConfiguration;
import cofh.core.gui.element.TabRedstone;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.lib.gui.element.ElementButton;
import cofh.lib.gui.element.ElementEnergyStored;
import com.temportalist.thaumicexpansion.common.TEC;
import com.temportalist.thaumicexpansion.common.inventory.ContainerThaumicAnalyzer;
import com.temportalist.thaumicexpansion.common.tile.TEThaumicAnalyzer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.lib.UtilsFX;

/**
 * http://pastebin.com/ai8acZYP
 *
 * @author TheTemportalist
 */
public class GuiDecomposer extends GuiBaseAdv {

	private static final ResourceLocation
			background = new ResourceLocation(TEC.MODID, "textures/gui/thaumicAnalyzer.png"),
			hexagon = new ResourceLocation(TEC.MODID, "textures/gui/progress.png");
	private static final int hexagonHeight = TEThaumicAnalyzer.hexagonProgressSteps * 54;

	public GuiDecomposer(ContainerThaumicAnalyzer container) {
		super(container, GuiDecomposer.background);
		this.xSize = 184;
		this.ySize = 176;
	}

	private ContainerThaumicAnalyzer container() {
		return (ContainerThaumicAnalyzer) this.inventorySlots;
	}

	@Override
	public void initGui() {
		super.initGui();

		this.addElement(new ElementEnergyStored(
				this, 14, 11, this.container().tile.getEnergyStorage()
		));
		this.addTab(new TabAugment(this, this.container()));
		this.addTab(new TabConfiguration(this, this.container().tile));
		this.addTab(new TabRedstone(this, this.container().tile));

		int centerX = (this.width / 2);
		int centerY = (this.height / 2);

		// 129x7
		this.constructBoundsOfAspects(centerX + 37, centerY - 81, 3, 4);

		this.addElement(new ElementButton(this,
				130, 71, "PreviousColumn",
				185, 0, 185, 0, 24, 8,
				GuiDecomposer.background.toString()
		));
		this.addElement(new ElementButton(this,
				154, 71, "NextColumn",
				209, 0, 209, 0, 24, 8,
				GuiDecomposer.background.toString()
		));

	}

	private int[][] aspectSlots;

	private void constructBoundsOfAspects(int startX, int startY, int qX, int qY) {
		this.aspectSlots = new int[qX * qY][4];
		int slotWidth = 16, slotHeight = 16, slotBreakX = 0, slotBreakY = 0;
		for (int i = 0; i < this.aspectSlots.length; i++) {
			int x = startX + (i / qY) * (slotWidth + slotBreakX);
			int y = startY + (i % qY) * (slotHeight + slotBreakY);
			this.aspectSlots[i] = new int[] { x, y, slotWidth, slotHeight };
		}
	}

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

	@Override
	public void handleElementButtonClick(String buttonName, int mouseButton) {
		PacketCoFHBase packet = null;
		if (buttonName.equals("PreviousColumn")) {
			packet = new PacketTileInfo(this.container().tile).addString("COLUMN").addInt(-1);
		}
		else if (buttonName.equals("NextColumn")) {
			packet = new PacketTileInfo(this.container().tile).addString("COLUMN").addInt(1);
		}
		if (packet != null) {
			PacketHandler.sendToServer(packet);
			PacketHandler.sendToAll(packet);
		}
	}

	private boolean isMouseOverArea(int mouseX, int mouseY, int[] xywh) {
		return mouseX >= xywh[0] && mouseX <= xywh[0] + xywh[2] &&
				mouseY >= xywh[1] && mouseY <= xywh[1] + xywh[3];
	}

	private int getAspectOffset() {
		return this.container().tile.getColumnOffset() * 4;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

		int centerX = (this.width / 2);
		int centerY = (this.height / 2);

		// 30x11 for hexagon
		if (this.container().tile.isProcessing()) {
			int progress = this.container().tile.getProgress();
			this.bindTexture(GuiDecomposer.hexagon);
			Gui.func_146110_a(
					centerX - 62, centerY - 77, 0, progress * 54, 64, 54,
					64, GuiDecomposer.hexagonHeight
			);
		}

		int indexOffset = this.getAspectOffset();
		AspectList aspectList = this.container().tile.getAspects();
		Aspect[] aspects = aspectList.getAspectsSorted();
		for (int i = 0; i < this.aspectSlots.length; i++) {
			int aspectI = i + indexOffset;
			if (aspectI < aspects.length) {
				int x = this.aspectSlots[i][0];
				int y = this.aspectSlots[i][1];
				UtilsFX.drawTag(x, y,
						aspects[aspectI], aspectList.getAmount(aspects[aspectI]),
						0, this.zLevel
				);
			}
		}

	}

	/*
	@Override
	protected void func_146977_a(Slot slot) {
		if (slot.getSlotIndex() == 0 &&
				slot.inventory == this.container().tile) { // todo instance of special
			GL11.glPushMatrix();
			slot.xDisplayPosition -= 16;
			slot.yDisplayPosition -= 16;
			GL11.glScalef(2, 2, 2);
			super.func_146977_a(slot);
			GL11.glScalef(.5f, .5f, .5f);
			slot.xDisplayPosition += 16;
			slot.yDisplayPosition += 16;
			GL11.glPopMatrix();
		}
		else {
			super.func_146977_a(slot);
		}
	}
	*/

}
