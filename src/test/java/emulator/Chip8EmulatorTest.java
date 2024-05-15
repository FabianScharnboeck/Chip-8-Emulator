package emulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;

class Chip8EmulatorTest {

    private Chip8Emulator cpu;

    @BeforeEach
    void setUp() {
        cpu = new Chip8Emulator();
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
    void testClearDisplay() {
        int[] memory = new int[4096];
        memory[0x200] = 0x00;
        memory[0x201] = 0xE0;
        cpu.setMemory(memory);

        try {
            Field displayField = cpu.getClass().getDeclaredField("display");
            displayField.setAccessible(true);
            short[] display = (short[]) displayField.get(cpu);
            for(int i = 0; i < display.length; i++) {
                display[i] = 1;
            }

            cpu.executeCycle();

            for (short b : display) {
                assertThat(b).isEqualTo(0);
            }
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test6xkk() {
        int[] memory = new int[4096];
        memory[0x200] = 0x61;
        memory[0x201] = 0x0A;
        cpu.setMemory(memory);

        cpu.executeCycle();

        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            assertThat(V[1]).isEqualTo(0x0A);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test6F23() {
        int[] memory = new int[4096];
        memory[0x200] = 0x6F;
        memory[0x201] = 0x23;
        cpu.setMemory(memory);

        cpu.executeCycle();

        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            assertThat(V[0xF]).isEqualTo(0x23);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test6A30() {
        int[] memory = new int[4096];
        memory[0x200] = 0x6A;
        memory[0x201] = 0x30;
        cpu.setMemory(memory);

        cpu.executeCycle();

        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            assertThat(V[0xA]).isEqualTo(0x30);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testA143() {
        int[] memory = new int[4096];
        memory[0x200] = 0xA1;
        memory[0x201] = 0x43;
        cpu.setMemory(memory);

        cpu.executeCycle();

        try {
            Field IField = cpu.getClass().getDeclaredField("I");
            IField.setAccessible(true);
            short I = (short) IField.get(cpu);
            assertThat(I).isEqualTo(0x143);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testAFFF() {
        int[] memory = new int[4096];
        memory[0x200] = 0xAF;
        memory[0x201] = 0xFF;
        cpu.setMemory(memory);

        cpu.executeCycle();

        try {
            Field IField = cpu.getClass().getDeclaredField("I");
            IField.setAccessible(true);
            short I = (short) IField.get(cpu);
            assertThat(I).isEqualTo(0xFFF);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testA000() {
        int[] memory = new int[4096];
        memory[0x200] = 0xA0;
        memory[0x201] = 0x00;
        cpu.setMemory(memory);

        cpu.executeCycle();

        try {
            Field IField = cpu.getClass().getDeclaredField("I");
            IField.setAccessible(true);
            short I = (short) IField.get(cpu);
            assertThat(I).isEqualTo(0x000);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testD124() {
        int[] memory = new int[4096];
        memory[0x200] = 0xD1;
        memory[0x201] = 0x24;

        setRegister(1, (short) 0x08);
        setRegister(2, (short) 0x03);



        memory[0x143] = 0b11110000;
        memory[0x144] = 0b01100000;
        memory[0x145] = 0b11110000;
        memory[0x146] = 0b01100000;

        cpu.setMemory(memory);

        try {
            Field IField = cpu.getClass().getDeclaredField("I");
            IField.setAccessible(true);
            short I = (short) IField.get(cpu);
            I = 0x143;
            IField.set(cpu, I);

            cpu.executeCycle();

            assertPCIs(0x202);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }


        cpu.setMemory(memory);
    }

    @Test
    void testD124Edge() {
        int[] memory = new int[4096];
        memory[0x200] = 0xD1;
        memory[0x201] = 0x24;

        setRegister(1, (short) 61);
        setRegister(2, (short) 30);



        memory[0x143] = 0b11110000;
        memory[0x144] = 0b01100000;
        memory[0x145] = 0b11110000;
        memory[0x146] = 0b01100000;

        cpu.setMemory(memory);

        try {
            Field IField = cpu.getClass().getDeclaredField("I");
            IField.setAccessible(true);
            short I = (short) IField.get(cpu);
            I = 0x143;
            IField.set(cpu, I);

            cpu.executeCycle();

            System.out.println(cpu);

            Field displayField = cpu.getClass().getDeclaredField("display");
            displayField.setAccessible(true);
            short[] display = (short[]) displayField.get(cpu);

            assertThat(display[61 + 30 * 64]).isEqualTo((short) 1);
            assertThat(display[62 + 30 * 64]).isEqualTo((short) 1);
            assertThat(display[63 + 30 * 64]).isEqualTo((short) 1);

            assertThat(display[61 + 31 * 64]).isEqualTo((short) 0);
            assertThat(display[62 + 31 * 64]).isEqualTo((short) 1);
            assertThat(display[63 + 31 * 64]).isEqualTo((short) 1);

            assertPCIs(0x202);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }


        cpu.setMemory(memory);
    }


    @ParameterizedTest
    @ValueSource(shorts = {0x200, 0x204, 0x208, 0x20C, 0xFFF, 0xFF8, 0xABC})
    void testJP_Add(final int value) {
        int val = 0x1000 | value;
        int[] memory = new int[4096];

        int left = val >> 8;
        int right = val & 0x00FF;
        memory[0x200] = left;
        memory[0x201] = right;

        cpu.setMemory(memory);



        assertPCIs(0x200);
        cpu.executeCycle();
        assertPCIs(value);
    }

    @Test
    void test7xNN() {
        int[] memory = new int[4096];
        memory[0x200] = 0x70;
        memory[0x201] = 0x0A;
        cpu.setMemory(memory);

        setRegister(0, (short) 0x05);

        cpu.executeCycle();

        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            assertThat(V[0]).isEqualTo(0x0F);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test7345() {
        int[] memory = new int[4096];
        memory[0x200] = 0x73;
        memory[0x201] = 0x43;
        cpu.setMemory(memory);

        setRegister(3, (short) 0x1);

        cpu.executeCycle();

        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            assertThat(V[3]).isEqualTo(0x44);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void test2nnn() {
        int[] memory = new int[4096];
        memory[0x200] = 0x23;
        memory[0x201] = 0x45;
        memory[0x345] = 0x00;
        memory[0x346] = 0xEE;
        cpu.setMemory(memory);

        try {


            Field stackPointerField = cpu.getClass().getDeclaredField("SP");
            stackPointerField.setAccessible(true);
            byte stackPointer = (byte) stackPointerField.get(cpu);
            assertThat(stackPointer).isEqualTo(0);
            assertPCIs(0x200);

            cpu.executeCycle();

            stackPointer = (byte) stackPointerField.get(cpu);
            assertPCIs(0x345);
            assertThat(stackPointer).isEqualTo(1);

            cpu.executeCycle();

            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test8011() {
        int[] memory = new int[4096];
        memory[0x200] = 0x80;
        memory[0x201] = 0x11;
        cpu.setMemory(memory);

        setRegister(0, (short) 0x0F);
        setRegister(1, (short) 0x0A);

        cpu.executeCycle();

        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            assertThat(V[0]).isEqualTo(0x0F | 0x0A);
            assertThat(V[1]).isEqualTo(0x0A);
            assertPCIs(0x202);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    void setRegister(final int index, final short value) {
        try {
            Field register = cpu.getClass().getDeclaredField("register");
            register.setAccessible(true);
            short[] V = (short[]) register.get(cpu);
            V[index] = value;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    void assertPCIs(final int expected) {
        try {
            Field pcField = cpu.getClass().getDeclaredField("PC");
            pcField.setAccessible(true);
            int pc = (int) pcField.get(cpu);
            assertThat(pc).isEqualTo(expected);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}