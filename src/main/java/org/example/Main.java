package org.example;

import emulator.Chip8Emulator;
import controller.EmulatorController;
import view.GUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Read ch8 file
        //File file = new File("src/main/resources/1-chip8-logo.ch8");
        //File file = new File("src/main/resources/2-ibm-logo.ch8");
        File file = new File("src/main/resources/3-corax+.ch8");
        int[] rom = readRom(file);
        int[] memory = new int[4096];

        for(int i = 0; i<rom.length; i++) {
            memory[0x200 + i] = rom[i];
        }

        Chip8Emulator cpu = new Chip8Emulator();
        cpu.setMemory(memory);

        // View constructed;
        EmulatorController controller = new EmulatorController(cpu);
        GUI gui = new GUI(controller);
        cpu.addObserver(gui);
    }


    private static int[] readRom(File file) {
        byte[] rom = new byte[4096-0x200];
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(rom);
            fis.close();


            int[] romInt = new int[rom.length];
            for(int i = 0; i<rom.length; i++) {
                romInt[i] = rom[i] & 0xFF;
            }

            return romInt;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}