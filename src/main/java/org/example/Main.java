package org.example;

import Emulator.Chip8Emulator;

public class Main {
    public static void main(String[] args) {
        Chip8Emulator cpu = new Chip8Emulator();
        int[] mem = new int[4096];
        mem[0x200] = 0xDABE;
        cpu.setMemory(mem);
        cpu.executeCycle();
        System.out.println(cpu);
    }
}