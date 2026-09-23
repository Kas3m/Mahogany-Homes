package thestonedturtle.mahoganyhomes;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TeleportRuneTest
{
	private Client client;
	private ItemContainer inventory;
	private ItemContainer equipment;
	private TeleportItem spell;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		inventory = mock(ItemContainer.class);
		equipment = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inventory);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
		when(client.getBoostedSkillLevel(Skill.MAGIC)).thenReturn(99);
		when(inventory.count(ItemID.LAW_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.AIR_RUNE)).thenReturn(3);
		spell = new TeleportItem("Varrock Teleport", InterfaceID.MagicSpellbook.VARROCK_TELEPORT, 60, 25,
			new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.FIRE_RUNE, 1));
	}

	@Test
	public void gePreferenceDoesNotExcludeCastableVarrockSpellOrTablet()
	{
		when(inventory.count(ItemID.FIRE_RUNE)).thenReturn(1);
		assertTrue(TeleportItems.BOB.getClosestTeleportItemOnPlayer(client).isSpell());
		when(client.getVarbitValue(VarbitID.VARROCK_GE_TELEPORT)).thenReturn(1);
		assertTrue(spell.canCastSpell(client));
		assertTrue(TeleportItems.BOB.getClosestTeleportItemOnPlayer(client).isSpell());
		when(inventory.contains(ItemID.VARROCK_TELEPORT)).thenReturn(true);
		assertEquals(ItemID.VARROCK_TELEPORT, TeleportItems.BOB.getClosestTeleportItemOnPlayer(client).getItemId());
		when(inventory.contains(ItemID.VARROCK_TELEPORT)).thenReturn(false);
		when(client.getVarbitValue(VarbitID.VARROCK_GE_TELEPORT)).thenReturn(0);
		assertTrue(TeleportItems.BOB.getClosestTeleportItemOnPlayer(client).isSpell());
	}

	@Test
	public void filteredSpellFallsBackToTalismanAndReturnsWhenUnhidden()
	{
		when(inventory.count(ItemID.EARTH_RUNE)).thenReturn(1);
		when(inventory.contains(ItemID.XERICS_TALISMAN)).thenReturn(true);
		when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(8);
		final Widget houseSpell = mock(Widget.class);
		when(client.getWidget(InterfaceID.MagicSpellbook.TELEPORT_TO_YOUR_HOUSE)).thenReturn(houseSpell);
		assertTrue(TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).isSpell());
		when(houseSpell.isSelfHidden()).thenReturn(true);
		assertEquals(ItemID.XERICS_TALISMAN, TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).getItemId());
		when(houseSpell.isSelfHidden()).thenReturn(false);
		assertTrue(TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).isSpell());
	}

	@Test
	public void closingMagicTabDoesNotExcludeAvailableSpell()
	{
		when(inventory.count(ItemID.FIRE_RUNE)).thenReturn(1);
		final Widget widget = mock(Widget.class);
		when(client.getWidget(spell.getSpellWidgetId())).thenReturn(widget);
		when(widget.isHidden()).thenReturn(true);
		when(widget.isSelfHidden()).thenReturn(false);
		assertTrue(spell.canCastSpell(client));
	}

	@Test
	public void hiddenSpellIsExcludedWhileReordering()
	{
		when(inventory.count(ItemID.FIRE_RUNE)).thenReturn(1);
		final Widget widget = mock(Widget.class);
		when(client.getWidget(spell.getSpellWidgetId())).thenReturn(widget);
		final String[] actions = new String[7];
		actions[6] = "Unhide";
		when(widget.getActions()).thenReturn(actions);
		assertFalse(spell.canCastSpell(client));
		actions[6] = "Hide";
		assertTrue(spell.canCastSpell(client));
	}

	@Test
	public void houseSpellRequiresHosidiusButOtherSpellsDoNot()
	{
		when(inventory.count(ItemID.EARTH_RUNE)).thenReturn(1);
		when(inventory.count(ItemID.FIRE_RUNE)).thenReturn(1);
		when(inventory.contains(ItemID.XERICS_TALISMAN)).thenReturn(true);
		for (final int location : new int[]{0, 1, 7, 9})
		{
			when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(location);
			assertEquals(ItemID.XERICS_TALISMAN, TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).getItemId());
			assertTrue(spell.isAvailableOnPlayer(client));
		}
		when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(8);
		assertTrue(TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).isSpell());
	}

	@Test
	public void ordinaryHouseTabletRequiresHosidiusButRedirectedTabletAndCapeDoNot()
	{
		when(inventory.contains(ItemID.TELEPORT_TO_HOUSE)).thenReturn(true);
		when(inventory.contains(ItemID.HOSIDIUS_TELEPORT)).thenReturn(true);
		when(equipment.contains(ItemID.CONSTRUCT_CAPE)).thenReturn(true);
		final TeleportItem tablet = new TeleportItem(ItemID.TELEPORT_TO_HOUSE, 14);
		assertFalse(tablet.isAvailableOnPlayer(client));
		assertTrue(new TeleportItem(ItemID.HOSIDIUS_TELEPORT, 14).isAvailableOnPlayer(client));
		assertTrue(new TeleportItem(ItemID.CONSTRUCT_CAPE, 14).isAvailableOnPlayer(client));
		assertEquals(ItemID.HOSIDIUS_TELEPORT, TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).getItemId());
		when(client.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(8);
		assertTrue(tablet.isAvailableOnPlayer(client));
		assertEquals(ItemID.TELEPORT_TO_HOUSE, TeleportItems.BARBARA.getClosestTeleportItemOnPlayer(client).getItemId());
	}

	@Test
	public void carriedStaffDoesNotSupplyRunes()
	{
		when(inventory.contains(ItemID.STAFF_OF_FIRE)).thenReturn(true);
		assertFalse(spell.canCastSpell(client));
	}

	@Test
	public void equippedStaffSuppliesRunes()
	{
		when(equipment.contains(ItemID.STAFF_OF_FIRE)).thenReturn(true);
		assertTrue(spell.canCastSpell(client));
	}

	@Test
	public void natureStaffDoesNotSupplyFireRunes()
	{
		when(equipment.contains(ItemID.BRYOPHYTAS_STAFF)).thenReturn(true);
		assertFalse(spell.canCastSpell(client));
	}

	@Test
	public void combinationRunesStillWork()
	{
		when(inventory.count(ItemID.AIR_RUNE)).thenReturn(0);
		when(inventory.count(ItemID.SMOKE_RUNE)).thenReturn(3);
		assertTrue(spell.canCastSpell(client));
	}

	@Test
	public void largeCombinedRuneStacksCannotOverflow()
	{
		when(inventory.count(ItemID.AIR_RUNE)).thenReturn(Integer.MAX_VALUE);
		when(inventory.count(ItemID.SMOKE_RUNE)).thenReturn(Integer.MAX_VALUE);
		assertTrue(spell.canCastSpell(client));
	}

	@Test
	public void pouchRunesAreCountedOnlyWhenPouchIsCarried()
	{
		final EnumComposition runeEnum = mock(EnumComposition.class);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(runeEnum);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_6)).thenReturn(10);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_6)).thenReturn(2);
		when(runeEnum.getIntValue(2)).thenReturn(ItemID.FIRE_RUNE);
		assertFalse(spell.canCastSpell(client));
		when(inventory.contains(ItemID.DIVINE_RUNE_POUCH)).thenReturn(true);
		assertTrue(spell.canCastSpell(client));
	}
}
