package dev.whatsappmc.core;

public interface GameOutput {
	void showIncoming(String name, String body);

	void showOwn(String body);

	void showStatus(String text);

	boolean ready();
}
