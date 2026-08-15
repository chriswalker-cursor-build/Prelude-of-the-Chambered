package com.mojang.escape.app;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.IntegerScaler;
import com.mojang.escape.input.InputHandler;
import com.mojang.escape.input.MouseLook;
import com.mojang.escape.render.Screen;
import com.mojang.escape.session.Game;
import com.mojang.escape.ui.HudRenderer;

public class EscapeComponent extends Canvas implements Runnable, InputHandler.MouseMotionSink {
	private static final long serialVersionUID = 1L;

	private static final int WIDTH = 160;
	private static final int HEIGHT = 120;
	private static final int SCALE = 4;

	private boolean running;
	private Thread thread;

	private Game game;
	private Screen screen;
	private BufferedImage img;
	private int[] pixels;
	private InputHandler inputHandler;
	private Cursor emptyCursor, defaultCursor;
	private boolean hadFocus = false;

	private final EscapeSettings.PresentMode presentMode = EscapeSettings.presentMode();
	private MouseLook mouseLook;
	private Robot robot;
	private HudRenderer hudRenderer;

	public EscapeComponent() {
		Dimension size = new Dimension(WIDTH * SCALE, HEIGHT * SCALE);
		setSize(size);
		setPreferredSize(size);
		if (presentMode == EscapeSettings.PresentMode.FIXED4) {
			setMinimumSize(size);
			setMaximumSize(size);
		}

		game = new Game();
		screen = new Screen(WIDTH, HEIGHT);

		img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

		inputHandler = new InputHandler();

		if (EscapeSettings.lookMode() == EscapeSettings.LookMode.MOUSE_LERP) {
			mouseLook = new MouseLook();
			inputHandler.mouseMotionSink = this;
			try {
				robot = new Robot();
			} catch (AWTException e) {
				// Without a Robot the cursor cannot be re-centered; deltas still
				// apply until the pointer reaches the window edge.
			}
		}

		addKeyListener(inputHandler);
		addFocusListener(inputHandler);
		addMouseListener(inputHandler);
		addMouseMotionListener(inputHandler);
		emptyCursor = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "empty");
		defaultCursor = getCursor();
	}

	public synchronized void start() {
		if (running) return;
		running = true;
		thread = new Thread(this);
		thread.start();
	}

	public synchronized void stop() {
		if (!running) return;
		running = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		int frames = 0;

		double unprocessedSeconds = 0;
		long lastTime = System.nanoTime();
		double secondsPerTick = 1 / 60.0;
		int tickCount = 0;

		requestFocus();

		while (running) {
			long now = System.nanoTime();
			long passedTime = now - lastTime;
			lastTime = now;
			if (passedTime < 0) passedTime = 0;
			if (passedTime > 100000000) passedTime = 100000000;

			unprocessedSeconds += passedTime / 1000000000.0;

			boolean ticked = false;
			while (unprocessedSeconds > secondsPerTick) {
				tick();
				unprocessedSeconds -= secondsPerTick;
				ticked = true;

				tickCount++;
				if (tickCount % 60 == 0) {
					System.out.println(frames + " fps");
					lastTime += 1000;
					frames = 0;
				}
			}

			if (ticked) {
				render();
				frames++;
			} else {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	private void tick() {
		if (hasFocus()) {
			if (mouseLook != null && game.menu == null && game.player != null) {
				game.player.rot += mouseLook.tick();
			}
			game.tick(inputHandler.keys);
		}
	}

	public void mouseMovedTo(int x, int y) {
		if (mouseLook == null || !hasFocus()) return;
		int cx = getWidth() / 2;
		int cy = getHeight() / 2;
		int dx = x - cx;
		// The Robot's own re-center event arrives exactly at (cx, cy) and is ignored.
		if (dx == 0 && y == cy) return;
		mouseLook.mouseDelta(dx);
		if (robot != null && isShowing()) {
			Point screen = getLocationOnScreen();
			robot.mouseMove(screen.x + cx, screen.y + cy);
		}
	}

	private void render() {
		if (hadFocus != hasFocus()) {
			hadFocus = !hadFocus;
			setCursor(hadFocus ? emptyCursor : defaultCursor);
		}
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(3);
			return;
		}

		screen.render(game, hasFocus());

		for (int i = 0; i < WIDTH * HEIGHT; i++) {
			pixels[i] = screen.pixels[i];
		}

		Graphics g = bs.getDrawGraphics();
		IntegerScaler.Placement p;
		if (presentMode == EscapeSettings.PresentMode.INTEGER_FILL) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			p = IntegerScaler.fit(WIDTH, HEIGHT, getWidth(), getHeight());
			g.drawImage(img, p.x(), p.y(), p.w(), p.h(), null);
		} else {
			g.fillRect(0, 0, getWidth(), getHeight());
			p = new IntegerScaler.Placement(SCALE, 0, 0, WIDTH * SCALE, HEIGHT * SCALE);
			g.drawImage(img, 0, 0, WIDTH * SCALE, HEIGHT * SCALE, null);
		}
		if (EscapeSettings.hudMode() == EscapeSettings.HudMode.HOTBAR
				&& game.level != null && game.menu == null && game.pauseTime == 0) {
			if (hudRenderer == null) hudRenderer = new HudRenderer();
			hudRenderer.render(g, game, p);
		}
		g.dispose();
		bs.show();
	}

	public static void main(String[] args) {
		EscapeComponent game = new EscapeComponent();

		JFrame frame = new JFrame("Prelude of the Chambered!");

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(game, BorderLayout.CENTER);

		frame.setContentPane(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setResizable(EscapeSettings.presentMode() == EscapeSettings.PresentMode.INTEGER_FILL);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		game.start();
	}
}
