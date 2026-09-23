package thestonedturtle.mahoganyhomes;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.VarbitComposition;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Mahogany Homes"
)
public class MahoganyHomesPlugin extends Plugin
{
	@VisibleForTesting
	static final Pattern CONTRACT_PATTERN = Pattern.compile("(Please could you g|G)o see (\\w*)[ ,][\\w\\s,-]*[?.] You can get another job once you have furnished \\w* home\\.");
	@VisibleForTesting
	static final Pattern REMINDER_PATTERN = Pattern.compile("You're currently on an? (\\w*) Contract\\. Go see (\\w*)[ ,][\\w\\s,-]*\\. You can get another job once you have furnished \\w* home\\.");
	private static final Pattern CONTRACT_FINISHED = Pattern.compile("You have completed (?:a|another|your|[\\d,]+) contracts?(?: with a total of [\\d,]+ points?)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern CONTRACT_ASSIGNED = Pattern.compile("(\\w*) Contract: Go see [\\w\\s,-]*\\.");
	private static final Pattern REQUEST_CONTACT_TIER = Pattern.compile("Could I have an? (\\w*) contract please\\?");

	private static final List<Integer> PLANK_IDS = Arrays.asList(ItemID.PLANK, ItemID.OAK_PLANK, ItemID.TEAK_PLANK, ItemID.MAHOGANY_PLANK);

	@Getter
	@Inject
	private Client client;

	@VisibleForTesting
	void setClient(final Client client)
	{
		this.client = client;
	}

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private MahoganyHomesConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MahoganyHomesOverlay textOverlay;

	@Inject
	private MahoganyHomesHighlightOverlay highlightOverlay;

	@Inject
	private TeleportItemOverlay teleportItemOverlay;

	@Inject
	private TeleportWidgetOverlay teleportWidgetOverlay;

	@Inject
	private DialogueHighlighter dialogueHighlighter;

	private volatile boolean running;
	private volatile int dialogueGeneration;
	private int pendingDialogueGeneration = -1;
	private boolean teleportInputsChanged;
	private volatile boolean restoreContractOnLogin;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private EventBus eventBus;

	@Provides
	MahoganyHomesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MahoganyHomesConfig.class);
	}

	@Getter
	private final List<GameObject> objectsToMark = new ArrayList<>();
	@Getter
	private final List<GameObject> laddersToMark = new ArrayList<>();
	// Varb values: 0=default, 1=Needs repair, 2=Repaired, 3=Remove 4=Bulld 5-8=Built Tiers
	private final HashMap<Integer, Integer> varbMap = new HashMap<>();

	private BufferedImage mapIcon;
	private BufferedImage mapArrow;

	@Getter
	private Home currentHome;
	@Getter
	private Contractor currentContractor;
	private WorldPoint lastContractorCheckLocation;
	private Home lastCompletedHome;
	private boolean varbChange;
	private boolean plankSackVarbChange;
	private boolean wasTimedOut;
	@Getter
	private int contractTier = 0;
	@Getter
	private int lastContractTier = 0;

	@Getter
	public TeleportItem teleportItem;
	@Getter
	private TeleportItem candidateTeleportItem;
	@Getter
	private NPC contractorNpc;
	private WorldPoint lastHintArrowPoint;
	private NPC lastHintArrowNpc;

	@Getter
	private int numPlanksInInventory = 0;
	@Getter
	private int numSteelBarsInInventory = 0;

	// Used to auto disable plugin if nothing has changed recently.
	private Instant lastChanged;
	private int lastCompletedCount = -1;

	@Getter
	private int sessionContracts = 0;
	@Getter
	private int sessionPoints = 0;

	private Duration pluginTimeoutDuration = Duration.ofMinutes(5);

	@Override
	public void startUp()
	{
		running = true;
		restoreContractOnLogin = true;
		dialogueGeneration++;
		overlayManager.add(textOverlay);
		overlayManager.add(highlightOverlay);
		overlayManager.add(teleportItemOverlay);
		overlayManager.add(teleportWidgetOverlay);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(this::loadFromConfig);
			clientThread.invoke(this::updateVarbMap);
		}
		lastChanged = Instant.now();
		lastCompletedCount = 0;
		pluginTimeoutDuration = Duration.ofMinutes(config.sessionTimeout());
		queueDialogueUpdate();
	}

	@Override
	public void shutDown()
	{
		running = false;
		restoreContractOnLogin = false;
		dialogueGeneration++;
		clientThread.invoke(dialogueHighlighter::clear);
		lastContractTier = 0;
		teleportInputsChanged = false;
		overlayManager.remove(textOverlay);
		overlayManager.remove(highlightOverlay);
		overlayManager.remove(teleportItemOverlay);
		overlayManager.remove(teleportWidgetOverlay);
		worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
		client.clearHintArrow();
		lastHintArrowPoint = null;
		lastHintArrowNpc = null;
		varbMap.clear();
		objectsToMark.clear();
		laddersToMark.clear();
		currentHome = null;
		currentContractor = null;
		contractorNpc = null;
		lastContractorCheckLocation = null;
		lastCompletedHome = null;
		mapIcon = null;
		mapArrow = null;
		teleportItem = null;
		candidateTeleportItem = null;
		lastChanged = null;
		lastCompletedCount = -1;
		contractTier = 0;
		numPlanksInInventory = 0;
		numSteelBarsInInventory = 0;
		wasTimedOut = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged c)
	{
		if ("spellbook".equals(c.getGroup()))
		{
			clientThread.invoke(() ->
			{
				teleportInputsChanged = true;
			});
			return;
		}
		if (!c.getGroup().equals(MahoganyHomesConfig.GROUP_NAME))
		{
			return;
		}

		if (c.getKey().equals(MahoganyHomesConfig.WORLD_MAP_KEY))
		{
			worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
			if (config.worldMapIcon() && currentHome != null)
			{
				worldMapPointManager.add(new MahoganyHomesWorldPoint(currentHome.getLocation(), this));
			}
		}
		else if (c.getKey().equals(MahoganyHomesConfig.HINT_ARROW_KEY))
		{
			client.clearHintArrow();
			lastHintArrowPoint = null;
			lastHintArrowNpc = null;
			if (client.getLocalPlayer() != null)
			{
				refreshHintArrow(client.getLocalPlayer().getWorldLocation());
			}
		}
		else if (c.getKey().equals(MahoganyHomesConfig.SESSION_TIMEOUT_KEY))
		{
			pluginTimeoutDuration = Duration.ofMinutes(config.sessionTimeout());
		}
		else if (c.getKey().equals(MahoganyHomesConfig.SHORTEST_PATH_KEY))
		{
			if (config.useShortestPath() && client.getLocalPlayer() != null)
			{
				if (currentHome != null)
				{
					setShortestPath(client.getLocalPlayer().getWorldLocation(), currentHome.getLocation());
				}
				else if (currentContractor != null && config.postContractGuidance())
				{
					setShortestPath(client.getLocalPlayer().getWorldLocation(), currentContractor.getLocation());
				}
			}
		}
		else if (c.getKey().equals(MahoganyHomesConfig.HIGHLIGHT_TELEPORTS_KEY))
		{
			clientThread.invoke(this::updateTeleportItem);
		}
		else if (c.getKey().equals(MahoganyHomesConfig.POST_CONTRACT_KEY) || c.getKey().equals(MahoganyHomesConfig.CONTRACTOR_MODE_KEY))
		{
			if (currentHome == null)
			{
				clientThread.invoke(this::selectPostContractContractor);
			}
		}
		queueDialogueUpdate();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == Varbits.SPELLBOOK || TeleportItem.isRunePouchVarbit(event.getVarbitId())
			|| event.getVarbitId() == VarbitID.POH_HOUSE_LOCATION)
		{
			teleportInputsChanged = true;
		}
		else if (event.getVarpId() >= 0)
		{
			// Server updates can report the containing varp instead of the house varbit.
			final VarbitComposition houseLocation = client.getVarbit(VarbitID.POH_HOUSE_LOCATION);
			if (houseLocation != null && event.getVarpId() == houseLocation.getIndex())
			{
				teleportInputsChanged = true;
			}
		}
		// Defer to game tick for better performance
		varbChange = true;
		switch (event.getVarbitId())
		{	// Limit unnecessary resource recalculations
			// if the varbit isn't a plank sack value
			case VarbitID.PLANK_SACK_PLAIN:
			case VarbitID.PLANK_SACK_OAK:
			case VarbitID.PLANK_SACK_TEAK:
			case VarbitID.PLANK_SACK_MAHOGANY:
				plankSackVarbChange = true;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			restoreContractOnLogin = true;
			lastCompletedHome = null;
			resetContractGuidanceVars();
		}
		if (e.getGameState() == GameState.LOADING)
		{
			objectsToMark.clear();
			laddersToMark.clear();
		}
	}

	@Subscribe
	public void onUsernameChanged(UsernameChanged e)
	{
		resetContractGuidanceVars();
		lastCompletedHome = null;
		restoreContractOnLogin = true;
		currentHome = null;
		contractTier = 0;
		loadFromConfig();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		processGameObjects(event.getGameObject(), null);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		processGameObjects(null, event.getGameObject());
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked e)
	{
		if (!e.getOverlay().equals(textOverlay))
		{
			return;
		}

		if (e.getEntry().getOption().equals(MahoganyHomesOverlay.CLEAR_OPTION))
		{
			setCurrentHome(null);
			updateConfig();
			lastChanged = null;
		}

		if (e.getEntry().getOption().equals(MahoganyHomesOverlay.TIMEOUT_OPTION))
		{
			lastChanged = Instant.now().minus(pluginTimeoutDuration);
			// Remove worldPoint and clear hint arrow when plugin times out
			worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
			client.clearHintArrow();
			wasTimedOut = true;
			resetTeleportGuidance();
			lastHintArrowPoint = null;
			lastHintArrowNpc = null;
		}

		if (e.getEntry().getOption().equals(MahoganyHomesOverlay.RESET_SESSION_OPTION))
		{
			sessionContracts = 0;
			sessionPoints = 0;
		}
	}

	@Subscribe
	public void onGameTick(GameTick t)
	{
		if (restoreContractOnLogin && client.getGameState() == GameState.LOGGED_IN)
		{
			// UsernameChanged can arrive before the account hash/profile is ready.
			// Retry once on the first logged-in tick, never on ordinary region loads.
			restoreContractOnLogin = false;
			resetContractGuidanceVars();
			currentHome = null;
			contractTier = 0;
			worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
			loadFromConfig();
			updateVarbMap();
		}
		if (contractTier == 0 || currentHome == null)
		{
			checkForContractTierDialog();
		}

		checkForAssignmentDialog();
		if (teleportInputsChanged)
		{
			teleportInputsChanged = false;
			refreshTeleportAvailability();
		}

		if (currentHome == null)
		{
			if (stopTimedOutGuidance())
			{
				return;
			}
			if (lastCompletedHome != null && config.postContractGuidance() && config.contractorMode() != ContractorMode.DISABLED && client.getLocalPlayer() != null)
			{
				final WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

				if (config.contractorMode() == ContractorMode.SMART_NEAREST
					&& (lastContractorCheckLocation == null || !lastContractorCheckLocation.equals(playerLocation)))
				{
					lastContractorCheckLocation = playerLocation;
					selectPostContractContractor();
				}

				if (currentContractor != null)
				{
					contractorNpc = client.getNpcs().stream()
						.filter(n -> Contractor.fromNpc(n) == currentContractor)
						.findFirst()
						.orElse(null);
					refreshHintArrow(playerLocation);
					refreshTeleportItem(playerLocation);
				}
			}
			else if (lastCompletedHome == null || !config.postContractGuidance() || config.contractorMode() == ContractorMode.DISABLED)
			{
				if (currentContractor != null || contractorNpc != null || candidateTeleportItem != null)
				{
					resetContractGuidanceVars();
				}
			}
			return;
		}

		if (varbChange)
		{
			varbChange = false;
			updateVarbMap();

			// If we couldn't find their contract tier recalculate it when they get close
			if (contractTier == 0)
			{
				calculateContractTier();
			}

			final int completed = getCompletedCount();
			if (completed != lastCompletedCount)
			{
				if (wasTimedOut)
				{
					// Refreshes hint arrow and world map icon if necessary
					setCurrentHome(currentHome);
					updateVarbMap();
					wasTimedOut = false;
				}

				lastCompletedCount = completed;
				lastChanged = Instant.now();
			}
		}

		if (stopTimedOutGuidance())
		{
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		refreshHintArrow(playerLocation);
		refreshTeleportItem(playerLocation);
	}

	private boolean stopTimedOutGuidance()
	{
		if (!isPluginTimedOut())
		{
			return false;
		}
		if (!wasTimedOut)
		{
			worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
			clearGuidanceHintArrow();
			resetTeleportGuidance();
			contractorNpc = null;
			lastContractorCheckLocation = null;
		}
		wasTimedOut = true;
		return true;
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM && e.getType() != ChatMessageType.MESBOX)
		{
			return;
		}

		final String cleanMessage = Text.removeTags(e.getMessage());
		final Matcher matcher = CONTRACT_ASSIGNED.matcher(cleanMessage);
		if (matcher.matches())
		{
			final String type = matcher.group(1).toLowerCase();
			setContactTierFromString(type);
			updateResourcesInInventory();
		}

		if (CONTRACT_FINISHED.matcher(cleanMessage).find())
		{
			completeCurrentContract();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId() && event.getContainerId() != InventoryID.EQUIPMENT.getId())
		{
			return;
		}

		if (event.getContainerId() == InventoryID.INVENTORY.getId() && contractTier != 0 && !isPluginTimedOut())
		{
			updateResourcesInInventory();
		}

		if ((currentHome == null && (!config.postContractGuidance() || currentContractor == null)) || !config.highlightTeleports())
		{
			return;
		}

		refreshTeleportAvailability();
	}

	@Subscribe
	public void onStatChanged(final StatChanged event)
	{
		if (event.getSkill() == Skill.MAGIC)
		{
			teleportInputsChanged = true;
		}
	}

	private void refreshTeleportAvailability()
	{
		if (currentHome == null && currentContractor != null && config.contractorMode() == ContractorMode.SMART_NEAREST)
		{
			selectPostContractContractor();
		}
		else
		{
			updateTeleportItem();
		}
	}

	int getTargetContractTier()
	{
		if (contractTier > 0)
		{
			return contractTier;
		}

		if (lastContractTier > 0)
		{
			return lastContractTier;
		}

		if (client == null)
		{
			return 4;
		}

		final int conLevel = client.getRealSkillLevel(Skill.CONSTRUCTION);
		if (conLevel >= 70)
		{
			return 4; // Expert
		}
		else if (conLevel >= 50)
		{
			return 3; // Adept
		}
		else if (conLevel >= 20)
		{
			return 2; // Novice
		}
		return 1; // Beginner
	}

	String getTargetTierName()
	{
		switch (getTargetContractTier())
		{
			case 1:
				return "Beginner";
			case 2:
				return "Novice";
			case 3:
				return "Adept";
			case 4:
			default:
				return "Expert";
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (isPluginTimedOut())
		{
			return;
		}
		final MenuEntry entry = event.getMenuEntry();
		final String rawOption = entry.getOption();
		final String option = Text.removeTags(rawOption).trim();
		final String target = entry.getTarget() != null ? Text.removeTags(entry.getTarget()).trim() : "";
		final Color teleColor = config.highlightTeleportsColor();
		final Widget widget = entry.getWidget();
		final boolean dialogueOption = widget != null && (widget.getId() >>> 16) == InterfaceID.CHATMENU;

		// Contractor right-click and dialogue tier highlights
		if (currentHome == null && teleColor != null && config.highlightContractorTiers())
		{
			// Highlight "Last-tier contract" on Contractor NPC
			if (option.equalsIgnoreCase("Last-tier contract") && Contractor.fromNpc(entry.getNpc()) != null)
			{
				entry.setOption(ColorUtil.prependColorTag(rawOption, teleColor));
				return;
			}

			// Highlight active contract tier in dialogue
			final String tierName = getTargetTierName().toLowerCase(Locale.ROOT);
			if (dialogueOption && (option.toLowerCase(Locale.ROOT).contains(tierName) || target.toLowerCase(Locale.ROOT).contains(tierName)))
			{
				entry.setOption(ColorUtil.prependColorTag(rawOption, teleColor));
				return;
			}
		}

		// Teleport item and menu highlights
		if (teleportItem == null || !config.highlightTeleports() || (currentHome == null && currentContractor == null) || teleColor == null)
		{
			return;
		}

		final String hint = teleportItem.getDestinationHint();
		final boolean matchingItem = !teleportItem.isSpell() && teleportItem.matchesMenuEntry(entry);
		final boolean teleportMenu = widget != null && ((widget.getId() >>> 16) == InterfaceID.MENU || dialogueOption);

		boolean match = false;
		if (matchingItem && teleportItem.getItemId() == ItemID.VARROCK_TELEPORT)
		{
			// Highlight the explicit square destination, not a default Break that may go to GE.
			if (option.equalsIgnoreCase("Varrock"))
			{
				entry.setOption(ColorUtil.prependColorTag(rawOption, teleColor));
			}
			return;
		}

		// Match direct destination hint in right-click options
		if (hint != null && (matchingItem || teleportMenu))
		{
			final String lowerOpt = option.toLowerCase(Locale.ROOT);
			final String lowerHint = hint.toLowerCase(Locale.ROOT);
			if (!lowerOpt.isEmpty() && lowerOpt.contains(lowerHint))
			{
				match = true;
			}
			else if (option.equalsIgnoreCase("Rub") && matchingItem)
			{
				match = true;
			}
		}

		// Match item actions for tablets, spells, and cape teleports
		if (!match && matchingItem)
		{
			if (option.equalsIgnoreCase("Break") || option.equalsIgnoreCase("Cast") || option.equalsIgnoreCase("Teleport") || option.equalsIgnoreCase("Read"))
			{
				match = true;
			}
		}
		if (teleportItem.isSpell() && teleportItem.matchesMenuEntry(entry) && option.equalsIgnoreCase("Cast"))
		{
			match = true;
		}

		if (match)
		{
			entry.setOption(ColorUtil.prependColorTag(rawOption, teleColor));
		}
	}

	private void checkForContractTierDialog()
	{
		final Widget dialog = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
		if (dialog == null)
		{
			return;
		}

		final String text = Text.sanitizeMultilineText(dialog.getText());
		final Matcher matcher = REQUEST_CONTACT_TIER.matcher(text);
		if (matcher.matches())
		{
			final String type = matcher.group(1).toLowerCase();
			setContactTierFromString(type);
			updateResourcesInInventory();
		}
	}

	private void setContactTierFromString(String tier)
	{
		switch (tier)
		{
			case "beginner":
				contractTier = 1;
				lastContractTier = 1;
				break;
			case "novice":
				contractTier = 2;
				lastContractTier = 2;
				break;
			case "adept":
				contractTier = 3;
				lastContractTier = 3;
				break;
			case "expert":
				contractTier = 4;
				lastContractTier = 4;
				break;
		}
		saveLastContractTier();
	}

	private void saveLastContractTier()
	{
		queueDialogueUpdate();
		if (lastContractTier > 0 && client.getGameState() == GameState.LOGGED_IN)
		{
			final String group = MahoganyHomesConfig.GROUP_NAME + "." + client.getAccountHash();
			configManager.setConfiguration(group, MahoganyHomesConfig.LAST_TIER_KEY, lastContractTier);
		}
	}

	// Check for NPC dialog assigning or reminding us of a contract
	private void checkForAssignmentDialog()
	{
		final Widget dialog = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
		if (dialog == null)
		{
			return;
		}

		final String npcText = Text.sanitizeMultilineText(dialog.getText());
		final Matcher startContractMatcher = CONTRACT_PATTERN.matcher(npcText);
		final Matcher reminderContract = REMINDER_PATTERN.matcher(npcText);
		String name = null;
		int tier = -1;
		if (startContractMatcher.matches())
		{
			name = startContractMatcher.group(2);
		}
		else if (reminderContract.matches())
		{
			name = reminderContract.group(2);
			tier = getTierByText(reminderContract.group(1));
		}

		if (name != null)
		{
			// They may have asked for a contract but already had one, check the configs
			if (contractTier == 0)
			{
				loadFromConfig();
				// If the config matches the assigned value then do nothing
				if (currentHome != null && currentHome.getName().equalsIgnoreCase(name))
				{
					return;
				}
			}

			// If we could parse the tier from the message (only for reminders) make sure the current tier matches it
			// update the tier and config with the parsed value
			if (tier != -1)
			{
				contractTier = tier;
				lastContractTier = tier;
				saveLastContractTier();
			}
			updateResourcesInInventory();

			for (final Home h : Home.values())
			{
				if (h.getName().equalsIgnoreCase(name) && (currentHome != h || isPluginTimedOut()))
				{
					setCurrentHome(h);
					updateConfig();
					break;
				}
			}
		}
	}

	void completeCurrentContract()
	{
		if (currentHome == null)
		{
			return;
		}

		sessionContracts++;
		sessionPoints += getPointsForCompletingTask();
		lastCompletedHome = currentHome;
		setCurrentHome(null);
		updateConfig();
		selectPostContractContractor();
	}

	private void setCurrentHome(final Home h)
	{
		resetTeleportGuidance();
		lastHintArrowPoint = null;
		lastHintArrowNpc = null;
		contractorNpc = null;
		currentHome = h;
		if (currentHome != null)
		{
			lastCompletedHome = null;
			currentContractor = null;
			lastContractorCheckLocation = null;
		}
		client.clearHintArrow();
		lastChanged = Instant.now();
		lastCompletedCount = 0;
		varbMap.clear();

		if (currentHome == null)
		{
			worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
			if (contractTier > 0)
			{
				lastContractTier = contractTier;
				saveLastContractTier();
			}
			contractTier = 0;
			teleportItem = null;
			numPlanksInInventory = 0;
			numSteelBarsInInventory = 0;
			return;
		}

		if (config.worldMapIcon())
		{
			worldMapPointManager.removeIf(MahoganyHomesWorldPoint.class::isInstance);
			worldMapPointManager.add(new MahoganyHomesWorldPoint(h.getLocation(), this));
		}

		if (config.useShortestPath() && client.getLocalPlayer() != null)
		{
			WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
			setShortestPath(playerWp, h.getLocation());
		}

		if (config.displayHintArrows() && client.getLocalPlayer() != null)
		{
			refreshHintArrow(client.getLocalPlayer().getWorldLocation());
		}

		if (config.highlightTeleports() && client.getLocalPlayer() != null)
		{
			clientThread.invoke(this::updateTeleportItem);
		}
	}

	private void updateTeleportItem()
	{
		if (!running)
		{
			return;
		}
		if (isPluginTimedOut() || (currentHome == null && (currentContractor == null || !config.postContractGuidance()
			|| config.contractorMode() == ContractorMode.DISABLED)) || !config.highlightTeleports())
		{
			resetTeleportGuidance();
			return;
		}

		final TeleportItems items = currentHome != null ? currentHome.getTeleportItems() : (currentContractor != null ? currentContractor.getTeleportItems() : null);
		candidateTeleportItem = items != null ? items.getClosestTeleportItemOnPlayer(client) : null;

		if (client.getLocalPlayer() != null)
		{
			refreshTeleportItem(client.getLocalPlayer().getWorldLocation());
		}
		else
		{
			teleportItem = candidateTeleportItem;
		}
		queueDialogueUpdate();
	}

	private void resetTeleportGuidance()
	{
		teleportItem = null;
		candidateTeleportItem = null;
		queueDialogueUpdate();
	}

	private void clearGuidanceHintArrow()
	{
		client.clearHintArrow();
		lastHintArrowPoint = null;
		lastHintArrowNpc = null;
	}

	private void resetContractGuidanceVars()
	{
		currentContractor = null;
		contractorNpc = null;
		lastContractorCheckLocation = null;
		resetTeleportGuidance();
		clearGuidanceHintArrow();
	}

	void selectPostContractContractor()
	{
		if (!running || currentHome != null)
		{
			return;
		}
		if (lastCompletedHome == null || !config.postContractGuidance() || config.contractorMode() == ContractorMode.DISABLED || isPluginTimedOut())
		{
			resetContractGuidanceVars();
			return;
		}
		final Contractor previousContractor = currentContractor;

		if (config.contractorMode() == ContractorMode.ALWAYS_AMY)
		{
			currentContractor = Contractor.AMY;
		}
		else
		{
			final WorldPoint playerPos = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
			final Contractor local = playerPos != null ? Contractor.getClosestContractor(playerPos) : (lastCompletedHome != null ? Contractor.fromHome(lastCompletedHome) : Contractor.AMY);

			final int walkDistance = (local != null && playerPos != null) ? distanceBetween(local.getArea(), playerPos) : Integer.MAX_VALUE;

			Contractor bestTeleportContractor = null;
			int bestTeleportDistance = Integer.MAX_VALUE;

			for (final Contractor c : Contractor.values())
			{
				if (c.getTeleportItems() != null)
				{
					final TeleportItem item = c.getTeleportItems().getClosestTeleportItemOnPlayer(client);
					if (item != null && item.getDistance() < bestTeleportDistance)
					{
						bestTeleportDistance = item.getDistance();
						bestTeleportContractor = c;
					}
				}
			}

			// If teleporting saves travel time and player is far from local contractor, route to best teleport contractor
			if (bestTeleportContractor != null && (walkDistance > 22 || walkDistance > bestTeleportDistance))
			{
				currentContractor = bestTeleportContractor;
			}
			else
			{
				currentContractor = local != null ? local : Contractor.AMY;
			}
		}

		if (currentContractor != previousContractor)
		{
			contractorNpc = null;
			clearGuidanceHintArrow();
		}

		if (config.displayHintArrows() && client.getLocalPlayer() != null)
		{
			refreshHintArrow(client.getLocalPlayer().getWorldLocation());
		}

		if (config.useShortestPath() && client.getLocalPlayer() != null && currentContractor != null)
		{
			setShortestPath(client.getLocalPlayer().getWorldLocation(), currentContractor.getLocation());
		}

		if (config.highlightTeleports() && client.getLocalPlayer() != null)
		{
			clientThread.invoke(this::updateTeleportItem);
		}
	}

	private void processGameObjects(final GameObject cur, final GameObject prev)
	{
		objectsToMark.remove(prev);
		laddersToMark.remove(prev);

		if (cur == null || (!Hotspot.isHotspotObject(cur.getId()) && !Home.isLadder(cur.getId())))
		{
			return;
		}

		if (Hotspot.isHotspotObject(cur.getId()))
		{
			objectsToMark.add(cur);
			return;
		}

		if (Home.isLadder(cur.getId()))
		{
			laddersToMark.add(cur);
		}
	}

	private void updateVarbMap()
	{
		varbMap.clear();

		for (final Hotspot spot : Hotspot.values())
		{
			varbMap.put(spot.getVarb(), client.getVarbitValue(spot.getVarb()));
		}
		if (plankSackVarbChange)
		{
			updateResourcesInInventory();
			plankSackVarbChange = false;
		}
	}

	private void loadFromConfig()
	{
		final String group = MahoganyHomesConfig.GROUP_NAME + "." + client.getAccountHash();

		final String lastTier = configManager.getConfiguration(group, MahoganyHomesConfig.LAST_TIER_KEY);
		lastContractTier = 0;
		if (lastTier != null)
		{
			try
			{
				final int savedTier = Integer.parseInt(lastTier);
				lastContractTier = savedTier >= 1 && savedTier <= 4 ? savedTier : 0;
			}
			catch (NumberFormatException ignored)
			{
			}
		}

		final String name = configManager.getConfiguration(group, MahoganyHomesConfig.HOME_KEY);
		if (name == null)
		{
			return;
		}

		try
		{
			final Home h = Home.valueOf(name.trim().toUpperCase());
			setCurrentHome(h);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Stored unrecognized home: {}", name);
			currentHome = null;
			configManager.setConfiguration(group, MahoganyHomesConfig.HOME_KEY, null);
		}

		// Get contract tier from config if home was loaded successfully
		if (currentHome == null)
		{
			return;
		}

		final String tier = configManager.getConfiguration(group, MahoganyHomesConfig.TIER_KEY);
		if (tier == null)
		{
			return;
		}

		try
		{
			contractTier = Integer.parseInt(tier);
			if (contractTier > 0)
			{
				lastContractTier = contractTier;
			}
			updateResourcesInInventory();
		}
		catch (NumberFormatException e)
		{
			log.warn("Stored unrecognized tier: {}", name);
			contractTier = 0;
			configManager.setConfiguration(group, MahoganyHomesConfig.TIER_KEY, null);
		}
	}

	private void updateConfig()
	{
		final String group = MahoganyHomesConfig.GROUP_NAME + "." + client.getAccountHash();
		if (currentHome == null)
		{
			configManager.unsetConfiguration(group, MahoganyHomesConfig.HOME_KEY);
			configManager.unsetConfiguration(group, MahoganyHomesConfig.TIER_KEY);
			return;
		}

		configManager.setConfiguration(group, MahoganyHomesConfig.HOME_KEY, currentHome.name());
		configManager.setConfiguration(group, MahoganyHomesConfig.TIER_KEY, contractTier);
	}

	private void refreshTeleportItem(final WorldPoint playerPos)
	{
		final TeleportItem previous = teleportItem;
		teleportItem = getRecommendedTeleport(playerPos);
		if (teleportItem != previous)
		{
			queueDialogueUpdate();
		}
	}

	private TeleportItem getRecommendedTeleport(final WorldPoint playerPos)
	{
		if (candidateTeleportItem == null)
		{
			return null;
		}

		final WorldArea targetArea;
		if (currentHome != null)
		{
			targetArea = currentHome.getArea();
		}
		else if (currentContractor != null && config.postContractGuidance())
		{
			targetArea = currentContractor.getArea();
		}
		else
		{
			return null;
		}

		final int distanceToTarget = distanceBetween(targetArea, playerPos);

		// Only highlight if the player is not at the target AND teleporting saves distance
		if (distanceToTarget > 10 && (distanceToTarget - candidateTeleportItem.getDistance() > 10))
		{
			return candidateTeleportItem;
		}
		return null;
	}

	void refreshHintArrow(final WorldPoint playerPos)
	{
		if (!config.displayHintArrows() || playerPos == null)
		{
			if (lastHintArrowPoint != null || lastHintArrowNpc != null)
			{
				client.clearHintArrow();
				lastHintArrowPoint = null;
				lastHintArrowNpc = null;
			}
			return;
		}

		if (currentHome != null)
		{
			if (distanceBetween(currentHome.getArea(), playerPos) > 0)
			{
				final WorldPoint target = currentHome.getLocation();
				if (!target.equals(lastHintArrowPoint) || lastHintArrowNpc != null)
				{
					client.clearHintArrow();
					client.setHintArrow(target);
					lastHintArrowPoint = target;
					lastHintArrowNpc = null;
				}
			}
			else
			{
				// We are really close to house, only display a hint arrow if we are done.
				if (getCompletedCount() != 0)
				{
					if (lastHintArrowPoint != null || lastHintArrowNpc != null)
					{
						client.clearHintArrow();
						lastHintArrowPoint = null;
						lastHintArrowNpc = null;
					}
					return;
				}

				final Optional<NPC> npc = client.getNpcs().stream().filter(n -> n.getId() == currentHome.getNpcId()).findFirst();
				if (npc.isPresent())
				{
					if (npc.get() != lastHintArrowNpc)
					{
						client.clearHintArrow();
						client.setHintArrow(npc.get());
						lastHintArrowNpc = npc.get();
						lastHintArrowPoint = null;
					}
					return;
				}

				// Couldn't find the NPC, find the closest ladder to player
				WorldPoint location = null;
				int distance = Integer.MAX_VALUE;
				for (final GameObject obj : laddersToMark)
				{
					// Ensure ladder isn't in a nearby home.
					if (distanceBetween(currentHome.getArea(), obj.getWorldLocation()) > 0)
					{
						continue;
					}

					final int diff = obj.getWorldLocation().distanceTo(playerPos);
					if (diff < distance)
					{
						distance = diff;
						location = obj.getWorldLocation();
					}
				}

				if (location != null && (!location.equals(lastHintArrowPoint) || lastHintArrowNpc != null))
				{
					client.clearHintArrow();
					client.setHintArrow(location);
					lastHintArrowPoint = location;
					lastHintArrowNpc = null;
				}
			}
		}
		else if (currentContractor != null && config.postContractGuidance())
		{
			if (contractorNpc != null)
			{
				if (contractorNpc != lastHintArrowNpc)
				{
					client.clearHintArrow();
					client.setHintArrow(contractorNpc);
					lastHintArrowNpc = contractorNpc;
					lastHintArrowPoint = null;
				}
			}
			else
			{
				final WorldPoint target = currentContractor.getLocation();
				if (!target.equals(lastHintArrowPoint) || lastHintArrowNpc != null)
				{
					client.clearHintArrow();
					client.setHintArrow(target);
					lastHintArrowPoint = target;
					lastHintArrowNpc = null;
				}
			}
		}
		else
		{
			if (lastHintArrowPoint != null || lastHintArrowNpc != null)
			{
				client.clearHintArrow();
				lastHintArrowPoint = null;
				lastHintArrowNpc = null;
			}
		}
	}

	int getCompletedCount()
	{
		if (currentHome == null)
		{
			return -1;
		}

		int count = 0;
		for (final Hotspot hotspot : Hotspot.values())
		{
			final boolean requiresAttention = doesHotspotRequireAttention(hotspot.getVarb());
			if (!requiresAttention)
			{
				continue;
			}

			count++;
		}

		return count;
	}

	boolean doesHotspotRequireAttention(final int varb)
	{
		final Integer val = varbMap.get(varb);
		if (val == null)
		{
			return false;
		}

		return val == 1 || val == 3 || val == 4;
	}

	// This check assumes objects are on the same plane as the WorldArea (ignores plane differences)
	int distanceBetween(final WorldArea area, final WorldPoint point)
	{
		return area.distanceTo(new WorldPoint(point.getX(), point.getY(), area.getPlane()));
	}

	BufferedImage getMapIcon()
	{
		if (mapIcon != null)
		{
			return mapIcon;
		}

		mapIcon = ImageUtil.getResourceStreamFromClass(getClass(), "map-icon.png");
		return mapIcon;
	}

	BufferedImage getMapArrow()
	{
		if (mapArrow != null)
		{
			return mapArrow;
		}

		mapArrow = ImageUtil.getResourceStreamFromClass(getClass(), "map-arrow-icon.png");
		return mapArrow;
	}

	boolean isPluginTimedOut()
	{
		return lastChanged != null && Duration.between(lastChanged, Instant.now()).compareTo(pluginTimeoutDuration) >= 0;
	}

	int getPointsForCompletingTask()
	{
		// Contracts reward 2-5 points depending on tier
		return getContractTier() + 1;
	}

	private void calculateContractTier()
	{
		int tier = 0;
		// Values 5-8 are the tier of contract completed
		for (int val : varbMap.values())
		{
			tier = Math.max(tier, val);
		}

		// Normalizes tier from 5-8 to 1-4
		tier -= 4;
		contractTier = Math.max(tier, 0);
	}

	public Set<Integer> getRepairableVarbs()
	{
		return varbMap.keySet()
			.stream()
			.filter(this::doesHotspotRequireAttention)
			.collect(Collectors.toSet());
	}

	private int getTierByText(final String tierText)
	{
		switch (tierText)
		{
			case "Beginner":
				return 1;
			case "Novice":
				return 2;
			case "Adept":
				return 3;
			case "Expert":
				return 4;
			default:
				return -1;
		}
	}

	void updateResourcesInInventory()
	{
		if (contractTier == 0)
		{
			this.numPlanksInInventory = 0;
			this.numSteelBarsInInventory = 0;
			return;
		}

		final ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY.getId());
		if (inventoryContainer == null)
		{
			return;
		}
		final int plankId = PLANK_IDS.get(contractTier - 1);
		int numPlanks = inventoryContainer.count(plankId);
		if (inventoryContainer.contains(ItemID.PLANK_SACK))
		{
			numPlanks += countPlankSackVarb(plankId);
		}
		this.numPlanksInInventory = numPlanks;
		this.numSteelBarsInInventory = inventoryContainer.count(ItemID.STEEL_BAR);
	}

	int countPlankSackVarb(final int plankID)
	{
		switch (plankID)
		{
			case ItemID.PLANK:
				return client.getVarbitValue(VarbitID.PLANK_SACK_PLAIN);
			case ItemID.OAK_PLANK:
				return client.getVarbitValue(VarbitID.PLANK_SACK_OAK);
			case ItemID.TEAK_PLANK:
				return client.getVarbitValue(VarbitID.PLANK_SACK_TEAK);
			case ItemID.MAHOGANY_PLANK:
				return client.getVarbitValue(VarbitID.PLANK_SACK_MAHOGANY);
		}
		return 0;
	}

	void setShortestPath(final WorldPoint start, final WorldPoint target)
	{
		if (config.useShortestPath() && start != null && target != null)
		{
			Map<String, Object> data = new HashMap<>();
			data.put("start", start);
			data.put("target", target);
			eventBus.post(new PluginMessage("shortestpath", "path", data));
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(final ScriptCallbackEvent event)
	{
		if ("spellbookSort".equals(event.getEventName()))
		{
			// Recheck on the next game tick, after the script and Spellbook plugin finish filtering.
			teleportInputsChanged = true;
		}
	}

	@Subscribe
	public void onWidgetLoaded(final WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.CHATMENU || event.getGroupId() == InterfaceID.MENU)
		{
			queueDialogueUpdate();
		}
	}

	private void queueDialogueUpdate()
	{
		clientThread.invoke(() ->
		{
			final int generation = dialogueGeneration;
			if (!running || pendingDialogueGeneration == generation)
			{
				return;
			}
			pendingDialogueGeneration = generation;
			// Options are populated by scripts after WidgetLoaded. Update once at tick end.
			clientThread.invokeAtTickEnd(() ->
			{
				if (!running || generation != dialogueGeneration)
				{
					return;
				}
				pendingDialogueGeneration = -1;
				final boolean active = !isPluginTimedOut();
				final String tier = active && currentHome == null && config.highlightContractorTiers() ? getTargetTierName() : null;
				final String destination = active && config.highlightTeleports() && teleportItem != null
					&& (currentHome != null || currentContractor != null) ? teleportItem.getDestinationHint() : null;
				dialogueHighlighter.refresh(tier, destination, config.highlightTeleportsColor());
			});
		});
	}
}
