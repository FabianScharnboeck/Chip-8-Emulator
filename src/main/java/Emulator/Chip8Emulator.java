package Emulator;

import java.util.Arrays;

/**
 * <a href="http://devernay.free.fr/hacks/chip8/C8TECH10.HTM#memmap">Link to Documentation</a>
 */
public class Chip8Emulator implements Emulator {

    // The Chip-8 language is capable of accessing up to 4KB (4,096 bytes) of RAM, from location 0x000 (0)
    // to 0xFFF (4095). The first 512 bytes, from 0x000 to 0x1FF,
    // are where the original interpreter was located, and should not be used by programs.
    //
    // Most Chip-8 programs start at location 0x200 (512), but some begin at 0x600 (1536).
    // Programs beginning at 0x600 are intended for the ETI 660 computer.
    private static final short FOUR_KB = 0x1000;
    private static final short START_LOCATION = 0x200;
    private static final short START_LOCATION_ETI = 0x600;
    private static final short KEYBOARD_SIZE = 0x10;

    private static final short DISPLAY_WIDTH = 64;
    private static final short DISPLAY_HEIGHT = 32;
    private static final short DISPLAY_SIZE = DISPLAY_WIDTH * DISPLAY_HEIGHT;
    private static final short START_FONT_SET_LOCATION = 0x000;

    //Chip-8 has 16 general purpose 8-bit registers, usually referred to as Vx, where x is a hexadecimal digit (0 through F).
    // There is also a 16-bit register called I. This register is generally used to store memory addresses,
    // so only the lowest (rightmost) 12 bits are usually used.
    private short V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, VA, VB, VC, VD, VE, VF;
    private short I;

    //There are also some "pseudo-registers" which are not accessable from Chip-8 programs.
    // The program counter (PC) should be 16-bit, and is used to store the currently executing address.
    // The stack pointer (SP) can be 8-bit, it is used to point to the topmost level of the stack.
    private int PC;

    private byte SP;

    //The stack is an array of 16 16-bit values,
    // used to store the address that the interpreter should return to when finished with a subroutine. Chip-8 allows
    // for up to 16 levels of nested subroutines.
    private int[] stack;

    private int[] memory;
    private byte[] keyboard;

    private short[] display;

    private static final short[] font_set = {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    // Timers & Sounds
    private int delayTimer;
    private int soundTimer;


    // Instructions. 16 bit instructions.
    private static final short JP_ADDR = 0x1;
    private static final short CALL_ADDR = 0x2;
    private static final short ZERO_INSTRUCTION = 0x0;
    private static final short CLEAR_DISPLAY = 0x0E0;
    private static final short RETURN = 0x0EE;
    private static final short SKIP_IF_VX_EQUALS_NN = 0x3;
    private static final short SKIP_IF_VX_NOT_EQUALS_NN = 0x4;
    private static final short SKIP_NEXT_INSTRUCTION_IF_VX_EQUALS_VY = 0x5;
    private static final short SET_VX_TO_VALUE = 0x6;
    private static final short ADD_VALUE_TO_VS = 0x7;
    private static final short STORE_TO_VX_FROM_VY_SET_OR_AND_XOR = 0x8;
    private static final short SKIP_NEXT_INSTRUCTION = 0x9;
    private static final short SET_I_TO_ADDR = 0xA;
    private static final short JUMP_TO_ADDR_PLUS_V0 = 0xB;
    private static final short RANDOM_BYTE_AND_KK = 0xC;
    private static final short DRAW_SPRITE = 0xD;
    private static final short SKIP_IF_KEY_PRESSED = 0xE;
    private static final short ALL_F_INSTRUCTIONS = 0xF;

    public Chip8Emulator() {
        this.stack = new int[0x10];
        this.memory = new int[FOUR_KB];
        this.display = new short[DISPLAY_SIZE];
        this.keyboard = new byte[KEYBOARD_SIZE];
        fillKeyboard();
        loadFontsIntoMemory();


        this.PC = START_LOCATION;
    }

    private void loadFontsIntoMemory() {
        for (int i = START_FONT_SET_LOCATION; i < font_set.length; i++) {
            memory[i] = font_set[i];
        }
    }

    private void fillKeyboard() {
        this.keyboard[0] = 1;
        this.keyboard[1] = 2;
        this.keyboard[2] = 3;
        this.keyboard[3] = 0xC;
        this.keyboard[4] = 4;
        this.keyboard[5] = 5;
        this.keyboard[6] = 6;
        this.keyboard[7] = 0xD;
        this.keyboard[8] = 7;
        this.keyboard[9] = 8;
        this.keyboard[10] = 9;
        this.keyboard[11] = 0xE;
        this.keyboard[12] = 0xA;
        this.keyboard[13] = 0;
        this.keyboard[14] = 0xB;
        this.keyboard[15] = 0xF;
    }

    @Override
    public void executeCycle() {
        int instruction = memory[PC];
        byte opcode = (byte) (instruction >> 12);
        short instructionBits = (short) (instruction & 0x0FFF);

        //Register may be used
        short register = 0x0;
        short value = 0x0;

        switch (opcode) {
            case JP_ADDR:
                PC = instructionBits;
                break;
            case CALL_ADDR:
                stack[SP] = PC;
                SP++;
                PC = instructionBits;
                break;
            case ZERO_INSTRUCTION:
                if (instruction == CLEAR_DISPLAY) {
                    throw new UnsupportedOperationException("DISPLAY NOT IMPLEMENTED");
                    //resetDisplay();
                } else if (instruction == RETURN) {
                    PC = stack[SP];
                    SP--;
                } else {
                    /**
                     * 0nnn - SYS addr
                     * Jump to a machine code routine at nnn.
                     *
                     * This instruction is only used on the old computers on which Chip-8 was originally implemented.
                     * It is ignored by modern interpreters.
                     */
                    throw new UnsupportedOperationException("No supported instruction: " + instruction);
                }
                break;
            case ADD_VALUE_TO_VS:
                register = (short) (instructionBits >> 8);
                value = (short) (instructionBits & 0x0FF);
                addToRegister(register, value);
                break;
            case SET_VX_TO_VALUE:
                register = (short) (instructionBits >> 8);
                value = (short) (instructionBits & 0x0FF);
                setRegister(register, value);
                break;
            case STORE_TO_VX_FROM_VY_SET_OR_AND_XOR:
                register = (short) (instructionBits >> 8);
                short register2 = (short) ((instructionBits >> 4) & 0x0F);
                short mode = (short) (instructionBits & 0x0F); // TODO last 4 bits different operation

                // All 0x8XYZ modes, where 0 <= Z <= 0x3, X and Y are registers
                final short SET = 0x0;
                final short OR = 0x1;
                final short AND = 0x2;
                final short XOR = 0x3;
                final short ADD = 0x4;
                final short SUB = 0x5;
                final short SHR = 0x6;
                final short SUBN = 0x7;
                final short SHL = 0xE;

                if (mode == SET) {
                    setRegisterYtoX(register, register2);
                } else if (mode == OR) {
                    registerOR(register, register2);
                } else if (mode == AND) {
                    registerAND(register, register2);
                } else if (mode == XOR) {
                    registerOR(register, register2);
                } else if (mode == SUB) {
                    registerSUBWithCarry(register, register2);
                } else if (mode == ADD) {
                    registerADDWithCarry(register, register2);
                } else if (mode == SHR) {
                    registerSHR(register);
                } else if (mode == SUBN) {
                    throw new UnsupportedOperationException("SUBN NOT IMPLEMENTED");
                    //registerSUBNWithCarry(register, register2);
                } else if (mode == SHL) {
                    registerSHL(register);
                } else {
                    throw new UnsupportedOperationException("No supported instruction: " + instruction);
                }
                break;
            case SKIP_IF_VX_EQUALS_NN:
                register = (short) (instructionBits >> 8);
                value = (short) (instructionBits & 0x0FF);
                if (getValueOfRegister(register) == value) {
                    incrementProgramCounter();
                }
                break;
            case SKIP_IF_VX_NOT_EQUALS_NN:
                register = (short) (instructionBits >> 8);
                value = (short) (instructionBits & 0x0FF);
                if (getValueOfRegister(register) != value) {
                    incrementProgramCounter();
                }
                break;
            case SKIP_NEXT_INSTRUCTION_IF_VX_EQUALS_VY:
                register = (short) (instructionBits >> 8);
                register2 = (short) ((instructionBits >> 4) & 0x0F);
                if (getValueOfRegister(register) == getValueOfRegister(register2)) {
                    incrementProgramCounter();
                }
                break;


            default:
                throw new UnsupportedOperationException("No supported instruction: " + instruction);
        }

    }

    private void registerSHR(short register) {
        final short LSB = 0x1;
        short value = getValueOfRegister(register);
        if ((value & LSB) == 1) {
            VF = 1;
        } else {
            VF = 0;
        }


        // 1000 >> 1 = 0100 => 8 >> 1 = 4
        setRegister(register, (short) (value >> 1));
    }

    private void registerSUBNWithCarry(short register, short register2) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void registerSHL(short register) {
        final short MSB = 0x80;
        short value = getValueOfRegister(register);

        short t = (short) (value & MSB);
        if ((value & MSB) == MSB) {
            VF = 1;
        } else {
            VF = 0;
        }

        // 0000 1000 << 1 = 0001 0000 => 8 << 1 = 16
        setRegister(register, (short) (value << 2));
    }

    private void registerADDWithCarry(short register1, short register2) {
        assert register1 >= 0 && register1 <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        short value = (short) (getValueOfRegister(register1) + getValueOfRegister(register2));

        // Set carry flag
        if (value > 0xFF) {
            VF = 1;
            value = (short) (value & 0xFF);
        } else {
            VF = 0;
        }
        setRegister(register1, value);
    }

    private void registerSUBWithCarry(short register1, short register2) {
        assert register1 >= 0 && register1 <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        short value = (short) (getValueOfRegister(register1) - getValueOfRegister(register2));

        // Set carry flag
        if (value < 0) {
            VF = 0;
            value = (short) (value & 0xFF);
        } else {
            VF = 1;
        }
        setRegister(register1, value);
    }



    private void incrementProgramCounter() {
        PC += 2;
    }

    private void registerOR(short register, short register2) {
        assert register >= 0 && register <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        switch (register) {
            case 0 -> V0 = (short) (V0 | getValueOfRegister(register2));
            case 1 -> V1 = (short) (V1 | getValueOfRegister(register2));
            case 2 -> V2 = (short) (V2 | getValueOfRegister(register2));
            case 3 -> V3 = (short) (V3 | getValueOfRegister(register2));
            case 4 -> V4 = (short) (V4 | getValueOfRegister(register2));
            case 5 -> V5 = (short) (V5 | getValueOfRegister(register2));
            case 6 -> V6 = (short) (V6 | getValueOfRegister(register2));
            case 7 -> V7 = (short) (V7 | getValueOfRegister(register2));
            case 8 -> V8 = (short) (V8 | getValueOfRegister(register2));
            case 9 -> V9 = (short) (V9 | getValueOfRegister(register2));
            case 0xA -> VA = (short) (VA | getValueOfRegister(register2));
            case 0xB -> VB = (short) (VB | getValueOfRegister(register2));
            case 0xC -> VC = (short) (VC | getValueOfRegister(register2));
            case 0xD -> VD = (short) (VD | getValueOfRegister(register2));
            case 0xE -> VE = (short) (VE | getValueOfRegister(register2));
            case 0xF -> VF = (short) (VF | getValueOfRegister(register2));
        }
    }

    private void registerAND(short register, short register2) {
        assert register >= 0 && register <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        switch (register) {
            case 0 -> V0 = (short) (V0 & getValueOfRegister(register2));
            case 1 -> V1 = (short) (V1 & getValueOfRegister(register2));
            case 2 -> V2 = (short) (V2 & getValueOfRegister(register2));
            case 3 -> V3 = (short) (V3 & getValueOfRegister(register2));
            case 4 -> V4 = (short) (V4 & getValueOfRegister(register2));
            case 5 -> V5 = (short) (V5 & getValueOfRegister(register2));
            case 6 -> V6 = (short) (V6 & getValueOfRegister(register2));
            case 7 -> V7 = (short) (V7 & getValueOfRegister(register2));
            case 8 -> V8 = (short) (V8 & getValueOfRegister(register2));
            case 9 -> V9 = (short) (V9 & getValueOfRegister(register2));
            case 0xA -> VA = (short) (VA & getValueOfRegister(register2));
            case 0xB -> VB = (short) (VB & getValueOfRegister(register2));
            case 0xC -> VC = (short) (VC & getValueOfRegister(register2));
            case 0xD -> VD = (short) (VD & getValueOfRegister(register2));
            case 0xE -> VE = (short) (VE & getValueOfRegister(register2));
            case 0xF -> VF = (short) (VF & getValueOfRegister(register2));
            default -> throw new UnsupportedOperationException("No supported register: " + register);
        }

    }

    private void registerXOR(short register, short register2) {
        assert register >= 0 && register <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        switch (register) {
            case 0 -> V0 = (short) (V0 ^ getValueOfRegister(register2));
            case 1 -> V1 = (short) (V1 ^ getValueOfRegister(register2));
            case 2 -> V2 = (short) (V2 ^ getValueOfRegister(register2));
            case 3 -> V3 = (short) (V3 ^ getValueOfRegister(register2));
            case 4 -> V4 = (short) (V4 ^ getValueOfRegister(register2));
            case 5 -> V5 = (short) (V5 ^ getValueOfRegister(register2));
            case 6 -> V6 = (short) (V6 ^ getValueOfRegister(register2));
            case 7 -> V7 = (short) (V7 ^ getValueOfRegister(register2));
            case 8 -> V8 = (short) (V8 ^ getValueOfRegister(register2));
            case 9 -> V9 = (short) (V9 ^ getValueOfRegister(register2));
            case 0xA -> VA = (short) (VA ^ getValueOfRegister(register2));
            case 0xB -> VB = (short) (VB ^ getValueOfRegister(register2));
            case 0xC -> VC = (short) (VC ^ getValueOfRegister(register2));
            case 0xD -> VD = (short) (VD ^ getValueOfRegister(register2));
            case 0xE -> VE = (short) (VE ^ getValueOfRegister(register2));
            case 0xF -> VF = (short) (VF ^ getValueOfRegister(register2));
            default -> throw new UnsupportedOperationException("No supported register: " + register);
        }
    }

    private void addToRegister(short register, short value) {
        assert register >= 0 && register <= 0xF;
        assert value < 256;
        switch (register) {
            case 0 -> V0 = (short) (V0 + value);
            case 1 -> V1 = (short) (V1 + value);
            case 2 -> V2 = (short) (V2 + value);
            case 3 -> V3 = (short) (V3 + value);
            case 4 -> V4 = (short) (V4 + value);
            case 5 -> V5 = (short) (V5 + value);
            case 6 -> V6 = (short) (V6 + value);
            case 7 -> V7 = (short) (V7 + value);
            case 8 -> V8 = (short) (V8 + value);
            case 9 -> V9 = (short) (V9 + value);
            case 0xA -> VA = (short) (VA + value);
            case 0xB -> VB = (short) (VB + value);
            case 0xC -> VC = (short) (VC + value);
            case 0xD -> VD = (short) (VD + value);
            case 0xE -> VE = (short) (VE + value);
            case 0xF -> VF = (short) (VF + value);
            default ->
                    throw new UnsupportedOperationException("Invalid register addition:" + register + " with value " + value);
        }
    }

    private void setRegister(short register, short value) {
        assert register >= 0 && register <= 0xF;
        assert value < 256;
        switch (register) {
            case 0 -> V0 = value;
            case 1 -> V1 = value;
            case 2 -> V2 = value;
            case 3 -> V3 = value;
            case 4 -> V4 = value;
            case 5 -> V5 = value;
            case 6 -> V6 = value;
            case 7 -> V7 = value;
            case 8 -> V8 = value;
            case 9 -> V9 = value;
            case 0xA -> VA = value;
            case 0xB -> VB = value;
            case 0xC -> VC = value;
            case 0xD -> VD = value;
            case 0xE -> VE = value;
            case 0xF -> VF = value;
            default ->
                    throw new UnsupportedOperationException("Invalid register set:" + register + " with value " + value);
        }
    }

    private void setRegisterYtoX(short x, short y) {
        short valueOfY = getValueOfRegister(y);
        setRegister(x, valueOfY);
    }

    private short getValueOfRegister(short register) {
        assert register >= 0 && register <= 0xF;
        switch (register) {
            case 0 -> {
                return V0;
            }
            case 1 -> {
                return V1;
            }
            case 2 -> {
                return V2;
            }
            case 3 -> {
                return V3;
            }
            case 4 -> {
                return V4;
            }
            case 5 -> {
                return V5;
            }
            case 6 -> {
                return V6;
            }
            case 7 -> {
                return V7;
            }
            case 8 -> {
                return V8;
            }
            case 9 -> {
                return V9;
            }
            case 0xA -> {
                return VA;
            }
            case 0xB -> {
                return VB;
            }
            case 0xC -> {
                return VC;
            }
            case 0xD -> {
                return VD;
            }
            case 0xE -> {
                return VE;
            }
            case 0xF -> {
                return VF;
            }
            default -> throw new UnsupportedOperationException("Invalid register set:" + register);
        }
    }


    @Override
    public void executeCycles(int n) {
        assert n > 0;
        while (n > 0) {
            executeCycle();
            n--;
        }
    }

    public void setMemory(int[] memory) {
        this.memory = memory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DISPLAY_HEIGHT; i++) {
            for (int j = 0; j < DISPLAY_WIDTH; j++) {
                boolean isOn = display[j + i * DISPLAY_WIDTH] == 1;
                if (isOn) {
                    sb.append("0");
                } else {
                    sb.append("*");
                }
            }
            sb.append(i);
            sb.append("\n");
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    private void resetDisplay() {
        Arrays.fill(display, (short) 0);
    }
}
