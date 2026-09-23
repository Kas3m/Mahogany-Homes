package thestonedturtle.mahoganyhomes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.VarbitComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GuidanceRegressionTest
{
	private Client client;
	private MahoganyHomesConfig config;
	private MahoganyHomesPlugin plugin;
	private ConfigManager configManager;
	private DialogueHighlighter highlighter;
	private List<Runnable> tickEnd;

	@Before
	public void setUp() throws Exception
	{
		client = mock(Client.class);
		config = mock(MahoganyHomesConfig.class);
		configManager = mock(ConfigManager.class);
		when(config.sessionTimeout()).thenReturn(5);
		when(config.highlightTeleports()).thenReturn(true);
		when(config.highlightContractorTiers()).thenReturn(true);
		when(config.highlightTabIcons()).thenReturn(true);
		when(config.highlightTeleportsColor()).thenReturn(Color.CYAN);
		when(config.postContractGuidance()).thenReturn(true);
		when(config.contractorMode()).thenReturn(ContractorMode.ALWAYS_AMY);
		when(config.displayHintArrows()).thenReturn(true);
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		when(client.getRealSkillLevel(Skill.CONSTRUCTION)).thenReturn(85);
		tickEnd = new ArrayList<>();
		final ClientThread clientThread = mock(ClientThread.class);
		doAnswer(invocation ->
		{
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
		doAnswer(invocation ->
		{
			tickEnd.add(invocation.getArgument(0));
			return null;
		}).when(clientThread).invokeAtTickEnd(any(Runnable.class));
		highlighter = new DialogueHighlighter(client);
		plugin = new MahoganyHomesPlugin();
		plugin.setClient(client);
		set("config", config);
		set("configManager", configManager);
		set("clientThread", clientThread);
		set("dialogueHighlighter", highlighter);
		set("overlayManager", mock(OverlayManager.class));
		set("worldMapPointManager", mock(WorldMapPointManager.class));
		plugin.startUp();
		flush();
		clearInvocations(client);
	}

	@Test
	public void shutdownClearsTemporaryStateWithoutErasingSavedTier() throws Exception
	{
		seedGuidance();
		set("lastContractTier", 3);
		plugin.shutDown();
		assertGuidanceCleared();
		assertEquals(0, plugin.getLastContractTier());
		verifyNoInteractions(configManager);
	}

	@Test
	public void disabledContractorModeClearsGuidanceImmediately() throws Exception
	{
		seedGuidance();
		when(config.contractorMode()).thenReturn(ContractorMode.DISABLED);
		change(MahoganyHomesConfig.CONTRACTOR_MODE_KEY);
		assertGuidanceCleared();
	}

	@Test
	public void disabledPostContractToggleClearsGuidanceImmediately() throws Exception
	{
		seedGuidance();
		when(config.postContractGuidance()).thenReturn(false);
		change(MahoganyHomesConfig.POST_CONTRACT_KEY);
		assertGuidanceCleared();
	}

	@Test
	public void disablingPostContractGuidancePreservesActiveHouse() throws Exception
	{
		seedGuidance();
		set("currentHome", Home.BOB);
		final TeleportItem teleport = plugin.getTeleportItem();
		when(config.postContractGuidance()).thenReturn(false);
		change(MahoganyHomesConfig.POST_CONTRACT_KEY);
		assertEquals(Home.BOB, plugin.getCurrentHome());
		assertSame(teleport, plugin.getTeleportItem());
	}

	@Test
	public void savedTierIsReloadedAfterRestart() throws Exception
	{
		set("lastContractTier", 3);
		when(configManager.getConfiguration(MahoganyHomesConfig.GROUP_NAME + ".0", MahoganyHomesConfig.LAST_TIER_KEY)).thenReturn("3");
		plugin.shutDown();
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		plugin.startUp();
		assertEquals(3, plugin.getLastContractTier());
	}

	@Test
	public void missingOrInvalidSavedTierDoesNotLeakPreviousPreference() throws Exception
	{
		for (final String saved : new String[]{null, "bad", "0", "5"})
		{
			set("lastContractTier", 3);
			when(configManager.getConfiguration(anyString(), eq(MahoganyHomesConfig.LAST_TIER_KEY))).thenReturn(saved);
			plugin.onUsernameChanged(new UsernameChanged());
			assertEquals(0, plugin.getLastContractTier());
			assertEquals(4, plugin.getTargetContractTier());
		}
	}

	@Test
	public void hintArrowCanBeRecreatedAtTheSameDestinationAfterToggle() throws Exception
	{
		final WorldPoint target = Home.BOB.getLocation();
		final WorldPoint position = new WorldPoint(3000, 3000, 0);
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(position);
		set("currentHome", Home.BOB);
		set("lastHintArrowPoint", target);
		change(MahoganyHomesConfig.HINT_ARROW_KEY);
		verify(client).setHintArrow(target);
	}

	@Test
	public void unrelatedInterfacesDoNotScheduleOrSearchWidgets()
	{
		load(InterfaceID.MAGIC_SPELLBOOK);
		assertTrue(tickEnd.isEmpty());
		verify(client, never()).getWidgetRoots();
		verify(client, never()).getWidget(anyInt());
	}

	@Test
	public void loadedDialogueIsReadAfterPopulationAndUpdatesOnlyOnce()
	{
		load(InterfaceID.CHATMENU);
		load(InterfaceID.CHATMENU);
		assertEquals(1, tickEnd.size());
		final Widget expert = option("Expert contract");
		container(InterfaceID.Chatmenu.OPTIONS, option("Novice contract"), expert);
		flush();
		assertTrue(expert.getText().contains("<col="));
		verify(expert, times(1)).setText(anyString());
		verify(client, never()).getWidgetRoots();
	}

	@Test
	public void queuedOldLifecycleCannotRecolorAfterRestart()
	{
		final Widget expert = option("Expert contract");
		container(InterfaceID.Chatmenu.OPTIONS, option("Novice contract"), expert);
		load(InterfaceID.CHATMENU);
		final Runnable stale = tickEnd.remove(0);
		plugin.shutDown();
		plugin.startUp();
		stale.run();
		verify(expert, never()).setText(anyString());
		flush();
		assertTrue(expert.getText().contains("<col="));
	}

	@Test
	public void tierToggleRestoresOpenDialogueWithoutDisablingTeleportHighlight() throws Exception
	{
		final Widget expert = option("Expert contract");
		final Widget destination = option("Xeric's Glade");
		container(InterfaceID.Chatmenu.OPTIONS, option("Novice contract"), expert);
		container(InterfaceID.Menu.LJ_LAYER1, destination);
		seedGuidance();
		plugin.teleportItem = new TeleportItem(ItemID.XERICS_TALISMAN, 30, "Xeric's Glade");
		load(InterfaceID.CHATMENU);
		flush();
		assertTrue(expert.getText().contains("<col="));
		when(config.highlightContractorTiers()).thenReturn(false);
		change(MahoganyHomesConfig.HIGHLIGHT_CONTRACTOR_TIERS_KEY);
		flush();
		assertEquals("Expert contract", expert.getText());
		assertTrue(destination.getText().contains("<col="));
	}

	@Test
	public void bothBooksUseTheKnownDialogueContainer()
	{
		final Widget destination = option("'Lunch by the Lancalliums' - Hosidius");
		final Widget other = option("'The Fisher's Flute' - Piscarilius");
		container(InterfaceID.Chatmenu.OPTIONS, option("What would you like to remember?"), destination, other);
		highlighter.refresh(null, "Lunch by the Lancalliums", Color.CYAN);
		assertTrue(destination.getText().contains("<col="));
		assertEquals("'The Fisher's Flute' - Piscarilius", other.getText());
		verify(client, never()).getWidgetRoots();
	}

	@Test
	public void unrelatedExpertTextIsNotAContractorMenu()
	{
		final Widget expert = option("Expert advice");
		container(InterfaceID.Chatmenu.OPTIONS, expert, option("Leave"));
		highlighter.refresh("Expert", null, Color.CYAN);
		verify(expert, never()).setText(anyString());
	}

	@Test
	public void restoringHighlightsPreservesLaterChangesByOtherPlugins()
	{
		final Widget destination = option("Xeric's Glade");
		container(InterfaceID.Menu.LJ_LAYER1, destination);
		highlighter.refresh(null, "Xeric's Glade", Color.CYAN);
		destination.setText("Other plugin's text");
		highlighter.clear();
		assertEquals("Other plugin's text", destination.getText());
	}

	@Test
	public void disabledAndHiddenOptionsKeepTheirAppearance()
	{
		final Widget disabled = option("<col=808080>Xeric's Glade</col>");
		final Widget hidden = option("Xeric's Glade");
		when(hidden.isHidden()).thenReturn(true);
		container(InterfaceID.Menu.LJ_LAYER1, disabled, hidden);
		highlighter.refresh(null, "Xeric's Glade", Color.CYAN);
		verify(disabled, never()).setText(anyString());
		verify(hidden, never()).setText(anyString());
	}

	@Test
	public void missingTeleportItemDoesNotCrashOrHighlightTheWrongTab() throws Exception
	{
		set("currentHome", Home.BOB);
		plugin.teleportItem = new TeleportItem(ItemID.VARROCK_TELEPORT, 60);
		final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			new TeleportWidgetOverlay(client, plugin, config).render(graphics);
			assertEquals(0, image.getRGB(10, 10));
			verify(client, never()).getWidget(anyInt());
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void spellRendersByIdAndPreservesCallerGraphics() throws Exception
	{
		set("currentHome", Home.BOB);
		when(config.highlightTabIcons()).thenReturn(false);
		plugin.teleportItem = new TeleportItem("Varrock Teleport", InterfaceID.MagicSpellbook.VARROCK_TELEPORT, 60, 25);
		final Widget spell = mock(Widget.class);
		when(client.getWidget(InterfaceID.MagicSpellbook.VARROCK_TELEPORT)).thenReturn(spell);
		when(spell.getBounds()).thenReturn(new Rectangle(10, 10, 20, 20));
		final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(Color.MAGENTA);
			new TeleportWidgetOverlay(client, plugin, config).render(graphics);
			assertEquals(Color.CYAN.getRGB(), image.getRGB(15, 15));
			assertEquals(Color.MAGENTA, graphics.getColor());
			verify(client, never()).getWidgetRoots();
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void allThreeTabsSupportAllThreeLayouts()
	{
		final int[][] ids = {
			{InterfaceID.Toplevel.STONE3, InterfaceID.ToplevelOsrsStretch.STONE3, InterfaceID.ToplevelPreEoc.STONE3},
			{InterfaceID.Toplevel.STONE4, InterfaceID.ToplevelOsrsStretch.STONE4, InterfaceID.ToplevelPreEoc.STONE4},
			{InterfaceID.Toplevel.STONE6, InterfaceID.ToplevelOsrsStretch.STONE6, InterfaceID.ToplevelPreEoc.STONE6}
		};
		final TeleportWidgetOverlay overlay = new TeleportWidgetOverlay(client, plugin, config);
		for (int tab = 0; tab < ids.length; tab++)
		{
			for (int layout = 0; layout < ids[tab].length; layout++)
			{
				reset(client);
				final Widget visible = mock(Widget.class);
				when(client.getWidget(ids[tab][layout])).thenReturn(visible);
				assertSame(visible, overlay.getTabWidget(TeleportItem.TeleportTab.values()[tab]));
			}
		}
	}

	@Test
	public void timeoutClearsTeleportCandidatesAndArrows() throws Exception
	{
		seedGuidance();
		set("lastChanged", Instant.now().minusSeconds(600));
		plugin.onGameTick(new GameTick());
		assertNull(plugin.getTeleportItem());
		assertNull(plugin.getCandidateTeleportItem());
		assertNull(get("lastHintArrowPoint"));
	}

	@Test
	public void contractorChoiceStillUsesCurrentCity() throws Exception
	{
		set("lastCompletedHome", Home.BOB);
		when(config.contractorMode()).thenReturn(ContractorMode.SMART_NEAREST);
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3240, 3474, 0));
		plugin.selectPostContractContractor();
		assertEquals(Contractor.MARLO, plugin.getCurrentContractor());
		when(player.getWorldLocation()).thenReturn(new WorldPoint(2638, 3293, 0));
		plugin.selectPostContractContractor();
		assertEquals(Contractor.ELLIE, plugin.getCurrentContractor());
	}

	@Test
	public void unrelatedItemActionsAreNotHighlighted() throws Exception
	{
		set("currentHome", Home.BOB);
		plugin.teleportItem = new TeleportItem(ItemID.VARROCK_TELEPORT, 60);
		final MenuEntry entry = mock(MenuEntry.class);
		when(entry.getOption()).thenReturn("Break");
		when(entry.getItemId()).thenReturn(-1);
		menu(entry);
		verify(entry, never()).setOption(anyString());
		when(entry.getItemId()).thenReturn(ItemID.VARROCK_TELEPORT);
		final Widget tablet = mock(Widget.class);
		when(tablet.getId()).thenReturn(InterfaceID.INVENTORY << 16);
		when(tablet.getItemId()).thenReturn(ItemID.VARROCK_TELEPORT);
		when(entry.getWidget()).thenReturn(tablet);
		menu(entry);
		verify(entry, never()).setOption(anyString());
		when(entry.getOption()).thenReturn("Grand Exchange");
		menu(entry);
		verify(entry, never()).setOption(anyString());
		when(entry.getOption()).thenReturn("Varrock");
		menu(entry);
		verify(entry).setOption(contains("<col="));
	}

	@Test
	public void equippedGladeDestinationIsHighlightedWithoutAnItemIdOnTheMenuEntry() throws Exception
	{
		set("currentHome", Home.MARIAH);
		plugin.teleportItem = new TeleportItem(ItemID.XERICS_TALISMAN, 50, "Xeric's Glade");
		final Widget slot = mock(Widget.class);
		when(slot.getId()).thenReturn(InterfaceID.WORNITEMS << 16);
		when(slot.getItemId()).thenReturn(-1);
		final Widget item = mock(Widget.class);
		when(item.getItemId()).thenReturn(ItemID.XERICS_TALISMAN);
		when(slot.getChild(1)).thenReturn(item);
		final MenuEntry entry = mock(MenuEntry.class);
		when(entry.getWidget()).thenReturn(slot);
		when(entry.getItemId()).thenReturn(-1);
		when(entry.getOption()).thenReturn("Xeric's Glade");
		menu(entry);
		verify(entry).setOption(ColorUtil.prependColorTag("Xeric's Glade", Color.CYAN));
		clearInvocations(entry);
		when(entry.getOption()).thenReturn("Xeric's Lookout");
		menu(entry);
		verify(entry, never()).setOption(anyString());
		when(entry.getOption()).thenReturn("Xeric's Glade");
		when(item.getItemId()).thenReturn(ItemID.VARROCK_TELEPORT);
		menu(entry);
		verify(entry, never()).setOption(anyString());
	}

	@Test
	public void lastTierMenuRequiresAnActualContractor()
	{
		final MenuEntry entry = mock(MenuEntry.class);
		when(entry.getOption()).thenReturn("Last-tier contract");
		menu(entry);
		verify(entry, never()).setOption(anyString());
		final NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(net.runelite.api.NpcID.AMY);
		when(entry.getNpc()).thenReturn(npc);
		menu(entry);
		verify(entry).setOption(contains("<col="));
	}

	@Test
	public void magicLevelChangesInvalidateSpellRecommendation() throws Exception
	{
		set("currentHome", Home.BOB);
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3000, 3000, 0));
		final ItemContainer inventory = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(inventory.count(ItemID.LAW_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.FIRE_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.AIR_RUNE)).thenReturn(3);
		when(client.getBoostedSkillLevel(Skill.MAGIC)).thenReturn(25);
		final StatChanged event = new StatChanged(Skill.MAGIC, 0, 25, 25);
		plugin.onStatChanged(event);
		plugin.onGameTick(new GameTick());
		assertNotNull(plugin.getTeleportItem());
		assertTrue(plugin.getTeleportItem().isSpell());
		when(client.getBoostedSkillLevel(Skill.MAGIC)).thenReturn(24);
		plugin.onStatChanged(event);
		plugin.onGameTick(new GameTick());
		assertNull(plugin.getTeleportItem());
		assertNull(plugin.getCandidateTeleportItem());
	}

	@Test
	public void restoredBobContractAndReminderKeepVarrockRecommendationWithGePreference() throws Exception
	{
		when(client.getAccountHash()).thenReturn(42L);
		when(configManager.getConfiguration("MahoganyHomes.42", MahoganyHomesConfig.HOME_KEY)).thenReturn("BOB");
		when(configManager.getConfiguration("MahoganyHomes.42", MahoganyHomesConfig.TIER_KEY)).thenReturn("3");
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3000, 3000, 0));
		final ItemContainer inventory = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(inventory.count(ItemID.LAW_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.FIRE_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.AIR_RUNE)).thenReturn(3);
		when(client.getBoostedSkillLevel(Skill.MAGIC)).thenReturn(25);
		when(client.getVarbitValue(VarbitID.VARROCK_GE_TELEPORT)).thenReturn(1);
		plugin.onGameTick(new GameTick());
		assertEquals(Home.BOB, plugin.getCurrentHome());
		assertNotNull(plugin.getTeleportItem());
		assertEquals(InterfaceID.MagicSpellbook.VARROCK_TELEPORT, plugin.getTeleportItem().getSpellWidgetId());
		final Widget reminder = mock(Widget.class);
		when(reminder.getText()).thenReturn("You're currently on an Adept Contract. Go see Bob in north-east Varrock, opposite the church. You can get another job once you have furnished his home.");
		when(client.getWidget(ComponentID.DIALOG_NPC_TEXT)).thenReturn(reminder);
		plugin.onGameTick(new GameTick());
		assertEquals(Home.BOB, plugin.getCurrentHome());
		assertTrue(plugin.getTeleportItem().isSpell());
	}

	@Test
	public void varrockSpellOnlyHighlightsTheSquareCastAction() throws Exception
	{
		set("currentHome", Home.BOB);
		plugin.teleportItem = new TeleportItem("Varrock Teleport", InterfaceID.MagicSpellbook.VARROCK_TELEPORT, 60, 25);
		final Widget widget = mock(Widget.class);
		when(widget.getId()).thenReturn(InterfaceID.MagicSpellbook.VARROCK_TELEPORT);
		final MenuEntry entry = mock(MenuEntry.class);
		when(entry.getWidget()).thenReturn(widget);
		when(entry.getOption()).thenReturn("Grand Exchange");
		when(entry.getIdentifier()).thenReturn(3);
		menu(entry);
		verify(entry, never()).setOption(anyString());
		when(entry.getOption()).thenReturn("Cast");
		menu(entry);
		verify(entry).setOption(contains("<col="));
	}

	@Test
	public void spellFilteringRefreshesRecommendationWithoutInventoryChange() throws Exception
	{
		final Widget houseSpell = prepareHosidiusGuidance();
		when(houseSpell.isSelfHidden()).thenReturn(true);
		final ScriptCallbackEvent redraw = new ScriptCallbackEvent();
		redraw.setEventName("spellbookSort");
		plugin.onScriptCallbackEvent(redraw);
		plugin.onGameTick(new GameTick());
		assertEquals(ItemID.XERICS_TALISMAN, plugin.getTeleportItem().getItemId());
		assertEquals(TeleportItem.TeleportTab.INVENTORY, plugin.getTeleportItem().getTab(client));

		when(houseSpell.isSelfHidden()).thenReturn(false);
		plugin.onScriptCallbackEvent(redraw);
		plugin.onGameTick(new GameTick());
		assertTrue(plugin.getTeleportItem().isSpell());

		final String[] actions = new String[7];
		actions[6] = "Unhide";
		when(houseSpell.getActions()).thenReturn(actions);
		final ConfigChanged hidden = new ConfigChanged();
		hidden.setGroup("spellbook");
		hidden.setKey("spell_hidden_book_1982_1");
		plugin.onConfigChanged(hidden);
		plugin.onGameTick(new GameTick());
		assertEquals(ItemID.XERICS_TALISMAN, plugin.getTeleportItem().getItemId());
	}

	@Test
	public void movingHouseRefreshesRecommendationForVarbitAndServerVarpUpdates() throws Exception
	{
		prepareHosidiusGuidance();
		when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(1);
		final VarbitChanged moved = new VarbitChanged();
		moved.setVarbitId(VarbitID.POH_HOUSE_LOCATION);
		plugin.onVarbitChanged(moved);
		plugin.onGameTick(new GameTick());
		assertEquals(ItemID.XERICS_TALISMAN, plugin.getTeleportItem().getItemId());

		final VarbitComposition location = mock(VarbitComposition.class);
		when(location.getIndex()).thenReturn(738);
		when(client.getVarbit(VarbitID.POH_HOUSE_LOCATION)).thenReturn(location);
		when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(8);
		final VarbitChanged serverUpdate = new VarbitChanged();
		serverUpdate.setVarpId(738);
		plugin.onVarbitChanged(serverUpdate);
		plugin.onGameTick(new GameTick());
		assertTrue(plugin.getTeleportItem().isSpell());
	}

	@Test
	public void unrelatedScriptDoesNotRecalculateTeleports() throws Exception
	{
		prepareHosidiusGuidance();
		clearInvocations(client);
		final ScriptCallbackEvent unrelated = new ScriptCallbackEvent();
		unrelated.setEventName("unrelated");
		plugin.onScriptCallbackEvent(unrelated);
		plugin.onGameTick(new GameTick());
		verify(client, never()).getBoostedSkillLevel(Skill.MAGIC);
	}

	@Test
	public void unknownAssignmentAfterLoginDoesNotSuggestAContractorTeleport() throws Exception
	{
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(config.contractorMode()).thenReturn(ContractorMode.SMART_NEAREST);
		final ItemContainer inventory = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(inventory.contains(ItemID.FALADOR_TELEPORT)).thenReturn(true);
		plugin.onGameTick(new GameTick());
		plugin.selectPostContractContractor();
		assertNull(plugin.getCurrentHome());
		assertNull(plugin.getCurrentContractor());
		assertNull(plugin.getTeleportItem());
		change(MahoganyHomesConfig.CONTRACTOR_MODE_KEY);
		assertNull(plugin.getCurrentContractor());
	}

	@Test
	public void savedAssignmentRestoresWhenAccountIsReadyWithoutUsernameEvent() throws Exception
	{
		when(client.getAccountHash()).thenReturn(42L);
		when(configManager.getConfiguration("MahoganyHomes.42", MahoganyHomesConfig.HOME_KEY)).thenReturn("BARBARA");
		when(configManager.getConfiguration("MahoganyHomes.42", MahoganyHomesConfig.TIER_KEY)).thenReturn("3");
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		set("lastChanged", Instant.now().minusSeconds(600));
		plugin.onGameTick(new GameTick());
		assertEquals(Home.BARBARA, plugin.getCurrentHome());
		assertEquals(3, plugin.getContractTier());
		assertFalse(plugin.isPluginTimedOut());
		assertNull(plugin.getCurrentContractor());
		plugin.onGameTick(new GameTick());
		verify(configManager, times(1)).getConfiguration("MahoganyHomes.42", MahoganyHomesConfig.HOME_KEY);
	}

	@Test
	public void observedCompletionStillStartsPostContractGuidance() throws Exception
	{
		set("currentHome", Home.BARBARA);
		set("contractTier", 3);
		plugin.completeCurrentContract();
		assertNull(plugin.getCurrentHome());
		assertEquals(Contractor.AMY, plugin.getCurrentContractor());
		assertEquals(1, plugin.getSessionContracts());
	}

	@Test
	public void loggingOutClearsContractorGuidanceAndAnotherAccountDoesNotInheritContract() throws Exception
	{
		seedGuidance();
		set("lastCompletedHome", Home.BARBARA);
		final GameStateChanged logout = new GameStateChanged();
		logout.setGameState(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(logout);
		assertGuidanceCleared();
		assertNull(get("lastCompletedHome"));
		set("currentHome", Home.BARBARA);
		set("contractTier", 3);
		plugin.onUsernameChanged(new UsernameChanged());
		assertNull(plugin.getCurrentHome());
		assertEquals(0, plugin.getContractTier());
	}

	@Test
	public void timeoutKeepsTheSavedAssignment() throws Exception
	{
		set("currentHome", Home.BARBARA);
		set("contractTier", 3);
		set("lastChanged", Instant.now().minusSeconds(600));
		plugin.onGameTick(new GameTick());
		assertEquals(Home.BARBARA, plugin.getCurrentHome());
		assertTrue(plugin.isPluginTimedOut());
		verify(configManager, never()).unsetConfiguration(anyString(), eq(MahoganyHomesConfig.HOME_KEY));
		verify(configManager, never()).unsetConfiguration(anyString(), eq(MahoganyHomesConfig.TIER_KEY));
	}

	private Widget prepareHosidiusGuidance() throws Exception
	{
		set("currentHome", Home.BARBARA);
		final Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3000, 3000, 0));
		final ItemContainer inventory = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(inventory.count(ItemID.LAW_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.AIR_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.EARTH_RUNE)).thenReturn(1);
		when(inventory.contains(ItemID.XERICS_TALISMAN)).thenReturn(true);
		when(client.getBoostedSkillLevel(Skill.MAGIC)).thenReturn(99);
		when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(8);
		final Widget spell = mock(Widget.class);
		when(client.getWidget(InterfaceID.MagicSpellbook.TELEPORT_TO_YOUR_HOUSE)).thenReturn(spell);
		plugin.onStatChanged(new StatChanged(Skill.MAGIC, 0, 99, 99));
		plugin.onGameTick(new GameTick());
		assertTrue(plugin.getTeleportItem().isSpell());
		return spell;
	}

	private void menu(final MenuEntry entry)
	{
		final MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getMenuEntry()).thenReturn(entry);
		plugin.onMenuEntryAdded(event);
	}

	private void seedGuidance() throws Exception
	{
		final TeleportItem item = new TeleportItem(ItemID.FALADOR_TELEPORT, 23);
		set("currentContractor", Contractor.AMY);
		set("contractorNpc", mock(NPC.class));
		set("candidateTeleportItem", item);
		set("lastHintArrowPoint", Contractor.AMY.getLocation());
		set("lastHintArrowNpc", mock(NPC.class));
		set("lastContractorCheckLocation", Contractor.AMY.getLocation());
		plugin.teleportItem = item;
	}

	private void assertGuidanceCleared() throws Exception
	{
		assertNull(plugin.getCurrentContractor());
		assertNull(plugin.getContractorNpc());
		assertNull(plugin.getTeleportItem());
		assertNull(plugin.getCandidateTeleportItem());
		assertNull(get("lastHintArrowPoint"));
		assertNull(get("lastHintArrowNpc"));
		assertNull(get("lastContractorCheckLocation"));
		verify(client, atLeastOnce()).clearHintArrow();
	}

	private void change(final String key)
	{
		final ConfigChanged event = new ConfigChanged();
		event.setGroup(MahoganyHomesConfig.GROUP_NAME);
		event.setKey(key);
		plugin.onConfigChanged(event);
	}

	private void load(final int group)
	{
		final WidgetLoaded event = new WidgetLoaded();
		event.setGroupId(group);
		plugin.onWidgetLoaded(event);
	}

	private Widget option(final String text)
	{
		final Widget widget = mock(Widget.class);
		final String[] current = {text};
		when(widget.getText()).thenAnswer(invocation -> current[0]);
		doAnswer(invocation ->
		{
			current[0] = invocation.getArgument(0);
			return widget;
		}).when(widget).setText(anyString());
		return widget;
	}

	private void container(final int id, final Widget... options)
	{
		final Widget parent = mock(Widget.class);
		when(parent.getChildren()).thenReturn(options);
		when(client.getWidget(id)).thenReturn(parent);
	}

	private void flush()
	{
		final List<Runnable> pending = new ArrayList<>(tickEnd);
		tickEnd.clear();
		pending.forEach(Runnable::run);
	}

	private void set(final String name, final Object value) throws Exception
	{
		final Field field = MahoganyHomesPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private Object get(final String name) throws Exception
	{
		final Field field = MahoganyHomesPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(plugin);
	}
}
