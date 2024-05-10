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
    private static final byte NUMBER_REGISTERS = 16;

    //Chip-8 has 16 general purpose 8-bit registers, usually referred to as Vx, where x is a hexadecimal digit (0 through F).
    // There is also a 16-bit register called I. This register is generally used to store memory addresses,
    // so only the lowest (rightmost) 12 bits are usually used.
    private short[] register;
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
        this.register = new short[NUMBER_REGISTERS];
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
                this.register[register] = value;
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
                    this.register[register] = this.register[register2];
                } else if (mode == OR) {
                    registerOR(register, register2);
                } else if (mode == AND) {
                    registerAND(register, register2);
                } else if (mode == XOR) {
                    registerXOR(register, register2);
                } else if (mode == SUB) {
                    registerSUBWithCarry(register, register2);
                } else if (mode == ADD) {
                    registerADDWithCarry(register, register2);
                } else if (mode == SHR) {
                    registerSHR(register);
                } else if (mode == SUBN) {
                    registerSUBNWithCarry(register, register2);
                } else if (mode == SHL) {
                    registerSHL(register);
                } else {
                    throw new UnsupportedOperationException("No supported instruction: " + instruction);
                }
                break;
            case SKIP_IF_VX_EQUALS_NN:
                register = (short) (instructionBits >> 8);
                value = (short) (instructionBits & 0x0FF);
                if (this.register[register] == value) {
                    incrementProgramCounter();
                }
                break;
            case SKIP_IF_VX_NOT_EQUALS_NN:
                register = (short) (instructionBits >> 8);
                value = (short) (instructionBits & 0x0FF);
                if (this.register[register] != value) {
                    incrementProgramCounter();
                }
                break;
            case SKIP_NEXT_INSTRUCTION_IF_VX_EQUALS_VY:
                register = (short) (instructionBits >> 8);
                register2 = (short) ((instructionBits >> 4) & 0x0F);
                if (this.register[register] == this.register[register2]) {
                    incrementProgramCounter();
                }
                break;
            case DRAW_SPRITE:
                short memLocX = (short) ((instructionBits >> 8) & 0xF);
                short memLocY = (short) ((instructionBits >> 4) & 0x0F);
                short xCoordinate = (short) (this.register[memLocX] % DISPLAY_WIDTH);
                short yCoordinate = (short) (this.register[memLocY] % DISPLAY_HEIGHT);
                short nibble = (short) (instructionBits & 0x00F);

                drawSprite(xCoordinate, yCoordinate, nibble);
                incrementProgramCounter();
                break;
            default:
                throw new UnsupportedOperationException("No supported instruction: " + instruction);
        }

    }

    /**
     * Dxyn - DRW Vx, Vy, nibble
     * Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
     *
     * The interpreter reads n bytes from memory, starting at the address stored in I.
     * These bytes are then displayed as sprites on screen at coordinates (Vx, Vy).
     * Sprites are XORed onto the existing screen. If this causes any pixels to be erased, VF is set to 1,
     * otherwise it is set to 0. If the sprite is positioned so part of it is outside the coordinates of the display,
     * it wraps around to the opposite side of the screen. See instruction 8xy3 for more information on XOR, and
     * section 2.4, Display, for more information on the Chip-8 screen and sprites.
     * <a href="http://devernay.free.fr/hacks/chip8/C8TECH10.HTM#2.1">See here</a>
     * <a href="https://tobiasvl.github.io/blog/write-a-chip-8-emulator/#dxyn-display">chip-8-display</a>
     */
    private void drawSprite(short xCoordinate, short yCoordinate, short height) {
        this.register[0xF] = 0;

        for (int row = 0; row < height; row++) {

            // sprite is hex-encoded. 0xF0 => 11110000 would result in drawing 4 pixels => ****
            // 0xA0 => 10100000 would result in drawing 2 pixels => * *
            short sprite = (short) this.memory[I + row];

            for (int col = 0; col < 8; col++) {

                // get the pixel at the recent col position
                short pixel = (short) ((sprite >> (7 - col)) & 0x1);

                short x = (short) (xCoordinate + col);
                short y = (short) (yCoordinate + row);

                // x goes out of bounds of the display
                if (x >= DISPLAY_WIDTH) {
                    continue;
                }

                // y goes out of bounds of the display
                if (y >= DISPLAY_HEIGHT) {
                    continue;
                }

                if (pixel == 1) {
                    if (this.display[x + y * DISPLAY_WIDTH] == 1) {
                        this.register[0xF] = 1;
                        this.display[x + y * DISPLAY_WIDTH] = 0;
                    } else {
                        this.display[x + y * DISPLAY_WIDTH] = pixel;
                    }
                }
            }
        }
    }

    /**
     * 8xy6 - SHR Vx {, Vy}
     * Set Vx = Vx SHR 1.
     * <p>
     * If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2.
     */
    private void registerSHR(short register) {
        final short LSB = 0x1;
        short value = this.register[register];
        if ((value & LSB) == 1) {
            this.register[0xF] = 1;
        } else {
            this.register[0xF] = 0;
        }


        // 1000 >> 1 = 0100 => 8 >> 1 = 4
        this.register[register] = (short) (value >> 1);
    }

    /**
     * 8xy7 - SUBN Vx, Vy
     * Set Vx = Vy - Vx, set VF = NOT borrow.
     * If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted from Vy, and the results stored in Vx.
     */
    private void registerSUBNWithCarry(short register, short register2) {
        if (this.register[register2] > this.register[register]) {
            this.register[0xF] = 1;
        } else {
            this.register[0xF] = 0;
        }
        this.register[register] = (short) (this.register[register2] - this.register[register]);
    }


    /**
     * 8xyE - SHL Vx {, Vy}
     * Set Vx = Vx SHL 1.
     * If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is multiplied by 2.
     */
    private void registerSHL(short register) {
        final short MSB = 0x80;
        short value = this.register[register];

        if ((value & MSB) == MSB) {
            this.register[0xF] = 1;
        } else {
            this.register[0xF] = 0;
        }

        // 0000 1000 << 1 = 0001 0000 => 8 << 1 = 16
        this.register[register] = (short) (value << 1);
    }

    /**
     * 8xy4 - ADD Vx, Vy
     * Set Vx = Vx + Vy, set VF = carry.
     * The values of Vx and Vy are added together. If the result is greater than 8 bits (i.e., > 255,) VF is set to 1,
     * otherwise 0. Only the lowest 8 bits of the result are kept, and stored in Vx.
     */
    private void registerADDWithCarry(short register1, short register2) {
        assert register1 >= 0 && register1 <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        short value = (short) (this.register[register1] + this.register[register2]);

        // Set carry flag
        if (value > 0xFF) {
            this.register[0xF] = 1;
            value = (short) (value & 0xFF);
        } else {
            this.register[0xF] = 0;
        }
        this.register[register1] = value;
    }

    /**
     * 8xy5 - SUB Vx, Vy
     * Set Vx = Vx - Vy, set VF = NOT borrow.
     * If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted from Vx, and the results stored in Vx.
     */
    private void registerSUBWithCarry(short register1, short register2) {
        assert register1 >= 0 && register1 <= 0xF;
        assert register2 >= 0 && register2 <= 0xF;
        short value = (short) (this.register[register1] - this.register[register2]);

        // Set carry flag
        if (value < 0) {
            this.register[0xF] = 0;
            value = (short) (value & 0xFF);
        } else {
            this.register[0xF] = 1;
        }
        this.register[register1] = value;
    }

    private void incrementProgramCounter() {
        PC += 2;
    }

    private void registerOR(short register, short register2) {
        this.register[register] = (short) (this.register[register] | this.register[register2]);
    }

    private void registerAND(short register, short register2) {
        this.register[register] = (short) (this.register[register] & this.register[register2]);
    }

    private void registerXOR(short register, short register2) {
        this.register[register] = (short) (this.register[register] ^ this.register[register2]);
    }

    private void addToRegister(short register, short value) {
        this.register[register] += value;
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
