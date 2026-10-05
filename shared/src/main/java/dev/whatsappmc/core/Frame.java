package dev.whatsappmc.core;

public final class Frame {
	public int v = 1;
	public String type;
	public String token;
	public String state;
	public String id;
	public String text;
	public String kind;
	public String name;
	public String lane;

	public static Frame status(String state) {
		Frame frame = new Frame();
		frame.type = "status";
		frame.state = state;
		return frame;
	}

	public static Frame nack(String id) {
		Frame frame = new Frame();
		frame.type = "nack";
		frame.id = id;
		return frame;
	}
}
