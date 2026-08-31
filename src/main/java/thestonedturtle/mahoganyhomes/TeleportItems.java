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

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;

public enum TeleportItems
{
	// East Ardy
	JESS(new TeleportItem(ItemID.ARDOUGNE_TELEPORT, 38),
		new TeleportItem("Ardougne Teleport", 38, 51, new Item(ItemID.LAW_RUNE, 2), new Item(ItemID.WATER_RUNE, 2)),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 40),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 40),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_4, 70),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_3, 70),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_2, 70),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_1, 70)),
	NOELLA(new TeleportItem(ItemID.ARDOUGNE_TELEPORT, 16),
		new TeleportItem("Ardougne Teleport", 16, 51, new Item(ItemID.LAW_RUNE, 2), new Item(ItemID.WATER_RUNE, 2)),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_4, 96),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_3, 96),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_2, 96),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_1, 96),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 100),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 100)),
	ROSS(new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 56),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 56),
		new TeleportItem(ItemID.ARDOUGNE_TELEPORT, 60),
		new TeleportItem("Ardougne Teleport", 60, 51, new Item(ItemID.LAW_RUNE, 2), new Item(ItemID.WATER_RUNE, 2)),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_4, 92),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_3, 92),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_2, 92),
		new TeleportItem(ItemID.ARDOUGNE_CLOAK_1, 92)),

	// Falador
	LARRY(new TeleportItem(ItemID.RING_OF_WEALTH_I5, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I4, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I3, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I2, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I1, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_5, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_4, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_3, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_2, 37, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_1, 37, "Falador"),
		new TeleportItem(ItemID.SKILLS_NECKLACE6, 38, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE5, 38, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE4, 38, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE3, 38, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE2, 38, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE1, 38, "Mining Guild"),
		new TeleportItem(ItemID.FALADOR_TELEPORT, 67),
		new TeleportItem("Falador Teleport", 67, 37, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.WATER_RUNE, 1)),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 80),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 80)),
	NORMAN(new TeleportItem(ItemID.RING_OF_WEALTH_I5, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I4, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I3, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I2, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I1, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_5, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_4, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_3, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_2, 38, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_1, 38, "Falador"),
		new TeleportItem(ItemID.SKILLS_NECKLACE6, 39, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE5, 39, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE4, 39, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE3, 39, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE2, 39, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE1, 39, "Mining Guild"),
		new TeleportItem(ItemID.FALADOR_TELEPORT, 70),
		new TeleportItem("Falador Teleport", 70, 37, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.WATER_RUNE, 1)),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 71),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 71)),
	TAU(new TeleportItem(ItemID.SKILLS_NECKLACE6, 45, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE5, 45, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE4, 45, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE3, 45, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE2, 45, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE1, 45, "Mining Guild"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I5, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I4, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I3, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I2, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I1, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_5, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_4, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_3, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_2, 47, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_1, 47, "Falador"),
		new TeleportItem(ItemID.FALADOR_TELEPORT, 78),
		new TeleportItem("Falador Teleport", 78, 37, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.WATER_RUNE, 1)),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 79),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 79)),

	// Hosidius
	BARBARA(new TeleportItem(ItemID.TELEPORT_TO_HOUSE, 14),
		new TeleportItem("Teleport to House", 14, 40, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 1), new Item(ItemID.EARTH_RUNE, 1)),
		new TeleportItem(ItemID.HOSIDIUS_TELEPORT, 14),
		new TeleportItem(ItemID.CONSTRUCT_CAPE, 14),
		new TeleportItem(ItemID.CONSTRUCT_CAPET, 14),
		new TeleportItem(ItemID.XERICS_TALISMAN, 27, "Xeric's Glade"),
		new TeleportItem(ItemID.KHAREDSTS_MEMOIRS, 70, "Lunch by the Lancalliums"),
		new TeleportItem(ItemID.BOOK_OF_THE_DEAD, 70, "Lunch by the Lancalliums")),
	LEELA(new TeleportItem(ItemID.XERICS_TALISMAN, 30, "Xeric's Glade"),
		new TeleportItem(ItemID.KHAREDSTS_MEMOIRS, 67, "Lunch by the Lancalliums"),
		new TeleportItem(ItemID.BOOK_OF_THE_DEAD, 67, "Lunch by the Lancalliums"),
		new TeleportItem(ItemID.TELEPORT_TO_HOUSE, 72),
		new TeleportItem("Teleport to House", 72, 40, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 1), new Item(ItemID.EARTH_RUNE, 1)),
		new TeleportItem(ItemID.HOSIDIUS_TELEPORT, 72)),
	MARIAH(new TeleportItem(ItemID.XERICS_TALISMAN, 50, "Xeric's Glade"),
		new TeleportItem(ItemID.KHAREDSTS_MEMOIRS, 56, "Lunch by the Lancalliums"),
		new TeleportItem(ItemID.BOOK_OF_THE_DEAD, 56, "Lunch by the Lancalliums"),
		new TeleportItem(ItemID.TELEPORT_TO_HOUSE, 100),
		new TeleportItem("Teleport to House", 100, 40, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 1), new Item(ItemID.EARTH_RUNE, 1)),
		new TeleportItem(ItemID.HOSIDIUS_TELEPORT, 100)),

	// Varrock
	BOB(new TeleportItem(ItemID.VARROCK_TELEPORT, 60),
		new TeleportItem("Varrock Teleport", 60, 25, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.FIRE_RUNE, 1)),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 100),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 100)),
	JEFF(new TeleportItem(ItemID.VARROCK_TELEPORT, 23),
		new TeleportItem("Varrock Teleport", 23, 25, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.FIRE_RUNE, 1)),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 30),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 30)),
	SARAH(new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE, 28),
		new TeleportItem(ItemID.ACHIEVEMENT_DIARY_CAPE_T, 28),
		new TeleportItem(ItemID.VARROCK_TELEPORT, 37),
		new TeleportItem("Varrock Teleport", 37, 25, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.FIRE_RUNE, 1))),

	// Contractors
	AMY(new TeleportItem(ItemID.RING_OF_WEALTH_I5, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I4, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I3, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I2, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_I1, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_5, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_4, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_3, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_2, 10, "Falador"),
		new TeleportItem(ItemID.RING_OF_WEALTH_1, 10, "Falador"),
		new TeleportItem(ItemID.FALADOR_TELEPORT, 23),
		new TeleportItem("Falador Teleport", 23, 37, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.WATER_RUNE, 1)),
		new TeleportItem(ItemID.SKILLS_NECKLACE6, 27, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE5, 27, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE4, 27, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE3, 27, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE2, 27, "Mining Guild"),
		new TeleportItem(ItemID.SKILLS_NECKLACE1, 27, "Mining Guild")),
	ANGELO(new TeleportItem(ItemID.XERICS_TALISMAN, 30, "Xeric's Glade"),
		new TeleportItem(ItemID.TELEPORT_TO_HOUSE, 40),
		new TeleportItem("Teleport to House", 40, 40, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 1), new Item(ItemID.EARTH_RUNE, 1)),
		new TeleportItem(ItemID.HOSIDIUS_TELEPORT, 40),
		new TeleportItem(ItemID.KHAREDSTS_MEMOIRS, 50, "Lunch by the Lancalliums"),
		new TeleportItem(ItemID.BOOK_OF_THE_DEAD, 50, "Lunch by the Lancalliums")),
	MARLO(new TeleportItem(ItemID.VARROCK_TELEPORT, 55),
		new TeleportItem("Varrock Teleport", 55, 25, new Item(ItemID.LAW_RUNE, 1), new Item(ItemID.AIR_RUNE, 3), new Item(ItemID.FIRE_RUNE, 1))),
	ELLIE(new TeleportItem(ItemID.ARDOUGNE_TELEPORT, 50),
		new TeleportItem("Ardougne Teleport", 50, 51, new Item(ItemID.LAW_RUNE, 2), new Item(ItemID.WATER_RUNE, 2)));

	private final TeleportItem[] teleportItems;

	TeleportItems(TeleportItem... items)
	{
		this.teleportItems = items;
	}

	public TeleportItem getClosestTeleportItemOnPlayer(final Client client)
	{
		for (final TeleportItem teleportItem : teleportItems)
		{
			if (teleportItem.isAvailableOnPlayer(client))
			{
				return teleportItem;
			}
		}

		return null;
	}
}
