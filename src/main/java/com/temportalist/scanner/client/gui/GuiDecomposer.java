package com.temportalist.scanner.client.gui;

import cofh.core.gui.GuiBaseAdv;
import cofh.core.gui.element.TabAugment;
import cofh.core.gui.element.TabConfiguration;
import cofh.core.gui.element.TabRedstone;
import cofh.lib.gui.element.ElementEnergyStored;
import com.temportalist.scanner.common.Scanner;
import com.temportalist.scanner.common.TEDecomposer;
import com.temportalist.scanner.common.inventory.ContainerDecomposer;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
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
			background = new ResourceLocation(Scanner.MODID, "textures/gui/decomposer.png"),
			hexagon = new ResourceLocation(Scanner.MODID, "textures/gui/progress.png");
	private static final int hexagonHeight = TEDecomposer.hexagonProgressSteps * 54;

	public GuiDecomposer(ContainerDecomposer container) {
		super(container, GuiDecomposer.background);
		this.xSize = 176;
		this.ySize = 166;
	}

	private ContainerDecomposer container() {
		return (ContainerDecomposer) this.inventorySlots;
	}

	@Override
	public void initGui() {
		super.initGui();

		int centerX = (this.width - this.xSize) / 2;
		int centerY = (this.height - this.ySize) / 2;

		this.addElement(new ElementEnergyStored(
				this, 8, 8, this.container().tile.getEnergyStorage()
		));
		this.addTab(new TabAugment(this, this.container()));
		this.addTab(new TabConfiguration(this, this.container().tile));
		this.addTab(new TabRedstone(this, this.container().tile));

	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

		int centerX = (this.width / 2);
		int centerY = (this.height / 2);

		// 29x8 for hexagon
		if (this.container().tile.isProcessing()) {
			int progress = this.container().tile.getProgress();
			this.bindTexture(GuiDecomposer.hexagon);
			Gui.func_146110_a(
					centerX - 59, centerY - 75, 0, progress * 54, 64, 54,
					64, GuiDecomposer.hexagonHeight
			);
		}

		// aspect 0,0 is 116x9
		int xStart = centerX + 28;
		int yStart = centerY - 74;
		int indexOffset = this.container().tile.getColumnOffset() * 4;
		AspectList aspectList = this.container().tile.getAspects();
		Aspect[] aspects = aspectList.getAspects();
		for (int i = indexOffset; i < aspects.length; i++) {
			int index = i - indexOffset;
			int x = xStart + (index / 4) * 18;
			int y = yStart + (index % 4) * 18;
			UtilsFX.drawTag(x, y, aspects[i], aspectList.getAmount(aspects[i]), 0, this.zLevel);
		}

	}

	@Override
	protected void func_146977_a(Slot slot) {
		if (slot.getSlotIndex() == 0 &&
				slot.inventory == this.container().tile) { // todo instance of special
			GL11.glPushMatrix();
			slot.xDisplayPosition -= 30;
			slot.yDisplayPosition -= 18;
			GL11.glScalef(2, 2, 2);
			super.func_146977_a(slot);
			GL11.glScalef(.5f, .5f, .5f);
			slot.xDisplayPosition += 30;
			slot.yDisplayPosition += 18;
			GL11.glPopMatrix();
		}
		else {
			super.func_146977_a(slot);
		}
	}

}
