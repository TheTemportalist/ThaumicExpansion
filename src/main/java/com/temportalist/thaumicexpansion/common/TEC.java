package com.temportalist.thaumicexpansion.common;

import com.temportalist.thaumicexpansion.common.block.BlockThaumicAnalyzer;
import com.temportalist.thaumicexpansion.common.item.ItemAugment;
import com.temportalist.thaumicexpansion.common.packet.PacketRecieveAspect;
import com.temportalist.thaumicexpansion.common.packet.PacketTileSync;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;
import thaumcraft.api.aspects.Aspect;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author TheTemportalist
 */
@Mod(modid = TEC.MODID, name = "Thaumic Expansion", version = "1.0")
public class TEC {

	public static final String MODID = "thaumicexpansion";

	@Mod.Instance(TEC.MODID)
	public static TEC instance;

	@SidedProxy(
			clientSide = "com.temportalist.thaumicexpansion.client.ProxyClient",
			serverSide = "com.temportalist.thaumicexpansion.common.ProxyCommon"
	)
	public static ProxyCommon proxy;

	public Block thaumicAnalyzer;
	public Item playerHolder, decomposerUpgrade, itemKeeper, thaumicAdjuster;

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

		TEC.proxy.register();
		NetworkRegistry.INSTANCE.registerGuiHandler(TEC.instance, TEC.proxy);
		PacketTileSync.init();
		PacketRecieveAspect.init();

		this.initConfig(event.getModConfigurationDirectory());

		this.thaumicAnalyzer = new BlockThaumicAnalyzer(
				Material.rock, TEC.MODID, "thaumicAnalyzer"
		);
		this.thaumicAnalyzer.setCreativeTab(CreativeTabs.tabRedstone);

		this.playerHolder = new ItemAugment(TEC.MODID, "playerTracker");
		this.playerHolder.setCreativeTab(CreativeTabs.tabRedstone);
		this.decomposerUpgrade = new ItemAugment(TEC.MODID, "decomposer");
		this.decomposerUpgrade.setCreativeTab(CreativeTabs.tabRedstone);
		this.itemKeeper = new ItemAugment(TEC.MODID, "itemKeeper");
		this.itemKeeper.setCreativeTab(CreativeTabs.tabRedstone);
		this.thaumicAdjuster = new ItemAugment(TEC.MODID, "thaumicAdjuster");
		this.thaumicAdjuster.setCreativeTab(CreativeTabs.tabRedstone);

		// todo use config
		TEC.decompositionStats.put(1, new int[] { 05, 10 });
		TEC.decompositionStats.put(2, new int[] { 10, 15 });
		TEC.decompositionStats.put(3, new int[] { 15, 20 });

	}

	private void initConfig(File dir) {
		Configuration config = new Configuration(new File(dir, "Decomposer.cfg"));
		config.load();

		TEC.maxEnergyStorage = config.get(
				"general", "max energy storage", TEC.maxEnergyStorage
		).getInt();
		TEC.consumeItems = config.get(
				"general", "consume items", TEC.consumeItems
		).getBoolean();

		if (config.hasChanged())
			config.save();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		TEC.aspectTiers.put(Aspect.AIR, 1);
		TEC.aspectTiers.put(Aspect.EARTH, 1);
		TEC.aspectTiers.put(Aspect.FIRE, 1);
		TEC.aspectTiers.put(Aspect.WATER, 1);
		TEC.aspectTiers.put(Aspect.ORDER, 1);
		TEC.aspectTiers.put(Aspect.ENTROPY, 1);

		TEC.aspectTiers.put(Aspect.VOID, 2);
		TEC.aspectTiers.put(Aspect.LIGHT, 2);
		TEC.aspectTiers.put(Aspect.WEATHER, 2);
		TEC.aspectTiers.put(Aspect.MOTION, 2);
		TEC.aspectTiers.put(Aspect.COLD, 2);
		TEC.aspectTiers.put(Aspect.CRYSTAL, 2);
		TEC.aspectTiers.put(Aspect.LIFE, 2);
		TEC.aspectTiers.put(Aspect.POISON, 2);
		TEC.aspectTiers.put(Aspect.ENERGY, 2);
		TEC.aspectTiers.put(Aspect.EXCHANGE, 2);

	}

	public static String getFullName(ItemStack itemStack) {
		if (itemStack == null)
			return null;
		String name;
		if (Block.getBlockFromItem(itemStack.getItem()) == Blocks.air) {
			name = GameData.getItemRegistry().getNameForObject(itemStack.getItem());
		}
		else {
			name = GameData.getBlockRegistry().getNameForObject(
					Block.getBlockFromItem(
							itemStack.getItem()));
		}
		return name + ":" + itemStack.getItemDamage();
	}

	public static EntityPlayer getPlayerOnline(UUID uuid) {
		EntityPlayer player = null;
		for (Object obj :
				MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
			if (obj instanceof EntityPlayer) {
				if (((EntityPlayer) obj).getUniqueID().equals(uuid)) {
					player = (EntityPlayer) obj;
					break;
				}
			}
		}
		return player;
	}

}
