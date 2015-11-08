package com.temportalist.thaumicexpansion.common.lib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * @author TheTemportalist
 */
public enum EnumSideTA {

	/*
		permutations:
			blank - default side
			INPUT & OUTPUT - empty
			INPUT - blue
			OUTPUT ALL - orange
			OUTPUT Thaumcraft - purple
			OUTPUT essentia - green
			OUTPUT vis - yellow
			OUTPUT items - red
	 */

	NONE("default"),
	IO("io"),
	INPUT_ITEM("blue"),
	OUTPUT("orange"),
	OUTPUT_ITEM("red"),
	OUTPUT_THAUM("purple"),
	OUTPUT_VIS("yellow"),
	OUTPUT_ESSENTIA("green");

	private final String name;
	@SideOnly(Side.CLIENT)
	private IIcon[] icons = new IIcon[4];

	private EnumSideTA(String name) {
		this.name = name;
	}

	@SideOnly(Side.CLIENT)
	public void registerIcon(IIconRegister reg, String base) {
		for (int i = 0; i < (this == EnumSideTA.NONE ? 4 : 3); i++) {
			this.icons[i] = reg.registerIcon(base + "/side" + i + "/colours/" + this.name);
		}
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, boolean isFront) {
		if (isFront)
			side = 3;
		else if (side > 1)
			side = 2;
		return this.icons[side];
	}

	public EnumSideTA next() {
		int i = this.ordinal() + 1;
		if (i >= EnumSideTA.values().length)
			i = 0;
		return EnumSideTA.values()[i];
	}

	public EnumSideTA last() {
		int i = this.ordinal() - 1;
		if (i < 0)
			i = EnumSideTA.values().length - 1;
		return EnumSideTA.values()[i];
	}

	public static int[][] getSlotGroups() {
		return new int[][] {
				new int[] { },
				new int[] { 0, 1, 2 },
				new int[] { 0, 1 },
				new int[] { 2 },
				new int[] { 2 },
				new int[] { },
				new int[] { },
				new int[] { }
		};
	}

	public static boolean[] getInputs() {
		return new boolean[] {
				false,
				true, true,
				false, false, false, false, false
		};
	}

	public static boolean[] getOutputs() {
		return new boolean[] {
				false,
				true, false,
				true, true, false, false, false
		};
	}

	public static byte[] getDefaultSiding() {
		return new byte[] {
				0, 0, 0, 0, 0, 0, 0
		};
	}

}
