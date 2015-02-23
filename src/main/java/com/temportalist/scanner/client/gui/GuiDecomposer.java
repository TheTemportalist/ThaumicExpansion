package com.temportalist.scanner.client.gui;

import cofh.core.gui.GuiBaseAdv;
import cofh.core.gui.element.TabAugment;
import cofh.lib.gui.element.ElementEnergyStored;
import com.temportalist.scanner.common.Scanner;
import com.temportalist.scanner.common.inventory.ContainerDecomposer;
import net.minecraft.util.ResourceLocation;

/**
 * http://pastebin.com/ai8acZYP
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
						this, 0, 0, this.container().tile.getEnergyStorage())
		);
		this.addTab(new TabAugment(this, this.container()));

	}

}
