package controller;

import emulator.*;

import java.util.Stack;

public class EmulatorController {

    Chip8Emulator emulator;
    Stack<Chip8Emulator> lastEmulators = new Stack<>();
    public EmulatorController(Chip8Emulator emulator) {
        this.emulator = emulator;
    }


    public void executeCycle() {
        this.lastEmulators.push(this.emulator.copy());
        this.emulator.executeCycle();
    }

    public void executeCycles(final int n) {
        for(int i = 0; i<n; i++) {
            this.executeCycle();
        }
    }

    public short[] getDisplay() {
        return this.emulator.getDisplay();
    }

    public void undo() {
        if(!this.lastEmulators.isEmpty()) {
            this.emulator = this.lastEmulators.pop();
            this.emulator.notifyObservers();
        }
    }

    public String getNextInstruction() {
        return this.emulator.getPCInstr();
    }
}
