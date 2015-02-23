package com.temportalist.scanner.common;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import thaumcraft.api.aspects.Aspect;

import java.util.HashMap;

/**
 * @author TheTemportalist
 */
@Mod(modid = Scanner.MODID, name = "Scanner", version = "1.0")
public class Scanner {

	public static final String MODID = "scanner";

	@Mod.Instance("scanner")
	public static Scanner instance;

	@SidedProxy(
			clientSide = "com.temportalist.scanner.client.ProxyClient",
			serverSide = "com.temportalist.scanner.common.ProxyCommon"
	)
	public static ProxyCommon proxy;

	public Block decomposer;
	public final HashMap<Aspect, Integer> aspectTiers = new HashMap<Aspect, Integer>();
	/**
	 * Aspect Tier -> [Energy taken, Time taken]
	 */
	public final HashMap<Integer, int[]> decompositionStats = new HashMap<Integer, int[]>();

	@Mod.EventHandler
	public void preinit(FMLPreInitializationEvent event) {

		Scanner.proxy.register();
		NetworkRegistry.INSTANCE.registerGuiHandler(Scanner.instance, Scanner.proxy);

		this.decomposer = new BlockDecomposer(Material.rock, Scanner.MODID, "decomposer");

		// todo use config
		this.decompositionStats.put(1, new int[] {05, 10});
		this.decompositionStats.put(2, new int[] {10, 15});
		this.decompositionStats.put(3, new int[] {15, 20});

	}

	@Mod.EventHandler
	public void postInit (FMLPostInitializationEvent event) {

		this.aspectTiers.put(Aspect.AIR, 1);
		this.aspectTiers.put(Aspect.EARTH, 1);
		this.aspectTiers.put(Aspect.FIRE, 1);
		this.aspectTiers.put(Aspect.WATER, 1);
		this.aspectTiers.put(Aspect.ORDER, 1);
		this.aspectTiers.put(Aspect.ENTROPY, 1);

		this.aspectTiers.put(Aspect.VOID, 2);
		this.aspectTiers.put(Aspect.LIGHT, 2);
		this.aspectTiers.put(Aspect.WEATHER, 2);
		this.aspectTiers.put(Aspect.MOTION, 2);
		this.aspectTiers.put(Aspect.COLD, 2);
		this.aspectTiers.put(Aspect.CRYSTAL, 2);
		this.aspectTiers.put(Aspect.LIFE, 2);
		this.aspectTiers.put(Aspect.POISON, 2);
		this.aspectTiers.put(Aspect.ENERGY, 2);
		this.aspectTiers.put(Aspect.EXCHANGE, 2);

	}

}
