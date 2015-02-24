package com.temportalist.scanner.common;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import thaumcraft.api.aspects.Aspect;

import java.io.File;
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
	public static final HashMap<Aspect, Integer> aspectTiers = new HashMap<Aspect, Integer>();
	/**
	 * Aspect Tier -> [Energy taken, Time taken]
	 */
	public static final HashMap<Integer, int[]> decompositionStats = new HashMap<Integer, int[]>();
	/**
	 * Key -> machine tier
	 * Value ->
	 * Key -> aspect complexity
	 * Value -> primary check for type of aspect
	 */
	public static final double[][] complexityTierChance = new double[][] {
			new double[] { 1, .5, .25 },
			new double[] { 1, .7, .45 },
			new double[] { 1, .9, .65 },
			new double[] { 1, .9, .85 }
	};
	/**
	 * Key -> machine tier
	 * Value -> second check for each aspect
	 */
	public static final double[] tieredChance = new double[] { .4, .6, .8, 1 };

	public static int maxEnergyStorage = 8000;
	public static boolean consumeItems = true;

	@Mod.EventHandler
	public void preinit(FMLPreInitializationEvent event) {

		Scanner.proxy.register();
		NetworkRegistry.INSTANCE.registerGuiHandler(Scanner.instance, Scanner.proxy);
		PacketTileSync.init();
		PacketRecieveAspect.init();

		this.initConfig(event.getModConfigurationDirectory());

		this.decomposer = new BlockDecomposer(Material.rock, Scanner.MODID, "decomposer");

		// todo use config
		Scanner.decompositionStats.put(1, new int[] { 05, 10 });
		Scanner.decompositionStats.put(2, new int[] { 10, 15 });
		Scanner.decompositionStats.put(3, new int[] { 15, 20 });

	}

	private void initConfig(File dir) {
		Configuration config = new Configuration(new File(dir, "Decomposer.cfg"));
		config.load();

		Scanner.maxEnergyStorage = config.get(
				"general", "max energy storage", Scanner.maxEnergyStorage
		).getInt();
		Scanner.consumeItems = config.get(
				"general", "consume items", Scanner.consumeItems
		).getBoolean();

		if (config.hasChanged())
			config.save();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		Scanner.aspectTiers.put(Aspect.AIR, 1);
		Scanner.aspectTiers.put(Aspect.EARTH, 1);
		Scanner.aspectTiers.put(Aspect.FIRE, 1);
		Scanner.aspectTiers.put(Aspect.WATER, 1);
		Scanner.aspectTiers.put(Aspect.ORDER, 1);
		Scanner.aspectTiers.put(Aspect.ENTROPY, 1);

		Scanner.aspectTiers.put(Aspect.VOID, 2);
		Scanner.aspectTiers.put(Aspect.LIGHT, 2);
		Scanner.aspectTiers.put(Aspect.WEATHER, 2);
		Scanner.aspectTiers.put(Aspect.MOTION, 2);
		Scanner.aspectTiers.put(Aspect.COLD, 2);
		Scanner.aspectTiers.put(Aspect.CRYSTAL, 2);
		Scanner.aspectTiers.put(Aspect.LIFE, 2);
		Scanner.aspectTiers.put(Aspect.POISON, 2);
		Scanner.aspectTiers.put(Aspect.ENERGY, 2);
		Scanner.aspectTiers.put(Aspect.EXCHANGE, 2);

	}

	public static String getFullName(ItemStack stack) {
		if (stack == null)
			return null;

		GameRegistry.UniqueIdentifier ui = new GameRegistry.UniqueIdentifier(
				Block.getBlockFromItem(stack.getItem()) == null ?
						GameData.getItemRegistry().getNameForObject(stack.getItem()) :
						GameData.getBlockRegistry().getNameForObject(
								Block.getBlockFromItem(stack.getItem())
						)
		);
		return ui.modId + ":" + ui.name + ":" + stack.getItemDamage();
	}

}
