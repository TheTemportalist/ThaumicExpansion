package com.temportalist.thaumicexp.common;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author TheTemportalist
 */
public enum Axis {

	HORIZONTAL,
	VERTICAL,
	X,
	Y,
	Z;

	private static Axis[] lateral = new Axis[]{Y, Y, Z, Z, X, X};

	public static Axis getLateral(ForgeDirection dir) {
		return Axis.lateral[dir.ordinal()];
	}

	public static void rotate(ForgeDirection from, ForgeDirection to) {
		

	}

}
