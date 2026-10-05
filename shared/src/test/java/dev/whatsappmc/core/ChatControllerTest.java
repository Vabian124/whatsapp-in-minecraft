package dev.whatsappmc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChatControllerTest {
	@Test
	void replyIsQueuedAndAckedWithoutSendingNormalChat() {
		FakeOutput output = new FakeOutput();
		List<String> sent = new ArrayList<>();
		ChatController chat = new ChatController(".h ", output, (id, text) -> sent.add(id + ":" + text));

		assertTrue(chat.allowChat("hello server"));
		assertFalse(chat.allowChat(".h hi"));
		chat.onCanceled("hello server");
		chat.onCanceled(".h hi");
		assertEquals(List.of("c1:hi"), sent);

		chat.onFrame(frame("ack", "c1", null));
		output.ready = true;
		chat.tick();
		assertEquals(List.of("you:hi"), output.lines);
	}

	@Test
	void incomingIsSplitAndASecondContactShapeIsJustText() {
		FakeOutput output = new FakeOutput();
		output.ready = true;
		ChatController chat = new ChatController(".h ", output, (id, text) -> { });
		Frame incoming = new Frame();
		incoming.type = "in";
		incoming.name = "Ada";
		incoming.text = "a".repeat(241);
		chat.onFrame(incoming);
		chat.tick();
		assertEquals(2, output.lines.size());
		assertTrue(output.lines.get(0).startsWith("in:Ada:"));
	}

	@Test
	void sameStatusIsShownOnce() {
		FakeOutput output = new FakeOutput();
		output.ready = true;
		ChatController chat = new ChatController(".h ", output, (id, text) -> { });
		chat.onFrame(Frame.status("open"));
		chat.onFrame(Frame.status("open"));
		chat.tick();
		assertEquals(List.of("status:WhatsApp connected."), output.lines);
	}

	@Test
	void emptyReplyAndANackStayOnThisComputer() {
		FakeOutput output = new FakeOutput();
		output.ready = true;
		List<String> sent = new ArrayList<>();
		ChatController chat = new ChatController(".h ", output, (id, text) -> sent.add(text));
		chat.onCanceled(".h ");
		chat.onCanceled(".h hi");
		chat.onFrame(Frame.nack("c1"));
		chat.tick();
		assertEquals(List.of("hi"), sent);
		assertEquals(List.of("status:Nothing to send.", "status:Message was not delivered."), output.lines);
	}

	private static Frame frame(String type, String id, String text) {
		Frame frame = new Frame();
		frame.type = type;
		frame.id = id;
		frame.text = text;
		return frame;
	}
}
