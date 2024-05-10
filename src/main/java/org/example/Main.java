package org.example;

import Emulator.Chip_8_Emulator;

public class Main {
    public static void main(String[] args) {
        Chip_8_Emulator cpu = new Chip_8_Emulator();
        int[] mem = new int[4096];
        mem[0x200] = 0x801E;
        cpu.setMemory(mem);
        cpu.executeCycle();
        System.out.println(cpu);
    }
}