package com.mojang.escape.input;

import java.awt.event.*;

public class InputHandler implements KeyListener, FocusListener, MouseListener, MouseMotionListener {
	public boolean[] keys = new boolean[65536];

	/** V2 seam: set when escape.look.mode=mouseLerp; null keeps 2011 no-op behaviour. */
	public MouseMotionSink mouseMotionSink;

	public interface MouseMotionSink {
		void mouseMovedTo(int x, int y);
	}

	public void mouseDragged(MouseEvent e) {
		if (mouseMotionSink != null) mouseMotionSink.mouseMovedTo(e.getX(), e.getY());
	}

	public void mouseMoved(MouseEvent e) {
		if (mouseMotionSink != null) mouseMotionSink.mouseMovedTo(e.getX(), e.getY());
	}

	public void mouseClicked(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void focusGained(FocusEvent arg0) {
	}

	public void focusLost(FocusEvent arg0) {
		for (int i=0; i<keys.length; i++) {
			keys[i] = false;
		}
	}

	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode(); 
		if (code>0 && code<keys.length) {
			keys[code] = true;
		}
	}

	public void keyReleased(KeyEvent e) {
		int code = e.getKeyCode(); 
		if (code>0 && code<keys.length) {
			keys[code] = false;
		}
	}

	public void keyTyped(KeyEvent arg0) {
	}
}