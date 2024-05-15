package view;

import util.Observer;
import controller.EmulatorController;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame implements Observer {

    private static final short CELL_SIZE = 10;
    private short[] gridData;
    private ControlPanel controlPanel;

    private EmulatorController controller;
    public GUI(EmulatorController controller) {
        gridData = new short[64 * 32];
        this.controller = controller;
        this.gridData = this.controller.getDisplay();

        setTitle("Grid GUI");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGrid(g);
            }
        };

        add(gridPanel);

        this.controlPanel = new ControlPanel(controller);
        add(this.controlPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void drawGrid(Graphics g) {
        final int numRows = 32;
        final int numCols = 64;

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                int x = col * CELL_SIZE;
                int y = row * CELL_SIZE;

                int index = row * numCols + col;

                if (gridData[index] == 0) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.BLACK);
                }

                g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    @Override
    public void update() {
        this.gridData = this.controller.getDisplay();
        this.controlPanel.setInstruction(this.controller.getNextInstruction());
        repaint();
    }
}
