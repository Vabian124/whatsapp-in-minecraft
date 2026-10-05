package dev.whatsappmc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroupRelayTest {
	@Test
	void groupLinesShowAndAPrivateLineDoesNot() {
		FakeOutput output = new FakeOutput();
		output.ready = true;
		GroupRelay relay = new GroupRelay(output, (id, text) -> { });
		Frame direct = new Frame();
		direct.type = "in";
		direct.lane = "direct";
		direct.name = "Ada";
		direct.text = "secret";
		Frame group = new Frame();
		group.type = "in";
		group.lane = "group";
		group.name = "Bea";
		group.text = "hello cave";
		relay.onFrame(direct);
		relay.onFrame(group);
		relay.tick();
		assertEquals(1, output.lines.size());
		assertTrue(output.lines.get(0).startsWith("in:Bea:"));
		assertFalse(output.lines.get(0).contains("secret"));
	}

	@Test
	void playerRosterIsRequestedOnce() {
		FakeOutput output = new FakeOutput();
		GroupRelay relay = new GroupRelay(output, (id, text) -> { });
		Frame ask = new Frame();
		ask.type = "ask";
		ask.text = "players";
		relay.onFrame(ask);
		relay.onFrame(ask);
		assertTrue(relay.consumePlayers());
		assertFalse(relay.consumePlayers());
	}
}
