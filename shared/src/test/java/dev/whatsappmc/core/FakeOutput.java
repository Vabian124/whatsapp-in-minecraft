package dev.whatsappmc.core;

import java.util.ArrayList;
import java.util.List;

final class FakeOutput implements GameOutput {
	final List<String> lines = new ArrayList<>();
	boolean ready;

	@Override
	public void showIncoming(String name, String body) {
		lines.add("in:" + name + ":" + body);
	}

	@Override
	public void showOwn(String body) {
		lines.add("you:" + body);
	}

	@Override
	public void showStatus(String text) {
		lines.add("status:" + text);
	}

	@Override
	public boolean ready() {
		return ready;
	}
}
