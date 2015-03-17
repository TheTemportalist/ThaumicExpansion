package com.temportalist.thaumicexpansion.common;

import cofh.lib.util.ArrayHashList;
import cofh.lib.util.helpers.AugmentHelper;
import cofh.thermalexpansion.block.simple.BlockFrame;
import cofh.thermalexpansion.item.TEItems;
import cofh.thermalexpansion.util.crafting.RecipeMachineUpgrade;
import cofh.thermalexpansion.util.crafting.TECraftingHandler;
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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;

import java.io.File;
import java.util.*;

/**
 * @author TheTemportalist
 */
@Mod(modid = TEC.MODID, name = "Thaumic Expansion", version = "1.0",
		dependencies = "required-after:Thaumcraft@[4.2,);" +
				"required-after:CoFHCore@[1.7.10R3.0.0RC7,);" +
				"required-after:ThermalFoundation@[1.7.10R1.0.0RC7,);" +
				"required-after:ThermalExpansion@[1.7.10R4.0.0RC7,);"
)
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

	public static final List<UUID> onlinePlayers = new ArrayList<UUID>();
	// todo: flaw: both of these 2 need to be saved to disk
	public static final HashMap<UUID, String> idToUsername = new HashMap<UUID, String>();
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

		this.playerTracker = new ItemAugment(TEC.MODID, "playerTracker") {
			@Override
			@SideOnly(Side.CLIENT)
			public void addInformation(ItemStack stack, EntityPlayer player,
					List list, boolean something) {
				if (stack.hasTagCompound()) {
					list.add("Player: " + (
							stack.getTagCompound().hasKey("playerName") ?
									stack.getTagCompound().getString("playerName") :
									TEC.idToUsername.get(
											UUID.fromString(
													stack.getTagCompound().getString("playerUUID")
											)
									)
					));
				}
			}

			@Override
			public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
				if (player.isSneaking()) {
					TEC.setPlayerForTracker(stack, player);
					// have to do this because mc doesnt check tags
					player.setCurrentItemOrArmor(0, stack);
				}
				return stack;
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

		this.thaumicAnalyzer = new BlockThaumicAnalyzer(TEC.MODID, "thaumicAnalyzer");
		this.thaumicAnalyzer.setCreativeTab(CreativeTabs.tabRedstone);

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

		ThaumcraftApi.registerObjectTag(new ItemStack(this.thaumicAnalyzer), new int[] {
						0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
				}, new AspectList().
						add(Aspect.MIND, 4).
						add(Aspect.METAL, 2).
						add(Aspect.MECHANISM, 10).
						add(Aspect.MAGIC, 7)
		);

		ItemStack[] augments = new ItemStack[] { new ItemStack(this.playerTracker) };
		for (int tier = 0; tier < TEC.machines.length; tier++) {
			TEC.machines[tier] = new ItemStack(
					this.thaumicAnalyzer, 1,
					this.thaumicAnalyzer.getMetadata(tier, ForgeDirection.EAST)
			);
			AugmentHelper.writeAugments(TEC.machines[tier], augments);
		}

		@SuppressWarnings("unchecked")
		Pair<ItemStack, ItemStack>[] machineRecipeKeys = new Pair[] {
				new Pair<ItemStack, ItemStack>(TEC.machines[0], BlockFrame.frameMachineBasic),
				new Pair<ItemStack, ItemStack>(TEC.machines[1], BlockFrame.frameMachineHardened),
				new Pair<ItemStack, ItemStack>(TEC.machines[2], BlockFrame.frameMachineReinforced),
				new Pair<ItemStack, ItemStack>(TEC.machines[3], BlockFrame.frameMachineResonant)
		};
		List<IRecipe> machineRecipes = new ArrayHashList<IRecipe>();
		for (Pair<ItemStack, ItemStack> pair : machineRecipeKeys) {
			machineRecipes.add(new ShapedOreRecipe(pair.getKey(),
					"iti", " f ", "iri",
					'i', "ingotThaumium", 't', ConfigItems.itemThaumometer,
					'f', pair.getValue(), 'r', TEItems.powerCoilGold
			));
			TECraftingHandler.addSecureRecipe(pair.getKey());
		}

		@SuppressWarnings("unchecked")
		Pair<String, String>[] upgradeThings = new Pair[] {
				new Pair<String, String>("ingotThaumium", "gearGold"),
				new Pair<String, String>("ingotInvar", "gearSilver"),
				new Pair<String, String>("blockGlassHardened", "gearPlatinum"),
				new Pair<String, String>("ingotEnderium", "gearEnderium")
		};
		for (int i = 1; i < TEC.machines.length; i++) {
			machineRecipes.add(new RecipeMachineUpgrade(i, TEC.machines[i], new Object[] {
					"a a", " l ", "aba",
					'a', upgradeThings[i].getKey(), 'b', upgradeThings[i].getValue(),
					'l', TEC.machines[i - 1]
			}));
		}

		String category = "THAUMICEXPANSION";
		ResearchCategories.registerCategory(category,
				new ResourceLocation(TEC.MODID, "textures/items/opticalScanner.png"),
				new ResourceLocation("thaumcraft", "textures/gui/gui_researchback.png")
		);

		this.addResearchAndRecipe(
				new ResearchItem("THAUMICANALYZER", category,
						new AspectList(),
						0, 0, 0, TEC.machines[0]
				).setParents("GOGGLES"),
				machineRecipes.toArray(new IRecipe[machineRecipes.size()]), // todo helper method
				new ResearchPage("tc.research_page.THAUMICANALYZER.1")
		);

		this.addResearchAndRecipe(
				new ResearchItem("PLAYERTRACKER", category,
						new AspectList(),
						-2, 0, 0, new ItemStack(this.playerTracker)
				).setParents("THAUMICANALYZER"),
				new IRecipe[] {
						new ShapedOreRecipe(this.playerTracker,
								"   ", " h ", "   ", 'h', new ItemStack(Items.skull)
						)
				}, new ResearchPage("tc.research_page.PLAYERTRACKER.1")
		);

		this.addResearchAndRecipe(
				new ResearchItem("DECOMPOSER", category,
						new AspectList(),
						-1, -2, 0, new ItemStack(this.decomposerUpgrade)
				).setParents("THAUMICANALYZER", "DECONSTRUCTOR"),
				new IRecipe[] {
						new ShapedOreRecipe(this.decomposerUpgrade,
								"   ", " d ", "   ",
								'd', new ItemStack(ConfigBlocks.blockTable, 1, 14)
						)
				}, new ResearchPage("tc.research_page.DECOMPOSER.1")
		);

		this.addResearchAndRecipe(
				new ResearchItem("ITEMKEEPER", category,
						new AspectList(),
						1, -2, 0, new ItemStack(this.itemKeeper)
				).setParents("THAUMICANALYZER"),
				new IRecipe[] {
						new ShapedOreRecipe(this.itemKeeper,
								"i i", "ici", " i ",
								'i', "ingotThaumium", 'c', Blocks.chest
						)
				}, new ResearchPage("tc.research_page.ITEMKEEPER.1")
		);

		this.addResearchAndRecipe(
				new ResearchItem("THAUMICADJUSTER", category,
						new AspectList(),
						2, 0, 0, new ItemStack(this.thaumicAdjuster)
				).setParents("THAUMICANALYZER"),
				new IRecipe[] {
						new ShapedOreRecipe(this.thaumicAdjuster,
								"i i", "eje", " e ",
								'i', "ingotThaumium", 'j', new ItemStack(ConfigBlocks.blockJar),
								'e', "ingotEnderium"
						)
				}, new ResearchPage("tc.research_page.THAUMICADJUSTER.1")
		);

	}

	private void addResearchAndRecipe(ResearchItem research, IRecipe[] recipes,
			ResearchPage... pages) {
		List<ResearchPage> allPages = new ArrayList<ResearchPage>(Arrays.asList(pages));
		for (IRecipe recipe : recipes) {
			GameRegistry.addRecipe(recipe);
			allPages.add(new ResearchPage(recipe));
		}
		research.setPages(
				allPages.toArray(new ResearchPage[allPages.size()])
		).registerResearchItem();
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

	public static boolean hasScannedOffline(UUID playerID, ItemStack stack) {
		List<Pair<ScanResult, Pair<Double, Double>>> offlineScans = TEC.aspectBuffer.get(playerID);
		if (offlineScans != null) {
			for (Pair<ScanResult, Pair<Double, Double>> scanPair : offlineScans) {
				if (scanPair.getKey().id == Item.getIdFromItem(stack.getItem()) &&
						scanPair.getKey().meta == stack.getItemDamage()) {
					return true;
				}
			}
		}
		return false;
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

	public static void setPlayerForTracker(ItemStack stack, EntityPlayer player) {
		NBTTagCompound stackTag = stack.hasTagCompound() ?
				stack.getTagCompound() :
				new NBTTagCompound();
		UUID id = player.getGameProfile().getId();
		TEC.idToUsername.put(id, player.getCommandSenderName());
		stackTag.setString("playerUUID", id.toString());
		stackTag.setString("playerName", player.getCommandSenderName());
		stack.setTagCompound(stackTag);
	}

	public static UUID getUUIDForPlayerTracker(ItemStack stack) {
		try {
			return UUID.fromString(stack.getTagCompound().getString("playerUUID"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SubscribeEvent
	public void saveWorld(WorldEvent.Save event) {
		if (event.world.provider.dimensionId == 0 && event.world instanceof WorldServer) {
			MapStorage storage = DimensionManager.getWorld(0).mapStorage;
			if (storage != null) {
				/*
				TECData data = (TECData)storage.loadData(TECData.class, "TECData");
				if (data == null) {
					data = new TECData();
					data.markDirty();
				}
				*/
			}
		}
	}

	@SubscribeEvent
	public void loadWorld(WorldEvent.Load event) {
		if (event.world.provider.dimensionId == 0 && event.world instanceof WorldServer) {
			MapStorage storage = DimensionManager.getWorld(0).mapStorage;
			if (storage != null) {
				TECData data = (TECData) storage.loadData(TECData.class, "TECData");
				if (data == null) {
					data = new TECData();
					data.markDirty();
				}
			}
		}
	}

	private static class TECData extends WorldSavedData {

		private TECData() {
			super("TECData");
		}

		@Override
		public void writeToNBT(NBTTagCompound tagCom) {
			NBTTagList idUsernameTag = new NBTTagList();
			for (Map.Entry<UUID, String> entry : TEC.idToUsername.entrySet()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString("playerName", entry.getValue());
				tag.setString("UUID", entry.getKey().toString());
				idUsernameTag.appendTag(tag);
			}
			tagCom.setTag("idUsernames", idUsernameTag);

			NBTTagList bufferTag = new NBTTagList();
			for (Map.Entry<UUID, List<Pair<ScanResult, Pair<Double, Double>>>> entry :
					TEC.aspectBuffer.entrySet()) {
				NBTTagCompound idedBuffer = new NBTTagCompound();
				idedBuffer.setString("playerID", entry.getKey().toString());
				NBTTagList playersBuffer = new NBTTagList();
				for (Pair<ScanResult, Pair<Double, Double>> pair : entry.getValue()) {
					NBTTagCompound bufferEntry = new NBTTagCompound();

					NBTTagCompound scan = new NBTTagCompound();
					scan.setByte("type", pair.getKey().type);
					scan.setInteger("id", pair.getKey().id);
					scan.setInteger("meta", pair.getKey().meta);
					NBTTagCompound entityTag = new NBTTagCompound();
					pair.getKey().entity.writeToNBTOptional(entityTag);
					scan.setTag("entityTag", entityTag);
					scan.setString("phenomena", pair.getKey().phenomena);
					bufferEntry.setTag("scan", scan);

					bufferEntry.setDouble("chance", pair.getValue().getKey());
					bufferEntry.setDouble("percent", pair.getValue().getValue());

					playersBuffer.appendTag(bufferEntry);
				}
				idedBuffer.setTag("buffer", playersBuffer);
				bufferTag.appendTag(idedBuffer);
			}
			tagCom.setTag("bufferTag", bufferTag);

		}

		@Override
		public void readFromNBT(NBTTagCompound tagCom) {
			TEC.idToUsername.clear();
			NBTTagList idUsernameTag = tagCom.getTagList("idUsernames", 10);
			for (int i = 0; i < idUsernameTag.tagCount(); i++) {
				NBTTagCompound tag = idUsernameTag.getCompoundTagAt(i);
				TEC.idToUsername.put(
						UUID.fromString(tagCom.getString("UUID")), tagCom.getString("playerName")
				);
			}

			TEC.aspectBuffer.clear();
			NBTTagList bufferTag = tagCom.getTagList("bufferTag", 10);
			for (int i = 0; i < bufferTag.tagCount(); i++) {
				NBTTagCompound idedBuffer = bufferTag.getCompoundTagAt(i);
				UUID playerID = UUID.fromString(idedBuffer.getString("playerID"));
				NBTTagList playersBuffer = idedBuffer.getTagList("buffer", 10);
				for (int j = 0; j < playersBuffer.tagCount(); j++) {
					NBTTagCompound bufferEntry = playersBuffer.getCompoundTagAt(j);

					NBTTagCompound scanTag = bufferEntry.getCompoundTag("scan");
					ScanResult scan = new ScanResult(
							scanTag.getByte("type"),
							scanTag.getInteger("id"),
							scanTag.getInteger("meta"),
							EntityList.createEntityFromNBT(
									scanTag.getCompoundTag("entityTag"),
									DimensionManager.getWorld(0)
							),
							scanTag.getString("phenomena")
					);

					TEC.addAspect(playerID, scan,
							bufferEntry.getDouble("chance"), bufferEntry.getDouble("percent"));

				}

			}

		}

	}

}
