package com.temportalist.scanner.common;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * @author TheTemportalist
 */
public enum EnumDecomposerSide {

	EMPTY("blank"),
	INPUT_ITEM("blue"),
	OUTPUT("red"),
	OUTPUT_VIS("yellow"),
	OUTPUT_ESSENTIA("purple");

	private final String name;
	@SideOnly(Side.CLIENT)
	private IIcon icons[] = new IIcon[4];

	private EnumDecomposerSide(String name) {
		this.name = name;
	}

	@SideOnly(Side.CLIENT)
	public void registerIcon(IIconRegister reg, String base) {
		for (int i = 0; i < 3; i++) {
			this.icons[i] = reg.registerIcon(base + "/side" + i + "/colours/" + this.name);
		}
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side) {
		return this.icons[side > 1 ? 2 : side];
	}

}
