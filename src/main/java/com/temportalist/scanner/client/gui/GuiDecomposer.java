package com.temportalist.scanner.client.gui;

import cofh.core.gui.element.TabAugment;
import cofh.lib.gui.GuiBase;
import cofh.lib.gui.element.ElementEnergyStored;
import com.temportalist.scanner.common.inventory.ContainerDecomposer;

/**
 * @author TheTemportalist
 */
public class GuiDecomposer extends GuiBase {

	public GuiDecomposer(ContainerDecomposer container) {
		super(container);
	}

	private ContainerDecomposer container() {
		return (ContainerDecomposer)this.inventorySlots;
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
