/*
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *  
 *  (C) Copyright 2012 m4tx
 *  http://www.m4tx.pl/
 */

package pl.m4tx.Carnobyl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

public class Main extends JFrame implements Runnable, KeyListener {
	/**
	 * Name of the game.
	 */
	protected final String name = "Carnobyl64k";

	/**
	 * Size of the window.
	 */
	protected final static int W = 800, H = 600;
	/**
	 * Max speed of the car.
	 */
	protected final double MAX_SPEED = 600;

	protected int carX = 0, carY = 0, carPosX, carPosY, killed = 0, total = 0;
	protected String gameOverTitile = null, gameOverDesc = null;

	protected Image carImg, grassImg, checkedImg;
	/**
	 * Some images shown on the map.
	 */
	protected Image[] pedestrian, killedPedestrian, blood, road;
	/**
	 * Background image.
	 */
	protected BufferedImage back;
	/**
	 * Container contains list of the pedestrians.
	 * <p>
	 * It contains list of <code>int[]</code> objects, which has got the
	 * following format:<br>
	 * <code>{x, y, id, killed[, blood id]}</code><br>
	 * id = id of the {@link #pedestrian} image<br>
	 * killed = 0 means alive, 1 means killed<br>
	 * blood id = optional. Id of the {@link #blood} image.
	 */
	protected ArrayList<int[]> pedestrians;

	/**
	 * Currently pressed keys.
	 * <p>
	 * Example code that checks if "W" key is pressed:
	 * <code>pressed.contains(KeyEvent.VK_W)</code>
	 * 
	 * @see KeyEvent
	 */
	protected Set<Integer> pressed;

	/**
	 * Array contains the tile map.
	 */
	protected int[][] map;

	/**
	 * Main method of the application.
	 * 
	 * @param args
	 *            arguments
	 */
	public static void main(final String[] args) {
		final Main m = new Main();
		m.start(args);
	}

	/**
	 * "Second" main method of the game, invoked by {@link #main(String[])}.
	 * 
	 * @param args
	 *            arguments
	 */
	public void start(final String args[]) {
		setTitle(name);
		setLayout(new GridLayout(1, 0));
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		setSize(W, H);
		setLocationRelativeTo(null);
		setResizable(false);

		pressed = new HashSet<Integer>();
		addKeyListener(this);

		setVisible(true);

		long seed;
		if (args.length == 2 && args[0].equals("--seed")) {
			seed = Long.parseLong(args[1]);
		} else {
			seed = System.currentTimeMillis();
		}
		this.seed = seed;
		random.setSeed(seed);

		carPosX = W / 2 - 32;
		carPosY = H / 2 - 64;

		grassImg = getImage(IMG_GRASS);
		checkedImg = getImage(IMG_CHECKERED);
		back = getCmptblImg(W + 64, H + 64, Transparency.OPAQUE);
		Graphics backG = back.getGraphics();
		for (int x = 0; x < (W + 128) / 64; x++) {
			for (int y = 0; y < (H + 128) / 64; y++) {
				backG.drawImage(grassImg, x * 64, y * 64, 64, 64, null);
			}
		}
		createCar();
		pedestrian = new Image[20];
		killedPedestrian = new Image[20];
		blood = new Image[20];
		for (int i = 0; i < 20; i++) {
			pedestrian[i] = getImage(IMG_PEDESTRIAN);
			killedPedestrian[i] = getImage(IMG_KILLED_PEDESTRIAN);
			blood[i] = getImage(IMG_BLOOD);
		}
		road = new Image[IMG_BUILDING - 150];
		for (int i = 151; i <= IMG_BUILDING; i++) {
			road[i - 151] = getImage(i);
		}
		pedestrians = new ArrayList<int[]>();

		//
		// Generating map
		//
		int size = random.nextInt(3) + 1;
		map = new int[size * 4 + 22][size * 4 + 22];

		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map.length; y++) {
				if (x == 0 || y == 0 || x == map.length - 1
						|| y == map.length - 1) {
					map[x][y] = IMG_LEVEL_END;
				} else {
					map[x][y] = -1;
				}
			}
		}
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int tileX = 11 + x * 3, tileY = 11 + y * 3;
				for (int x2 = (x == 0 ? 0 : 1); x2 < tile[0].length; x2++) {
					for (int y2 = (y == 0 ? 0 : 1); y2 < tile.length; y2++) {
						map[tileX + x2][tileY + y2] = tile[x2][y2];
					}
				}
			}
		}

		int x, y;
		for (int i = 0; i < size * 20; i++) {
			do {
				y = x = 8 * 128 + 50;
				x += random.nextInt(size * 128 * 4 + 100);
				y += random.nextInt(size * 128 * 4 + 100);
			} while (map[x / 128][y / 128] == IMG_BUILDING);

			pedestrians.add(new int[] { x, y,
					random.nextInt(pedestrian.length), 0 });
		}
		total = size * 20;
		//
		//
		//

		createBufferStrategy(2);

		new Thread(this).start();
	}

	@Override
	public synchronized void keyPressed(KeyEvent e) {
		pressed.add(e.getKeyCode());
	}

	@Override
	public synchronized void keyReleased(KeyEvent e) {
		pressed.remove(e.getKeyCode());
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	/**
	 * Main loop.
	 */
	@Override
	public void run() {
		double fps = 0, now, tpf, speed = 0, velocityX = 0, rotation = 0, sleepTime, frameTime = System
				.nanoTime() / 1000000, time = 60, gameOverTime = 0;
		int camX = 0, camY = 0;
		BufferStrategy bf = this.getBufferStrategy();
		Graphics2D g = null;
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		rh.add(new RenderingHints(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY));
		Font fnt = new Font("SansSerif", Font.BOLD, W / 8);
		FontRenderContext frc;
		Rectangle2D car = new Rectangle2D.Float(carPosX + 8, carPosY + 8, 48,
				112);
		Shape transCar;
		AffineTransform trans;

		Font f = new Font("Serif", Font.BOLD, W / 10), f2 = new Font(
				"SansSerif", Font.BOLD, W / 40);
		TextLayout textTl;
		Shape outline;
		Rectangle r;
		while (true) {
			// TPF & FPS
			now = System.nanoTime() / 1000000;
			tpf = now - frameTime;
			frameTime = now;
			if (tpf != 0) {
				fps = 1000 / tpf;
			}

			//
			// Calculations
			//
			time -= tpf / 1000;
			if (time < 0) {
				if (gameOverTitile == null) {
					gameOverTitile = "BITCH, PLEASE";
					gameOverDesc = "U WERE 2 SLOW!";
				}
			}

			// Steering the car
			if (!(pressed.contains(KeyEvent.VK_W) && pressed
					.contains(KeyEvent.VK_S))) {
				if (pressed.contains(KeyEvent.VK_W)) {
					velocityX = Math.cos(speed / MAX_SPEED / 2 * Math.PI);
				} else if (pressed.contains(KeyEvent.VK_S)) {
					if (speed > 0) {
						velocityX = -2;
					} else {
						velocityX = -Math.cos(speed / -MAX_SPEED / 2 * Math.PI) / 2;
					}
				} else {
					if (speed < 0.05 && speed > -0.05) {
						velocityX = 0;
						speed = 0;
					} else {
						velocityX = (speed > 0 ? -0.1 : 0.1);
					}
				}
			}
			speed += velocityX * tpf / 5;

			if (!(pressed.contains(KeyEvent.VK_A) && pressed
					.contains(KeyEvent.VK_D))) {
				if (pressed.contains(KeyEvent.VK_A)) {
					rotation -= Math.sin(tpf / 2000 * speed / MAX_SPEED
							* Math.PI);
				} else if (pressed.contains(KeyEvent.VK_D)) {
					rotation += Math.sin(tpf / 2000 * speed / MAX_SPEED
							* Math.PI);
				}
			}
			while (rotation > 1) {
				rotation -= 2;
			}
			while (rotation < -1) {
				rotation += 2;
			}

			// Used in collision detecting
			trans = new AffineTransform();
			trans.rotate(rotation * Math.PI, carPosX + 32, carPosY + 64);
			transCar = trans.createTransformedShape(car);

			// VelocityX - velocity of the car on the X axis - that's why we add
			// it to Y position.
			carX += speed * tpf / 1000 * -Math.sin(rotation * Math.PI);
			carY += speed * tpf / 1000 * Math.cos(rotation * Math.PI);

			camX = carX - carPosX;
			camY = carY - carPosY;

			//
			// Drawing
			//
			try {
				g = (Graphics2D) bf.getDrawGraphics();

				/*
				 * Drawing background
				 * 
				 * To increase the rendering speed, the background is stored at
				 * the first in 64x64 tiles, and then bundled into one width+64,
				 * height+64 background image.
				 * 
				 * The game calculates this bundle's position on the screen by
				 * the following way:
				 * 
				 * (camY < 0 ? 64 : 0) + (camY % 64) - 64
				 * 
				 * How it works? CamY is camera's position on the Y axis. 64 is
				 * the tile size. If we'll modulo cam's pos by tile size, we'll
				 * get the background translation on Y axis. But when user'll go
				 * into negative values on Y axis, we'll also have to subtract
				 * pos % 64 from 64. That's the whole trick.
				 */
				g.drawImage(back, (camX < 0 ? 64 : 0) + (camX % 64) - 64,
						(camY < 0 ? 64 : 0) + (camY % 64) - 64, null);

				g.setRenderingHints(rh);

				// Render map
				g.translate(camX, camY);
				for (int x = -camX / ROAD - 1; x < (-camX + W) / ROAD + 1; x++) {
					for (int y = -camY / ROAD - 1; y < (-camY + H) / ROAD + 1; y++) {
						try {
							if (map[x][y] != -1) {
								g.drawImage(road[map[x][y] - 151], x * ROAD, y
										* ROAD, null);
							}
							if ((map[x][y] == IMG_BUILDING || map[x][y] == IMG_LEVEL_END)
									&& transCar.intersects(x * ROAD + camX, y
											* ROAD + camY, ROAD, ROAD)) {
								if (speed > 500) {
									if (gameOverTitile == null) {
										gameOverTitile = "BITCH, PLEASE";
										gameOverDesc = "Y U BREAK UR CAR?!";
									}
								}
								speed = -speed;
							}
						} catch (Exception e) {
						}
					}
				}
				// Render pedestrians
				for (int i = 0; i < pedestrians.size(); i++) {
					int[] ped = pedestrians.get(i);
					if (ped[0] > -camX - 64 && ped[0] < -camX + W + 32
							&& ped[1] > -camY - 64 && ped[1] < -camY + H + 32) {
						if (ped[3] == 0) {
							if (transCar.intersects(ped[0] + camX - 12, ped[1]
									+ camY - 12, 24, 24)) {
								pedestrians.remove(ped);
								pedestrians
										.add(new int[] { ped[0], ped[1],
												ped[2], 1,
												random.nextInt(blood.length) });
								time += 1.5;
								killed++;

								if (killed == total) {
									if (gameOverTitile == null) {
										gameOverTitile = "CONGRATZ";
										gameOverDesc = "U KILLED'EM'ALL! THX 4 PLAYIN'!";
									}
								}
							}
						}

						if (ped[3] != 0) {
							g.drawImage(blood[ped[4]], ped[0] - TILE / 2,
									ped[1] - TILE / 2, null);
						}
						g.drawImage((ped[3] == 0 ? pedestrian
								: killedPedestrian)[ped[2]], ped[0] - TILE / 2,
								ped[1] - TILE / 2, null);
					}
				}
				g.translate(-camX, -camY);

				// Render car
				g.translate(carPosX + 32, carPosY + 64);
				g.rotate(rotation * Math.PI);
				drawCar(g);
				g.rotate(-rotation * Math.PI);
				g.translate(-carPosX - 32, -carPosY - 64);

				// Killed / total (pedestrians)
				g.drawString(killed + " / " + total, 0, 10);

				// Time limit
				frc = g.getFontRenderContext();
				textTl = new TextLayout(Integer.toString(Math
						.max((int) time, 0)), fnt, frc);
				outline = textTl.getOutline(null);
				r = outline.getBounds();
				int w = W / 2 - (r.width / 2);
				int h = r.height;
				g.translate(w, h);
				g.setStroke(new BasicStroke(5));
				g.setColor(Color.BLACK);
				g.draw(outline);
				g.setClip(outline);
				g.drawImage(checkedImg, r.x, r.y, this);
				g.translate(-w, -h);
				g.setClip(null);

				// Render "game over" screen
				if (gameOverTitile != null) {
					gameOverTime += tpf;
					if (gameOverTime < 4000) {
						g.setColor(new Color(0f, 0f, 0f,
								(float) gameOverTime / 4000));
						g.fillRect(0, 0, W, H);
					} else {
						g.setColor(new Color(0, 0, 0, 255));
						g.fillRect(0, 0, W, H);

						textTl = new TextLayout(gameOverTitile, f, frc);
						outline = textTl.getOutline(null);
						r = outline.getBounds();
						int tw = W / 2 - (r.width / 2);
						int th = H / 2 + (r.height / 2);
						g.translate(tw, th);
						g.setColor(Color.GRAY);
						g.draw(outline);
						g.setClip(outline);
						g.drawImage(checkedImg, r.x, r.y, this);
						g.translate(-tw, -th);
						g.setClip(null);

						textTl = new TextLayout(gameOverDesc, f2, frc);
						outline = textTl.getOutline(null);
						Rectangle r2 = outline.getBounds();
						tw = W / 2 - (r2.width / 2);
						th = H / 2 + (r2.height / 2) + r.height;
						g.translate(tw, th);
						g.setColor(Color.BLUE);
						g.draw(outline);
						g.setColor(Color.WHITE);
						g.fill(outline);
						g.translate(-tw, -th);

						if (gameOverTime < 6000) {
							g.setColor(new Color(0f, 0f, 0f,
									(float) (1f - (gameOverTime - 4000) / 2000)));
							g.fillRect(0, 0, W, H);
						}
					}
				}
			} finally {
				// It is best to dispose() a Graphics object when done with it.
				g.dispose();
			}

			// Shows the contents of the backbuffer on the screen.
			bf.show();

			// Tell the System to do the drawing now, otherwise it can take a
			// few extra ms until drawing is done
			Toolkit.getDefaultToolkit().sync();

			// Framerate limit
			if (fps > 60) {
				try {
					sleepTime = (float) 1000 / (float) 60 - tpf;
					Thread.sleep((long) sleepTime);
				} catch (InterruptedException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							null, e);
				}
			}
		}
	}

	protected final static int IMG_GRASS = 101;
	protected final static int IMG_ASPHALT = 151;
	protected final static int IMG_ROAD_VERT = 152;
	protected final static int IMG_ROAD_HORI = 153;
	protected final static int IMG_ROAD_END_UP = 154;
	protected final static int IMG_ROAD_END_DOWN = 155;
	protected final static int IMG_ROAD_END_LEFT = 156;
	protected final static int IMG_ROAD_END_RIGHT = 157;
	protected final static int IMG_LEVEL_END = 158;
	protected final static int IMG_BUILDING = 159;
	protected final static int IMG_PEDESTRIAN = 201;
	protected final static int IMG_KILLED_PEDESTRIAN = 202;
	protected final static int IMG_BLOOD = 203;
	protected final static int IMG_CHECKERED = 1;

	/**
	 * Single tile of the map.
	 */
	protected final static int tile[][] = {
			{ -1, IMG_ROAD_END_UP, -1, -1, IMG_ROAD_END_UP, -1 },
			{ IMG_ROAD_END_LEFT, IMG_ASPHALT, IMG_ROAD_HORI, IMG_ROAD_HORI,
					IMG_ASPHALT, IMG_ROAD_END_RIGHT },
			{ -1, IMG_ROAD_VERT, IMG_BUILDING, IMG_BUILDING, IMG_ROAD_VERT, -1 },
			{ -1, IMG_ROAD_VERT, IMG_BUILDING, IMG_BUILDING, IMG_ROAD_VERT, -1 },
			{ IMG_ROAD_END_LEFT, IMG_ASPHALT, IMG_ROAD_HORI, IMG_ROAD_HORI,
					IMG_ASPHALT, IMG_ROAD_END_RIGHT },
			{ -1, IMG_ROAD_END_DOWN, -1, -1, IMG_ROAD_END_DOWN, -1 } };

	/**
	 * Seed used by map and image generator.
	 */
	protected long seed = 1;
	/**
	 * Random number generator.
	 */
	protected Random random = new Random();

	/**
	 * Size of the most of the images.
	 */
	protected final static int TILE = 64;
	/**
	 * Size of the road tile.
	 */
	protected final static int ROAD = 128;

	/**
	 * Generates an image.
	 * 
	 * @param image
	 *            ID of the image specified by <code>IMG_*</code> static fields.
	 * @return Generated image.
	 */
	protected Image getImage(int image) {
		BufferedImage img = null;

		if (image == IMG_CHECKERED) {
			img = getCmptblImg(W, H, Transparency.OPAQUE);
		} else if (image > 200) {
			img = getCmptblImg(TILE, TILE, Transparency.TRANSLUCENT);
		} else if (image > 150) {
			img = getCmptblImg(ROAD, ROAD, Transparency.OPAQUE);
		} else if (image > 100) {
			img = getCmptblImg(TILE, TILE, Transparency.OPAQUE);
		}
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		switch (image) {
		case IMG_GRASS: {
			for (int x = 0; x < TILE; x++) {
				for (int y = 0; y < TILE; y++) {
					img.setRGB(x, y, rndPxl(255, 0, 0, 25, 230, 25, 0, 25)
							.getRGB());
				}
			}
			break;
		}
		case IMG_ASPHALT: {
			for (int x = 0; x < ROAD; x++) {
				for (int y = 0; y < ROAD; y++) {
					img.setRGB(
							x,
							y,
							(random.nextInt(4) < 3 ? rndPxl(255, 0, 0, 40, 0,
									40, 0, 40).getRGB() : rndPxl(255, 0, 80,
									20, 80, 20, 80, 20).getRGB()));
				}
			}
			break;
		}
		case IMG_ROAD_VERT:
			g.rotate(Math.PI / 2);
			g.translate(0, -ROAD);
			g.drawImage(getImage(IMG_ROAD_HORI), 0, 0, null);
			break;
		case IMG_ROAD_HORI:
			g.drawImage(getImage(IMG_ASPHALT), 0, 0, null);
			g.setStroke(new BasicStroke(5));
			g.setColor(rndPxl(255, 0, 245, 10, 245, 10, 245, 10));
			g.drawLine(ROAD / 2, 32, ROAD / 2, ROAD - 32);
			g.setColor(rndPxl(255, 0, 180, 20, 180, 20, 180, 20));
			g.drawLine(0, 0, 0, ROAD);
			g.drawLine(ROAD, 0, ROAD, ROAD);
			break;
		case IMG_ROAD_END_UP:
			g.drawImage(getImage(IMG_ROAD_VERT), 0, 0, null);
			g.setColor(rndPxl(255, 0, 180, 20, 180, 20, 180, 20));
			g.setStroke(new BasicStroke(5));
			g.drawLine(0, 0, ROAD, 0);
			break;
		case IMG_ROAD_END_DOWN:
			g.rotate(Math.PI);
			g.translate(-ROAD, -ROAD);
			g.drawImage(getImage(IMG_ROAD_END_UP), 0, 0, null);
			break;
		case IMG_ROAD_END_LEFT:
			g.rotate(-Math.PI / 2);
			g.translate(-ROAD, 0);
			g.drawImage(getImage(IMG_ROAD_END_UP), 0, 0, null);
			break;
		case IMG_ROAD_END_RIGHT:
			g.rotate(Math.PI / 2);
			g.translate(0, -ROAD);
			g.drawImage(getImage(IMG_ROAD_END_UP), 0, 0, null);
			break;
		case IMG_LEVEL_END: {
			int p1 = rndPxl(255, 0, 200, 20, 0, 10, 0, 10).getRGB();
			int p2 = rndPxl(255, 0, 245, 10, 245, 10, 245, 10).getRGB();
			for (int x = 0; x < ROAD; x++) {
				for (int y = 0; y < ROAD; y++) {
					img.setRGB(x, y, ((x + y) % 16 < 10 ? p1 : p2));
				}
			}
			break;
		}
		case IMG_BUILDING:
			for (int x = 0; x < ROAD; x++) {
				for (int y = 0; y < ROAD; y++) {
					img.setRGB(x, y, rndPxl(255, 0, 200, 20, 200, 20, 200, 20)
							.getRGB());
				}
			}
			g.setStroke(new BasicStroke(3));
			g.setColor(rndPxl(255, 0, 100, 10, 100, 10, 100, 10));
			g.drawLine(0, 0, 0, ROAD);
			g.drawRect(0, 0, ROAD, ROAD);
			break;
		case IMG_CHECKERED: {
			int p1 = rndPxl(255, 0, 170, 50, 0, 50, 0, 50).getRGB();
			int p2 = rndPxl(255, 0, 205, 50, 205, 50, 205, 50).getRGB();
			for (int x = 0; x < W; x++) {
				for (int y = 0; y < H; y++) {
					img.setRGB(
							x,
							y,
							((((y % 20) > 10 ? x + 10 : x) % 20) < 10 ? p1 : p2));
				}
			}
			break;
		}
		case IMG_BLOOD: {
			// Puddles
			int x, y, w;
			for (int i = 0; i < 16; i++) {
				x = random.nextInt(TILE);
				y = random.nextInt(TILE);
				w = random.nextInt(TILE / 2);
				if ((x + w) > TILE) {
					x -= x + w - TILE;
				}
				if ((y + w) > TILE) {
					y -= y + w - TILE;
				}
				g.setColor(rndPxl(255, 0, 200, 10, 40, 10, 30, 20));
				g.fillOval(x, y, w, w);
			}
			break;
		}
		case IMG_PEDESTRIAN:
			g.translate(TILE / 2, TILE / 2);
			g.rotate((random.nextDouble() - 1) * 2 * Math.PI);
			g.translate(-TILE / 2, -TILE / 2);

			// Trunk
			g.setColor(rndPxl(255, 0, 0, 255, 0, 255, 0, 255));
			g.fillRoundRect(24, 32, 16, 5, 2, 2);
			// Head
			g.setColor(rndPxl(255, 0, 245, 10, 120, 20, 60, 60));
			g.fillOval(28, 28, 8, 8);

			break;
		case IMG_KILLED_PEDESTRIAN: {
			g.translate(TILE / 2, TILE / 2);
			g.rotate((random.nextDouble() - 1) * 2 * Math.PI);
			g.translate(-TILE / 2, -TILE / 2);

			// Neck
			g.setColor(rndPxl(255, 0, 245, 10, 200, 10, 185, 15));
			g.setStroke(new BasicStroke(3));
			g.drawLine(19, 24, 19, 32);
			// Head
			g.setColor(rndPxl(255, 0, 245, 10, 120, 20, 60, 60));
			g.fillOval(16, 16, 8, 8);
			// Trunk
			g.setColor(rndPxl(255, 0, 0, 255, 0, 255, 0, 255));
			g.fillRoundRect(12, 25, 16, 24, 8, 8);

			// Limbs
			double xPos[] = new double[4];
			double yPos[] = new double[4];
			for (int i = 0; i < 4; i++) {
				// Rotate the limb by random number of degrees and calculate its
				// position
				int r = random.nextInt(360);
				xPos[i] = Math.sin(Math.toRadians(r)) * 12;
				yPos[i] = Math.cos(Math.toRadians(r)) * 12;
			}
			g.setStroke(new BasicStroke(3));
			g.setColor(rndPxl(255, 0, 245, 10, 200, 10, 185, 15));
			// Left arm
			g.drawLine(14, 27, 14 + (int) xPos[0], 27 + (int) yPos[0]);
			// Right arm
			g.drawLine(28, 27, 28 + (int) xPos[1], 27 + (int) yPos[1]);
			// Left leg
			g.drawLine(14, 48, 14 + (int) xPos[2], 48 + (int) yPos[2]);
			// Right leg
			g.drawLine(28, 48, 28 + (int) xPos[3], 48 + (int) yPos[3]);

			break;
		}
		}

		return img;
	}

	/**
	 * Color of the outline of the car.
	 */
	protected Color outline;
	protected Area c, fwin, bwin, headlights, airInlet = null;
	/**
	 * Color of the car.
	 */
	protected int cr, cg, cb, avg;

	/**
	 * Generates a car.
	 */
	protected void createCar() {
		Rectangle2D.Float rect = new Rectangle2D.Float();
		Ellipse2D.Float ellipse = new Ellipse2D.Float();

		//
		// Car
		//
		rect.setFrame(8, 24, 48, 72);
		c = new Area(rect);

		// Front
		ellipse.setFrame(6, 0, 52, 58);
		c.add(new Area(ellipse));

		// Back
		ellipse.setFrame(6, 55, 52, 78);
		rect.setFrame(0, 122, 64, 20);
		Area tmp = new Area(ellipse);
		tmp.subtract(new Area(rect));
		c.add(tmp);
		c.add(new Area(new Arc2D.Float(13, 112, 38, 15, 180, 180, Arc2D.PIE)));

		// Mirrors
		ellipse.setFrame(0, 44, 16, 8);
		rect.setFrame(0, 48, 16, 4);
		tmp = new Area(ellipse);
		tmp.subtract(new Area(rect));
		c.add(tmp);
		ellipse.x = 48;
		rect.x = 48;
		tmp = new Area(ellipse);
		tmp.subtract(new Area(rect));
		c.add(tmp);

		// Front window
		fwin = new Area(new Arc2D.Float(8, 30, 48, 30, 40, 100, Arc2D.CHORD));
		fwin.add(new Area(createTrapezoid(17, 35, 37, 30, 15, false)));

		// Back window
		bwin = new Area(new Arc2D.Float(20, 95, 24, 20, 180, 180, Arc2D.CHORD));
		bwin.add(new Area(createTrapezoid(20, 95, 28, 24, 10, false)));

		// Headlights
		headlights = new Area(new Arc2D.Float(9, 0, 10, 10, 240, 130,
				Arc2D.OPEN));
		headlights.add(new Area(new Arc2D.Float(45, 0, 10, 10, 160, 130,
				Arc2D.OPEN)));

		// Air inlet
		if ((seed & 0x01) == 1) {
			airInlet = new Area(createTrapezoid(27.5f, 55, 10, 8, 5, true));
		}

		// "Randomize" car's color from the seed
		cr = (int) (seed & 0xff);
		cg = (int) ((seed & 0xff00) >> 8);
		cb = (int) ((seed & 0xff0000) >> 16);
		avg = ((cr + cg + cb) / 3);
		// Outline of the car is white, when the car is dark
		// ("average" color is under 128), and black if the car is
		// bright.
		outline = (avg < 128 ? Color.WHITE : Color.BLACK);
	}

	/**
	 * Draws a car.
	 * 
	 * @param g
	 *            a {@link Graphics2D} object.
	 */
	protected void drawCar(Graphics2D g) {
		g.translate(-32, -64);

		// Shape of the car
		g.setStroke(new BasicStroke(3));
		g.setColor(outline);
		g.draw(c);

		g.setColor(new Color(cr, cg, cb));
		g.fill(c);

		// Windows
		g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		g.setColor(outline);
		g.draw(fwin);
		g.draw(bwin);

		// Others
		g.draw(headlights);

		if (airInlet != null) {
			g.draw(airInlet);
		}

		g.translate(32, 64);
	}

	/**
	 * Creates a trapezoid.
	 * 
	 * @param x
	 *            position of the trapezoid on the X axis
	 * @param y
	 *            position of the trapezoid on the Y axis
	 * @param a
	 *            the top basis
	 * @param b
	 *            the bottom basis
	 * @param h
	 *            the height of the trapezoid
	 * @return The newly created polygon - trapezoid.
	 */
	protected GeneralPath createTrapezoid(float x, float y, float a, float b,
			float h, boolean close) {
		float xPts[] = { x + (b - a) / 2, x, x + b, x + (b - a) / 2 + a };
		float yPts[] = { y, y + h, y + h, y };
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
				xPts.length);
		polygon.moveTo(xPts[0], yPts[0]);

		for (int i = 1; i < xPts.length; i++) {
			polygon.lineTo(xPts[i], yPts[i]);
		}

		if (close) {
			polygon.closePath();
		}

		return polygon;
	}

	/**
	 * Randomizes a pixel from the range A2-A1, R2-R1, etc. If "*2" parameter is
	 * 0, the number isn't random, e.g. if A1 = 255, and A2 = 0, returned A
	 * value is always 255.
	 * <p>
	 * <p>
	 * Example:
	 * <p>
	 * <code>rndPxl(255, 0, 0, 30, 0, 50, 230, 25)</code>
	 * <p>
	 * Gives a pixel with following RGB values: A = 255; 0 < R < 30; 0 < G < 50;
	 * 230 < B < 255 (230+25)
	 * 
	 * @param A1
	 *            the starting value of the Alpha
	 * @param A2
	 *            the random range of the Alpha
	 * @param R1
	 *            the starting value of the Red
	 * @param R2
	 *            the random range of the Red
	 * @param G1
	 *            the starting value of the Green
	 * @param G2
	 *            the random range of the Green
	 * @param B1
	 *            the starting value of the Blue
	 * @param B2
	 *            the random range of the Blue
	 * @return Object of the {@link Color} class.
	 * @see Color
	 * @see Random
	 */
	protected Color rndPxl(int A1, int A2, int R1, int R2, int G1, int G2,
			int B1, int B2) {
		return new Color((R2 == R1 ? 0 : R1 + random.nextInt(R2)),
				(G2 == 0 ? G1 : G1 + random.nextInt(G2)), (B2 == 0 ? B1 : B1
						+ random.nextInt(B2)), (A2 == 0 ? A1 : A1
						+ random.nextInt(A2)));
	}

	/**
	 * Returns a {@link BufferedImage} that is compatible with the current
	 * display settings.
	 * 
	 * @param width
	 *            the width of the image
	 * @param height
	 *            the height of the image
	 * @param translucency
	 *            the translucency of the image. It should be an integer from
	 *            the {@link java.awt.Transparency} class.
	 * @return The newly created, compatible BufferedImage.
	 * @see GraphicsConfiguration#createCompatibleImage(int, int, int)
	 */
	protected BufferedImage getCmptblImg(int width, int height, int translucency) {
		return GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration()
				.createCompatibleImage(width, height, translucency);
	}
}
