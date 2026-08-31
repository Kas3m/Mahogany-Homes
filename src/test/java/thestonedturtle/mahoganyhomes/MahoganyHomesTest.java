/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package thestonedturtle.mahoganyhomes;

import java.util.regex.Matcher;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MahoganyHomesTest
{
	@Test
	public void testAssignmentRegexPattern()
	{
		matchContractAssignment("Jess", "Please could you go see Jess, upstairs of the building south of the church in East Ardougne? You can get another job once you have furnished her home.");
		matchContractAssignment("Jess", "Go see Jess, upstairs of the building south of the church in East Ardougne. You can get another job once you have furnished her home.");

		matchContractAssignment("Sarah", "Please could you go see Sarah along the south wall of Varrock? You can get another job once you have furnished her home.");
		matchContractAssignment("Sarah", "Go see Sarah along the south wall of Varrock. You can get another job once you have furnished her home.");

		matchContractAssignment("Bob", "Please could you go see Bob in north-east Varrock, opposite the church? You can get another job once you have furnished his home.");
		matchContractAssignment("Bob", "Go see Bob in north-east Varrock, opposite the church. You can get another job once you have furnished his home.");

		matchContractAssignment("Barbara", "Please could you go see Barbara, south of Hosidius, near the mill for us? You can get another job once you have furnished her home.");
	}

	private void matchContractAssignment(final String name, final String message)
	{
		final Matcher matcher = MahoganyHomesPlugin.CONTRACT_PATTERN.matcher(message);
		assertTrue(matcher.matches());
		assertEquals(name, matcher.group(2));
	}

	@Test
	public void testReminderRegexPattern()
	{
		matchReminderContract("Expert", "Ross", "You're currently on an Expert Contract. Go see Ross, north of the church in East Ardounge. You can get another job once you have furnished his home.");
		matchReminderContract("Expert", "Jess", "You're currently on an Expert Contract. Go see Jess, upstairs of the building south of the church in East Ardougne. You can get another job once you have furnished her home.");
		matchReminderContract("Expert", "Barbara", "You're currently on an Expert Contract. Go see Barbara, south of Hosidius, near the mill. You can get another job once you have furnished her home.");
		matchReminderContract("Hard", "Bob", "You're currently on a Hard Contract. Go see Bob in north-east Varrock, opposite the church. You can get another job once you have furnished his home.");
		matchReminderContract("Novice", "Barbara", "You're currently on a Novice Contract. Go see Barbara, south of Hosidius, near the mill. You can get another job once you have furnished her home.");
	}

	private void matchReminderContract(final String tier, final String name, final String message)
	{
		final Matcher matcher = MahoganyHomesPlugin.REMINDER_PATTERN.matcher(message);
		assertTrue(matcher.matches());
		assertEquals(tier, matcher.group(1));
		assertEquals(name, matcher.group(2));
	}

	@Test
	public void testHosidiusBarbaraPriority()
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		final net.runelite.api.ItemContainer inv = org.mockito.Mockito.mock(net.runelite.api.ItemContainer.class);
		org.mockito.Mockito.when(client.getItemContainer(net.runelite.api.InventoryID.INVENTORY)).thenReturn(inv);
		org.mockito.Mockito.when(inv.contains(net.runelite.api.ItemID.TELEPORT_TO_HOUSE)).thenReturn(true);

		final TeleportItem best = Home.BARBARA.getTeleportItems().getClosestTeleportItemOnPlayer(client);
		org.junit.Assert.assertNotNull(best);
		org.junit.Assert.assertEquals(net.runelite.api.ItemID.TELEPORT_TO_HOUSE, best.getItemId());
		org.junit.Assert.assertEquals(14, best.getDistance());
	}

	@Test
	public void testHosidiusLeelaXericsTalismanPriority()
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		final net.runelite.api.ItemContainer inv = org.mockito.Mockito.mock(net.runelite.api.ItemContainer.class);
		org.mockito.Mockito.when(client.getItemContainer(net.runelite.api.InventoryID.INVENTORY)).thenReturn(inv);
		org.mockito.Mockito.when(inv.contains(net.runelite.api.ItemID.XERICS_TALISMAN)).thenReturn(true);

		final TeleportItem best = Home.LEELA.getTeleportItems().getClosestTeleportItemOnPlayer(client);
		org.junit.Assert.assertNotNull(best);
		org.junit.Assert.assertEquals(net.runelite.api.ItemID.XERICS_TALISMAN, best.getItemId());
		org.junit.Assert.assertEquals(30, best.getDistance());
	}

	@Test
	public void testHosidiusLeelaFallbackToHouseSpell()
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		final net.runelite.api.ItemContainer inv = org.mockito.Mockito.mock(net.runelite.api.ItemContainer.class);
		org.mockito.Mockito.when(client.getItemContainer(net.runelite.api.InventoryID.INVENTORY)).thenReturn(inv);
		org.mockito.Mockito.when(inv.contains(net.runelite.api.ItemID.XERICS_TALISMAN)).thenReturn(false);
		org.mockito.Mockito.when(inv.contains(net.runelite.api.ItemID.TELEPORT_TO_HOUSE)).thenReturn(false);
		org.mockito.Mockito.when(client.getVarbitValue(net.runelite.api.Varbits.SPELLBOOK)).thenReturn(0);
		org.mockito.Mockito.when(client.getBoostedSkillLevel(net.runelite.api.Skill.MAGIC)).thenReturn(70);
		org.mockito.Mockito.when(inv.count(net.runelite.api.ItemID.LAW_RUNE)).thenReturn(5);
		org.mockito.Mockito.when(inv.count(net.runelite.api.ItemID.AIR_RUNE)).thenReturn(5);
		org.mockito.Mockito.when(inv.count(net.runelite.api.ItemID.EARTH_RUNE)).thenReturn(5);

		final TeleportItem best = Home.LEELA.getTeleportItems().getClosestTeleportItemOnPlayer(client);
		org.junit.Assert.assertNotNull(best);
		org.junit.Assert.assertTrue(best.isSpell());
		org.junit.Assert.assertEquals("Teleport to House", best.getSpellName());
		org.junit.Assert.assertEquals(72, best.getDistance());
	}

	@Test
	public void testSpellUnavailableWhenOnDifferentSpellbook()
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		final net.runelite.api.ItemContainer inv = org.mockito.Mockito.mock(net.runelite.api.ItemContainer.class);
		org.mockito.Mockito.when(client.getItemContainer(net.runelite.api.InventoryID.INVENTORY)).thenReturn(inv);
		// On Ancient Magicks (varbit != 0)
		org.mockito.Mockito.when(client.getVarbitValue(net.runelite.api.Varbits.SPELLBOOK)).thenReturn(1);

		final TeleportItem best = Home.JEFF.getTeleportItems().getClosestTeleportItemOnPlayer(client);
		org.junit.Assert.assertNull(best);
	}

	@Test
	public void testDistanceCalculation()
	{
		final net.runelite.api.coords.WorldPoint near = new net.runelite.api.coords.WorldPoint(1751, 3535, 0);
		final net.runelite.api.coords.WorldPoint farBank = new net.runelite.api.coords.WorldPoint(3000, 3000, 0);

		org.junit.Assert.assertEquals(0, Home.BARBARA.getArea().distanceTo(near));
		org.junit.Assert.assertTrue(Home.BARBARA.getArea().distanceTo(farBank) > 500);
	}

	@Test
	public void testSpellWidgetDiscovery()
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		final MahoganyHomesPlugin plugin = org.mockito.Mockito.mock(MahoganyHomesPlugin.class);
		final MahoganyHomesConfig config = org.mockito.Mockito.mock(MahoganyHomesConfig.class);

		final net.runelite.api.widgets.Widget parent = org.mockito.Mockito.mock(net.runelite.api.widgets.Widget.class);
		final net.runelite.api.widgets.Widget spell = org.mockito.Mockito.mock(net.runelite.api.widgets.Widget.class);

		org.mockito.Mockito.when(client.getWidget(net.runelite.api.widgets.ComponentID.SPELLBOOK_PARENT)).thenReturn(parent);
		org.mockito.Mockito.when(parent.getNestedChildren()).thenReturn(new net.runelite.api.widgets.Widget[]{spell});
		org.mockito.Mockito.when(spell.getName()).thenReturn("<col=00ff00>Cast</col> <col=00ffff>Teleport to House</col>");
		org.mockito.Mockito.when(spell.getBounds()).thenReturn(new java.awt.Rectangle(100, 100, 24, 24));

		final TeleportWidgetOverlay overlay = new TeleportWidgetOverlay(client, plugin, config);
		final net.runelite.api.widgets.Widget found = overlay.findSpellWidget("Teleport to House");

		org.junit.Assert.assertNotNull(found);
		org.junit.Assert.assertEquals(spell, found);
	}

	@Test
	public void testDialogOptionDiscovery()
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		final MahoganyHomesPlugin plugin = new MahoganyHomesPlugin();
		plugin.setClient(client);

		final net.runelite.api.widgets.Widget parent = org.mockito.Mockito.mock(net.runelite.api.widgets.Widget.class);
		final net.runelite.api.widgets.Widget option1 = org.mockito.Mockito.mock(net.runelite.api.widgets.Widget.class);
		final net.runelite.api.widgets.Widget option2 = org.mockito.Mockito.mock(net.runelite.api.widgets.Widget.class);

		org.mockito.Mockito.when(client.getWidget(net.runelite.api.widgets.ComponentID.DIALOG_OPTION_OPTIONS)).thenReturn(parent);
		org.mockito.Mockito.when(parent.getChildren()).thenReturn(new net.runelite.api.widgets.Widget[]{option1, option2});
		org.mockito.Mockito.when(option1.getText()).thenReturn("'Lunch by the Lancalliums' - Hosidius");
		org.mockito.Mockito.when(option1.getBounds()).thenReturn(new java.awt.Rectangle(200, 300, 200, 20));

		final net.runelite.api.widgets.Widget found = plugin.findDialogOptionWidget("Lunch by the Lancalliums");

		org.junit.Assert.assertNotNull(found);
		org.junit.Assert.assertEquals(option1, found);
	}

	@Test
	public void testContractorResolution()
	{
		org.junit.Assert.assertEquals(Contractor.ANGELO, Contractor.fromHome(Home.BARBARA));
		org.junit.Assert.assertEquals(Contractor.ANGELO, Contractor.fromHome(Home.LEELA));
		org.junit.Assert.assertEquals(Contractor.ANGELO, Contractor.fromHome(Home.MARIAH));

		org.junit.Assert.assertEquals(Contractor.AMY, Contractor.fromHome(Home.LARRY));
		org.junit.Assert.assertEquals(Contractor.AMY, Contractor.fromHome(Home.NORMAN));
		org.junit.Assert.assertEquals(Contractor.AMY, Contractor.fromHome(Home.TAU));

		org.junit.Assert.assertEquals(Contractor.MARLO, Contractor.fromHome(Home.BOB));
		org.junit.Assert.assertEquals(Contractor.MARLO, Contractor.fromHome(Home.JEFF));
		org.junit.Assert.assertEquals(Contractor.MARLO, Contractor.fromHome(Home.SARAH));

		org.junit.Assert.assertEquals(Contractor.ELLIE, Contractor.fromHome(Home.JESS));
		org.junit.Assert.assertEquals(Contractor.ELLIE, Contractor.fromHome(Home.NOELLA));
		org.junit.Assert.assertEquals(Contractor.ELLIE, Contractor.fromHome(Home.ROSS));

		org.junit.Assert.assertEquals(Contractor.AMY, Contractor.fromNpcId(net.runelite.api.NpcID.AMY));
		org.junit.Assert.assertEquals(Contractor.ANGELO, Contractor.fromNpcId(net.runelite.api.NpcID.ANGELO_10413));
	}

	@Test
	public void testClosestContractor()
	{
		final net.runelite.api.coords.WorldPoint inFalador = new net.runelite.api.coords.WorldPoint(2990, 3365, 0);
		final net.runelite.api.coords.WorldPoint inHosidius = new net.runelite.api.coords.WorldPoint(1780, 3625, 0);
		final net.runelite.api.coords.WorldPoint inVarrock = new net.runelite.api.coords.WorldPoint(3240, 3474, 0);
		final net.runelite.api.coords.WorldPoint inArdougne = new net.runelite.api.coords.WorldPoint(2638, 3293, 0);

		org.junit.Assert.assertEquals(Contractor.AMY, Contractor.getClosestContractor(inFalador));
		org.junit.Assert.assertEquals(Contractor.ANGELO, Contractor.getClosestContractor(inHosidius));
		org.junit.Assert.assertEquals(Contractor.MARLO, Contractor.getClosestContractor(inVarrock));
		org.junit.Assert.assertEquals(Contractor.ELLIE, Contractor.getClosestContractor(inArdougne));
	}

	@Test
	public void testContractTierCalculationAndNaming() throws Exception
	{
		final net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		org.mockito.Mockito.when(client.getRealSkillLevel(net.runelite.api.Skill.CONSTRUCTION)).thenReturn(85);

		final MahoganyHomesPlugin plugin = new MahoganyHomesPlugin();
		final java.lang.reflect.Field clientField = MahoganyHomesPlugin.class.getDeclaredField("client");
		clientField.setAccessible(true);
		clientField.set(plugin, client);

		// With level 85 and no history, defaults to 4 (Expert)
		org.junit.Assert.assertEquals(4, plugin.getTargetContractTier());
		org.junit.Assert.assertEquals("Expert", plugin.getTargetTierName());

		// With lastContractTier explicitly set to 3 (Adept), returns 3 (Adept)
		final java.lang.reflect.Field lastTierField = MahoganyHomesPlugin.class.getDeclaredField("lastContractTier");
		lastTierField.setAccessible(true);
		lastTierField.set(plugin, 3);
		org.junit.Assert.assertEquals(3, plugin.getTargetContractTier());
		org.junit.Assert.assertEquals("Adept", plugin.getTargetTierName());
	}
}
