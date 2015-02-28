package com.temportalist.thaumicexpansion.common;

import cofh.thermalexpansion.item.TEItems;
import cofh.thermalexpansion.util.crafting.RecipeMachine;
import cofh.thermalexpansion.util.crafting.RecipeMachineUpgrade;
import com.temportalist.thaumicexpansion.common.block.BlockThaumicAnalyzer;
import com.temportalist.thaumicexpansion.common.item.ItemAugment;
import com.temportalist.thaumicexpansion.common.lib.Pair;
import com.temportalist.thaumicexpansion.common.packet.PacketRecieveAspect;
import com.temportalist.thaumicexpansion.common.packet.PacketTileSync;
import com.temportalist.thaumicexpansion.server.CommandTEC;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.ShapedOreRecipe;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	public BlockThaumicAnalyzer thaumicAnalyzer;
	public Item playerTracker, decomposerUpgrade, itemKeeper, thaumicAdjuster;

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

	public static ItemStack[] machines = new ItemStack[4];

	public static int maxEnergyStorage = 8000;
	public static boolean consumeItems = true;

	public static final HashMap<UUID, String> idToUsername = new HashMap<UUID, String>();
	public static final List<UUID> onlinePlayers = new ArrayList<UUID>();
	private static final HashMap<UUID, List<Pair<ScanResult, Pair<Double, Double>>>> aspectBuffer =
			new HashMap<UUID, List<Pair<ScanResult, Pair<Double, Double>>>>();

	@Mod.EventHandler
	public void preinit(FMLPreInitializationEvent event) {

		TEC.proxy.register();
		NetworkRegistry.INSTANCE.registerGuiHandler(TEC.instance, TEC.proxy);
		PacketTileSync.init();
		PacketRecieveAspect.init();
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);

		this.initConfig(event.getModConfigurationDirectory());

		this.thaumicAnalyzer = new BlockThaumicAnalyzer(
				Material.rock, TEC.MODID, "thaumicAnalyzer"
		);
		this.thaumicAnalyzer.setCreativeTab(CreativeTabs.tabRedstone);

		this.playerTracker = new ItemAugment(TEC.MODID, "playerTracker") {
			@Override
			@SideOnly(Side.CLIENT)
			public void addInformation(ItemStack stack, EntityPlayer player,
					List list, boolean something) {
				if (stack.hasTagCompound()) {
					list.add("Player UUID: " + stack.getTagCompound().getString("playerUUID"));
				}
			}
		};
		this.playerTracker.setCreativeTab(CreativeTabs.tabRedstone);
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

		NBTTagCompound blockStackTag = new NBTTagCompound();
		NBTTagList augmentList = new NBTTagList(); // todo nbt helper to pass a string and ... params for key in
		NBTTagCompound augmentTag = new NBTTagCompound();
		augmentTag.setInteger("Slot", 0);
		new ItemStack(this.playerTracker, 1, 0).writeToNBT(augmentTag);
		augmentList.appendTag(augmentTag);
		blockStackTag.setTag("Augments", augmentList);
		for (int i = 0; i < TEC.machines.length; i++) {
			TEC.machines[i] = new ItemStack(
					this.thaumicAnalyzer, 1,
					this.thaumicAnalyzer.getMetadata(i, ForgeDirection.EAST)
			);
			TEC.machines[i].setTagCompound(blockStackTag);
		}

		ItemStack[] augments = new ItemStack[] { new ItemStack(this.playerTracker) };
		GameRegistry.addRecipe(new RecipeMachine(TEC.machines[0], augments, new Object[] {
				"iti", " f ", "iri",
				'i', "ingotThaumium", 't', ConfigItems.itemThaumometer,
				'f', "thermalexpansion:machineFrame", 'r', TEItems.powerCoilGold
		}) {
			@Override
			public ItemStack getCraftingResult(InventoryCrafting inv) {
				ItemStack stack = super.getCraftingResult(inv);
				byte level = stack.getTagCompound().getByte("Level");
				stack.getTagCompound().removeTag("Level");
				stack.setItemDamage(TEC.instance.thaumicAnalyzer.getMetadata(
						level, ForgeDirection.EAST
				));
				return stack;
			}
		});
		@SuppressWarnings("unchecked")
		Pair<String, String>[] upgradeThings = new Pair[] {
				new Pair<String, String>("ingotThaumium", "gearGold"),
				new Pair<String, String>("ingotInvar", "gearSilver"),
				new Pair<String, String>("blockGlassHardened", "gearPlatinum"),
				new Pair<String, String>("ingotEnderium", "gearEnderium")
		};
		for (int i = 1; i < TEC.machines.length; i++) {
			GameRegistry.addRecipe(new RecipeMachineUpgrade(i, TEC.machines[i], new Object[] {
					"a a", " l ", "aba",
					'a', upgradeThings[i].getKey(), 'b', upgradeThings[i].getValue(),
					'l', TEC.machines[i - 1]
			}));
		}

		GameRegistry.addRecipe(new ShapedOreRecipe(this.playerTracker,
				"   ", " h ", "   ", 'h', Blocks.skull
		));

		GameRegistry.addRecipe(new ShapedOreRecipe(this.decomposerUpgrade,
				"   ", " d ", "   ", 'd', new ItemStack(ConfigBlocks.blockTable, 1, 14)
		));

		GameRegistry.addRecipe(new ShapedOreRecipe(this.itemKeeper,
				"i i", "ici", " i ",
				'i', "ingotThaumium", 'c', Blocks.chest
		));

		GameRegistry.addRecipe(new ShapedOreRecipe(this.thaumicAdjuster,
				"i i", "eje", " e ",
				'i', "ingotThaumium", 'j', ConfigBlocks.blockJar, 'e', "ingotEnderium"
		));

	}

	@Mod.EventHandler
	public void serverStart(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandTEC());
	}

	@SubscribeEvent
	public void login(PlayerEvent.PlayerLoggedInEvent event) {
		UUID id = event.player.getGameProfile().getId();
		TEC.idToUsername.put(id, event.player.getCommandSenderName());
		TEC.onlinePlayers.add(id);
		if (TEC.aspectBuffer.containsKey(id)) {
			List<Pair<ScanResult, Pair<Double, Double>>> pendingScans = TEC.aspectBuffer.get(id);
			for (Pair<ScanResult, Pair<Double, Double>> scan : pendingScans) {
				TEC.addAspect(event.player, scan.getKey(), scan.getValue().getKey(),
						scan.getValue().getValue());
			}
			TEC.aspectBuffer.remove(id);
		}
	}

	@SubscribeEvent
	public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
		TEC.onlinePlayers.remove(event.player.getGameProfile().getId());
	}

	public static void addAspect(UUID playerUUID, ScanResult scan, double aspectChance,
			double aspectPercentageOnChance) {
		if (TEC.onlinePlayers.contains(playerUUID))
			TEC.addAspect(TEC.getPlayerOnline(playerUUID), scan, aspectChance,
					aspectPercentageOnChance);
		else {
			if (!TEC.aspectBuffer.containsKey(playerUUID))
				TEC.aspectBuffer.put(
						playerUUID, new ArrayList<Pair<ScanResult, Pair<Double, Double>>>()
				);
			TEC.aspectBuffer.get(playerUUID).add(new Pair<ScanResult, Pair<Double, Double>>(
					scan,
					new Pair<Double, Double>(
							aspectChance, aspectPercentageOnChance
					)
			));
		}
	}

	private static void addAspect(EntityPlayer player, ScanResult scan, double aspectChance,
			double aspectPercentageOnChance) {
		Thaumcraft.proxy.getResearchManager().completeScannedObject(
				player, "@" + ScanManager.generateItemHash(Item.getItemById(scan.id), scan.meta)
		);
		AspectList aspects = ThaumcraftCraftingManager.getObjectTags(
				new ItemStack(Item.getItemById(scan.id), 1, scan.meta)
		);
		if (aspects != null) {
			PlayerKnowledge pk = Thaumcraft.proxy.getPlayerKnowledge();
			for (Aspect aspect : aspects.getAspects()) {
				if (pk.hasDiscoveredParentAspects(player.getCommandSenderName(), aspect)) {
					int amt = aspects.getAmount(aspect);
					if (aspectChance > 0d &&
							player.getEntityWorld().rand.nextDouble() < aspectChance)
						amt = MathHelper
								.ceiling_double_int((double) amt * aspectPercentageOnChance);
					ScanManager.checkAndSyncAspectKnowledge(player, aspect, amt);
				}
			}
		}
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
		if (uuid == null)
			System.out.println("null uuid");
		EntityPlayer player = null;
		for (Object obj :
				MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
			if (obj instanceof EntityPlayer) {
				if (((EntityPlayer) obj).getGameProfile().getId().equals(uuid)) {
					player = (EntityPlayer) obj;
					break;
				}
			}
		}
		return player;
	}

	public static boolean canInsertBIntoA(ItemStack a, ItemStack b) {
		return a == null || (
				b != null
						&& a.getItem() == b.getItem()
						&& a.getItemDamage() == b.getItemDamage()
						&& ItemStack.areItemStackTagsEqual(a, b)
						&& a.stackSize + b.stackSize <= a.getMaxStackSize()
		);
	}

}
