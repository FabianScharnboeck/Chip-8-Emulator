package view;

import controller.EmulatorController;

import javax.swing.*;

public class ControlPanel extends JPanel {
    private JButton emulateCycle;
    private JButton emulateNCycles;
    private JTextField numberOfCycles;
    private JButton undo;
    private JTextField lastInstruction;
    private EmulatorController controller;

    public ControlPanel(EmulatorController controller) {
        this.controller = controller;

        lastInstruction = new JTextField(20);
        emulateCycle = new JButton("Emulate Cycle");
        emulateNCycles = new JButton("Emulate n Cycles");
        numberOfCycles = new JTextField(5);
        undo = new JButton("Undo");


        emulateCycle.addActionListener(e -> this.controller.executeCycle());
        emulateNCycles.addActionListener(e -> {
            for(int i = 0; i<Integer.parseInt(numberOfCycles.getText()); i++){
                this.controller.executeCycle();
            }
            // this.controller.executeCycles(Integer.parseInt(numberOfCycles.getText()));
        });
        undo.addActionListener(e -> this.controller.undo());
        lastInstruction.setText(this.controller.getNextInstruction());

        add(lastInstruction);
        add(emulateCycle);
        add(emulateNCycles);
        add(numberOfCycles);
        add(undo);

        setVisible(true);
    }


    public void setInstruction(final String instr) {
        this.lastInstruction.setText("Next Instruction: " + instr);
    }
}
