package Emulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;

class Chip8EmulatorTest {

    private Chip_8_Emulator cpu;

    @BeforeEach
    void setUp() {
        cpu = new Chip_8_Emulator();
    }

    @Test
    void testConstructor() {
        // Get private field
        try {
            // Memory
            Field memField = cpu.getClass().getDeclaredField("memory");
            memField.setAccessible(true);
            int[] memory = (int[]) memField.get(cpu);
            assertThat(memory).hasLength(0x1000);

            // Stack
            Field stackField = cpu.getClass().getDeclaredField("stack");
            stackField.setAccessible(true);
            int[] stack = (int[]) stackField.get(cpu);
            assertThat(stack).hasLength(16);

            // Keyboard
            Field keyboardField = cpu.getClass().getDeclaredField("keyboard");
            keyboardField.setAccessible(true);
            byte[] keyboard = (byte[]) keyboardField.get(cpu);
            assertThat(keyboard).hasLength(16);
            assertThat(keyboard[0]).isEqualTo((byte) 1);
            assertThat(keyboard[1]).isEqualTo((byte) 2);
            assertThat(keyboard[2]).isEqualTo((byte) 3);
            assertThat(keyboard[3]).isEqualTo((byte) 0xC);
            assertThat(keyboard[4]).isEqualTo((byte) 4);
            assertThat(keyboard[5]).isEqualTo((byte) 5);
            assertThat(keyboard[6]).isEqualTo((byte) 6);
            assertThat(keyboard[7]).isEqualTo((byte) 0xD);
            assertThat(keyboard[8]).isEqualTo((byte) 7);
            assertThat(keyboard[9]).isEqualTo((byte) 8);
            assertThat(keyboard[10]).isEqualTo((byte) 9);
            assertThat(keyboard[11]).isEqualTo((byte) 0xE);
            assertThat(keyboard[12]).isEqualTo((byte) 0xA);
            assertThat(keyboard[13]).isEqualTo((byte) 0);
            assertThat(keyboard[14]).isEqualTo((byte) 0xB);
            assertThat(keyboard[15]).isEqualTo((byte) 0xF);

            // TODO DISPLAY

            // PC
            Field pcField = cpu.getClass().getDeclaredField("PC");
            pcField.setAccessible(true);
            int pc = (int) pcField.get(cpu);
            assertThat(pc).isEqualTo((short) 0x200);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testJPAddrFFF() {
        int[] memory = new int[4096];
        memory[0x200] = 0x1FFF;
        cpu.setMemory(memory);
        cpu.executeCycle();
        try {
            Field pcField = cpu.getClass().getDeclaredField("PC");
            pcField.setAccessible(true);
            int pc = (int) pcField.get(cpu);
            assertThat(pc).isEqualTo(0xFFF);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testJPAddrA12() {
        int[] memory = new int[4096];
        memory[0x200] = 0x1A12;
        cpu.setMemory(memory);
        cpu.executeCycle();
        try {
            Field pcField = cpu.getClass().getDeclaredField("PC");
            pcField.setAccessible(true);
            int pc = (int) pcField.get(cpu);
            assertThat(pc).isEqualTo(0xA12);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testJPAddr111() {
        int[] memory = new int[4096];
        memory[0x200] = 0x1111;
        cpu.setMemory(memory);
        cpu.executeCycle();
        try {
            Field pcField = cpu.getClass().getDeclaredField("PC");
            pcField.setAccessible(true);
            int pc =  (int) pcField.get(cpu);
            assertThat(pc).isEqualTo(0x111);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}