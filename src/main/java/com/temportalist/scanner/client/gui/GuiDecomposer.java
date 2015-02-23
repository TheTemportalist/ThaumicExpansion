package com.temportalist.scanner.client.gui;

import cofh.core.gui.GuiBaseAdv;
import cofh.core.gui.element.TabAugment;
import cofh.core.gui.element.TabConfiguration;
import cofh.core.gui.element.TabRedstone;
import cofh.lib.gui.element.ElementEnergyStored;
import com.temportalist.scanner.common.Scanner;
import com.temportalist.scanner.common.inventory.ContainerDecomposer;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;

/**
 * http://pastebin.com/ai8acZYP
 *
 * @author TheTemportalist
 */
public class GuiDecomposer extends GuiBaseAdv {

	public GuiDecomposer(ContainerDecomposer container) {
		super(container, new ResourceLocation(Scanner.MODID, "textures/gui/decomposer.png"));
	}

	private ContainerDecomposer container() {
		return (ContainerDecomposer) this.inventorySlots;
	}

	@Override
	public void initGui() {
		super.initGui();

		this.addElement(new ElementEnergyStored(
				this, 0, 0, this.container().tile.getEnergyStorage()
		));
		this.addTab(new TabAugment(this, this.container()));
		this.addTab(new TabConfiguration(this, this.container().tile));
		this.addTab(new TabRedstone(this, this.container().tile));

	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

		int centerX = (this.width - this.xSize) / 2;
		int centerY = (this.height - this.ySize) / 2;

		UtilsFX.drawTag(centerX + 64, centerY + 48, Aspect.AIR, 0f, 0, this.zLevel);

	}

}
