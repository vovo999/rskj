/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.vm;

import co.rsk.config.TestSystemProperties;
import co.rsk.config.VmConfig;
import co.rsk.core.Coin;
import co.rsk.core.RskAddress;
import co.rsk.test.builders.AccountBuilder;
import co.rsk.test.builders.TransactionBuilder;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.core.Account;
import org.ethereum.core.BlockFactory;
import org.ethereum.core.Transaction;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.VM;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.Stack;
import org.ethereum.vm.program.invoke.ProgramInvokeMockImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ajlopez on 25/01/2017.
 */
public class VMExecutionTest {
    private final TestSystemProperties config = new TestSystemProperties();
    private final VmConfig vmConfig = config.getVmConfig();
    private final PrecompiledContracts precompiledContracts = new PrecompiledContracts(config);
    private final BlockFactory blockFactory = new BlockFactory(config.getActivationConfig());
    private ProgramInvokeMockImpl invoke;
    private BytecodeCompiler compiler;

    @Before
    public void setup() {
        invoke = new ProgramInvokeMockImpl();
        compiler = new BytecodeCompiler();
    }

    @Test
    public void testPush1() {
        Program program = executeCode("PUSH1 0xa0", 1);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueOf(0xa0), stack.peek());
    }

    @Test
    public void testAdd() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 ADD", 3);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueOf(3), stack.peek());
    }

    @Test
    public void testMul() {
        Program program = executeCode("PUSH1 0x03 PUSH1 0x02 MUL", 3);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueOf(6), stack.peek());
    }

    @Test
    public void testSub() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 SUB", 3);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());
    }


    private void executeShift(String number, String shiftAmount, String expect,String op ,BlockchainConfig blockchainConfig){
        Program program = executeCodeWithBlockchainConfig("PUSH32 "+number+" PUSH1 "+shiftAmount+" "+op, 3, blockchainConfig);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueFromHex(expect), stack.peek());
    }

    @Test
    public void testSHL1() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x00",
                "0000000000000000000000000000000000000000000000000000000000000001",
                "SHL",
                blockchainConfig);
    }


    @Test
    public void testSHL2() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x01",
                "0000000000000000000000000000000000000000000000000000000000000002",
                "SHL",
                blockchainConfig);
    }

    @Test
    public void testSHL3() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0xff",
                "8000000000000000000000000000000000000000000000000000000000000000",
                "SHL",
                blockchainConfig);
    }

    @Test
    public void testSHL4() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        String number = "0x0000000000000000000000000000000000000000000000000000000000000001";
        String shiftAmount = "0x0100";
        String op = "SHL";
        String expect = "0000000000000000000000000000000000000000000000000000000000000000";

        Program program = executeCodeWithBlockchainConfig("PUSH32 "+number+" PUSH2 "+shiftAmount+" "+op, 3, blockchainConfig);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueFromHex(expect), stack.peek());
    }

    @Test
    public void testSHL5() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0x00",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "SHL",
                blockchainConfig);
    }

    @Test
    public void testSHL6() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0xff",
                "8000000000000000000000000000000000000000000000000000000000000000",
                "SHL",
                blockchainConfig);
    }

    @Test
    public void testSHL7() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0x01",
                "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe",
                "SHL",
                blockchainConfig);
    }

    @Test
    public void testSHR1() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x00",
                "0000000000000000000000000000000000000000000000000000000000000001",
                "SHR",
                blockchainConfig);
    }

    @Test
    public void testSHR2() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x01",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "SHR",
                blockchainConfig);
    }

    @Test
    public void testSHR3() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x8000000000000000000000000000000000000000000000000000000000000000",
                "0x01",
                "4000000000000000000000000000000000000000000000000000000000000000",
                "SHR",
                blockchainConfig);
    }

    @Test
    public void testSHR4() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x8000000000000000000000000000000000000000000000000000000000000000",
                "0xff",
                "0000000000000000000000000000000000000000000000000000000000000001",
                "SHR",
                blockchainConfig);
    }


    @Test
    public void testSHR5() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0x00",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "SHR",
                blockchainConfig);
    }

    @Test
    public void testSHR6() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0xff",
                "0000000000000000000000000000000000000000000000000000000000000001",
                "SHR",
                blockchainConfig);
    }

    @Test
    public void testSHR7() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        String number = "0x8000000000000000000000000000000000000000000000000000000000000000";
        String shiftAmount = "0x0100";
        String op = "SHR";
        String expect = "0000000000000000000000000000000000000000000000000000000000000000";

        Program program = executeCodeWithBlockchainConfig("PUSH32 "+number+" PUSH2 "+shiftAmount+" "+op, 3, blockchainConfig);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueFromHex(expect), stack.peek());
    }

    @Test
    public void testSAR1() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x00",
                "0000000000000000000000000000000000000000000000000000000000000001",
                "SAR",
                blockchainConfig);
    }


    @Test
    public void testSAR2() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x0000000000000000000000000000000000000000000000000000000000000001",
                "0x01",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "SAR",
                blockchainConfig);
    }


    @Test
    public void testSAR3() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x8000000000000000000000000000000000000000000000000000000000000000",
                "0x01",
                "c000000000000000000000000000000000000000000000000000000000000000",
                "SAR",
                blockchainConfig);
    }


    @Test
    public void testSAR4() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x8000000000000000000000000000000000000000000000000000000000000000",
                "0xff",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "SAR",
                blockchainConfig);
    }


    @Test
    public void testSAR5() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0x00",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "SAR",
                blockchainConfig);
    }


    @Test
    public void testSAR6() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0x01",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "SAR",
                blockchainConfig);
    }


    @Test
    public void testSAR7() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        executeShift("0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0xf8",
                "000000000000000000000000000000000000000000000000000000000000007f",
                "SAR",
                blockchainConfig);
    }

    @Test
    public void testSAR8() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(true);

        String number = "0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        String shiftAmount = "0x0100";
        String op = "SAR";
        String expect = "0000000000000000000000000000000000000000000000000000000000000000";

        Program program = executeCodeWithBlockchainConfig("PUSH32 "+number+" PUSH2 "+shiftAmount+" "+op, 3, blockchainConfig);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueFromHex(expect), stack.peek());
    }



    @Test(expected = Program.IllegalOperationException.class)
    public void testSAR3ShouldFailOnOldVersion() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(false);
        executeCodeWithBlockchainConfig("PUSH32 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff PUSH1 0xff SAR", 3,blockchainConfig);
    }

    @Test(expected = Program.IllegalOperationException.class)
    public void testSHL1ShouldFailOnOldVersion() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(false);

        executeCodeWithBlockchainConfig("PUSH32 0x0000000000000000000000000000000000000000000000000000000000000001 PUSH1 0x01 SHL", 3, blockchainConfig);
    }

    @Test(expected = Program.IllegalOperationException.class)
    public void testSHR1ShouldFailOnOldVersion() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip120()).thenReturn(false);

        executeCodeWithBlockchainConfig("PUSH32 0x0000000000000000000000000000000000000000000000000000000000000001 PUSH1 0x01 SHR", 3, blockchainConfig);
    }

    @Test
    public void testJumpSkippingInvalidJump() {
        Program program = executeCode("PUSH1 0x05 JUMP PUSH1 0xa0 JUMPDEST PUSH1 0x01", 4);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());
    }

    @Test
    public void dupnFirstItem() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x00 DUPN", 3);
        Stack stack = program.getStack();

        Assert.assertEquals(2, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());
        Assert.assertEquals(DataWord.valueOf(1), stack.get(0));
    }

    @Test
    public void dupnFourthItem() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x03 PUSH1 0x04 PUSH1 0x03 DUPN", 6);
        Stack stack = program.getStack();

        Assert.assertEquals(5, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());

        for (int k = 0; k < 4; k++)
            Assert.assertEquals(DataWord.valueOf(k + 1), stack.get(k));
    }

    @Test
    public void dupnTwentiethItem() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x03 PUSH1 0x04 PUSH1 0x05 PUSH1 0x06 PUSH1 0x07 PUSH1 0x08 PUSH1 0x09 PUSH1 0x0a PUSH1 0x0b PUSH1 0x0c PUSH1 0x0d PUSH1 0x0e PUSH1 0x0f PUSH1 0x10 PUSH1 0x11 PUSH1 0x12 PUSH1 0x13 PUSH1 0x14 PUSH1 0x13 DUPN", 22);
        Stack stack = program.getStack();

        Assert.assertEquals(21, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());

        for (int k = 0; k < 20; k++)
            Assert.assertEquals(DataWord.valueOf(k + 1), stack.get(k));
    }

    @Test(expected = Program.StackTooSmallException.class)
    public void dupnTwentiethItemWithoutEnoughItems() {
        executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x03 PUSH1 0x04 PUSH1 0x05 PUSH1 0x06 PUSH1 0x07 PUSH1 0x08 PUSH1 0x09 PUSH1 0x0a PUSH1 0x0b PUSH1 0x0c PUSH1 0x0d PUSH1 0x0e PUSH1 0x0f PUSH1 0x10 PUSH1 0x11 PUSH1 0x12 PUSH1 0x13 PUSH1 0x13 DUPN", 21);
    }

    @Test(expected = Program.StackTooSmallException.class)
    public void dupnTooManyItemsWithOverflow() {
        executeCode("PUSH1 0x01 PUSH4 0x7f 0xff 0xff 0xff DUPN", 3);
    }

    @Test
    public void swapnSecondItem() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x00 SWAPN", 4);
        Stack stack = program.getStack();

        Assert.assertEquals(2, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());
        Assert.assertEquals(DataWord.valueOf(2), stack.get(0));
    }

    @Test
    public void swapnFourthItem() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x03 PUSH1 0x04 PUSH1 0x02 SWAPN", 6);
        Stack stack = program.getStack();

        Assert.assertEquals(4, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());
        Assert.assertEquals(DataWord.valueOf(4), stack.get(0));
        Assert.assertEquals(DataWord.valueOf(2), stack.get(1));
        Assert.assertEquals(DataWord.valueOf(3), stack.get(2));
    }

    @Test
    public void swapnTwentiethItem() {
        Program program = executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x03 PUSH1 0x04 PUSH1 0x05 PUSH1 0x06 PUSH1 0x07 PUSH1 0x08 PUSH1 0x09 PUSH1 0x0a PUSH1 0x0b PUSH1 0x0c PUSH1 0x0d PUSH1 0x0e PUSH1 0x0f PUSH1 0x10 PUSH1 0x11 PUSH1 0x12 PUSH1 0x13 PUSH1 0x14 PUSH1 0x12 SWAPN", 22);
        Stack stack = program.getStack();

        Assert.assertEquals(20, stack.size());
        Assert.assertEquals(DataWord.valueOf(1), stack.peek());
        Assert.assertEquals(DataWord.valueOf(20), stack.get(0));

        for (int k = 1; k < 19; k++)
            Assert.assertEquals(DataWord.valueOf(k + 1), stack.get(k));
    }

    @Test(expected = Program.StackTooSmallException.class)
    public void swapnTooManyItemsWithOverflow() {
        executeCode("PUSH1 0x01 PUSH1 0x01 PUSH4 0x7f 0xff 0xff 0xff SWAPN", 4);
    }

    @Test(expected = Program.StackTooSmallException.class)
    public void swapnTwentiethItemWithoutEnoughItems() {
        executeCode("PUSH1 0x01 PUSH1 0x02 PUSH1 0x03 PUSH1 0x04 PUSH1 0x05 PUSH1 0x06 PUSH1 0x07 PUSH1 0x08 PUSH1 0x09 PUSH1 0x0a PUSH1 0x0b PUSH1 0x0c PUSH1 0x0d PUSH1 0x0e PUSH1 0x0f PUSH1 0x10 PUSH1 0x11 PUSH1 0x12 PUSH1 0x13 PUSH1 0x12 SWAPN", 22);
    }

    @Test
    public void txindexExecution() {
        invoke.setTransactionIndex(DataWord.valueOf(42));
        Program program = executeCode("TXINDEX", 1);
        Stack stack = program.getStack();

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(DataWord.valueOf(42), stack.peek());
    }

    @Test
    public void invalidJustAfterEndOfCode() {
        try {
            executeCode("PUSH1 0x03 JUMP", 2);
            Assert.fail();
        }
        catch (Program.BadJumpDestinationException ex) {
            Assert.assertEquals("Operation with pc isn't 'JUMPDEST': PC[3];", ex.getMessage());
        }
    }

    @Test
    public void invalidJumpOutOfRange() {
        try {
            executeCode("PUSH1 0x05 JUMP", 2);
            Assert.fail();
        }
        catch (Program.BadJumpDestinationException ex) {
            Assert.assertEquals("Operation with pc isn't 'JUMPDEST': PC[5];", ex.getMessage());
        }
    }

    @Test
    public void invalidNegativeJump() {
        try {
            executeCode("PUSH32 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff JUMP", 2);
            Assert.fail();
        }
        catch (Program.BadJumpDestinationException ex) {
            Assert.assertEquals("Operation with pc isn't 'JUMPDEST': PC[-1];", ex.getMessage());
        }
    }

    @Test
    public void invalidTooFarJump() {
        try {
            executeCode("PUSH1 0xff JUMP", 2);
            Assert.fail();
        }
        catch (Program.BadJumpDestinationException ex) {
            Assert.assertEquals("Operation with pc isn't 'JUMPDEST': PC[255];", ex.getMessage());
        }
    }

    @Test
    public void dupnArgumentIsNotJumpdest() {
        byte[] code = compiler.compile("JUMPDEST DUPN 0x5b 0x5b");
        Program program = new Program(vmConfig, precompiledContracts, blockFactory, mock(BlockchainConfig.class), code, invoke, null);

        BitSet jumpdestSet = program.getJumpdestSet();

        Assert.assertNotNull(jumpdestSet);
        Assert.assertEquals(4, jumpdestSet.size());
        Assert.assertTrue(jumpdestSet.get(0));
        Assert.assertFalse(jumpdestSet.get(1));
        Assert.assertTrue(jumpdestSet.get(2));
        Assert.assertTrue(jumpdestSet.get(3));
    }

    @Test
    public void swapnArgumentIsNotJumpdest() {
        byte[] code = compiler.compile("JUMPDEST SWAPN 0x5b 0x5b");
        Program program = new Program(vmConfig, precompiledContracts, blockFactory, mock(BlockchainConfig.class), code, invoke, null);

        BitSet jumpdestSet = program.getJumpdestSet();

        Assert.assertNotNull(jumpdestSet);
        Assert.assertEquals(4, jumpdestSet.size());
        Assert.assertTrue(jumpdestSet.get(0));
        Assert.assertFalse(jumpdestSet.get(1));
        Assert.assertTrue(jumpdestSet.get(2));
        Assert.assertTrue(jumpdestSet.get(3));
    }

    @Test
    public void thePathOfFifteenThousandJumps() {
        byte[] bytecode = new byte[15000 * 6 + 3];

        int k = 0;

        while (k < 15000 * 6) {
            int target = k + 6;
            bytecode[k++] = 0x5b; // JUMPDEST
            bytecode[k++] = 0x62; // PUSH3
            bytecode[k++] = (byte)(target >> 16);
            bytecode[k++] = (byte)(target >> 8);
            bytecode[k++] = (byte)(target & 0xff);
            bytecode[k++] = 0x56; // JUMP
        }

        bytecode[k++] = 0x5b; // JUMPDEST
        bytecode[k++] = 0x60; // PUSH1
        bytecode[k++] = 0x01; // 1

        ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        long initialTime = thread.getCurrentThreadCpuTime();
        testCode(bytecode, 15000 * 3 + 2, "0000000000000000000000000000000000000000000000000000000000000001");
        long finalTime = thread.getCurrentThreadCpuTime();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.println(String.format("Execution Time %s nanoseconds", finalTime - initialTime));
        System.out.println(String.format("Delta memory %s", finalMemory - initialMemory));
    }

    @Test
    public void returnDataSizeBasicGasCost() {
        Program program = executeCode("0x3d", 1);

        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getResult());
        Assert.assertNull(program.getResult().getException());
        Assert.assertEquals(2, program.getResult().getGasUsed());
    }

    @Test
    public void returnDataCopyBasicGasCost() {
        Program program = executeCode(
                // push some values for RETURNDATACOPY
                "PUSH1 0x00 PUSH1 0x00 PUSH1 0x01 " +
                // call RETURNDATACOPY
                "0x3e",
        4);

        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getResult());
        Assert.assertNull(program.getResult().getException());
        Assert.assertEquals(12, program.getResult().getGasUsed());
    }

    private void callCreate2WithBlockchainConfig(String address, String salt, String pushInitCode, int size, int intOffset, String expected, long gasExpected, BlockchainConfig blockchainConfig) {
        int value = 10;
        RskAddress testAddress = new RskAddress(address);
        invoke.setOwnerAddress(testAddress);
        invoke.getRepository().addBalance(testAddress, Coin.valueOf(value + 1));
        String inSize = "0x" + DataWord.valueOf(size);
        String inOffset = "0x" + DataWord.valueOf(intOffset);

        if (!pushInitCode.isEmpty()) {
            pushInitCode += " PUSH1 0x00 MSTORE";
        }

        Program program = executeCodeWithBlockchainConfig(
                pushInitCode +
                        " PUSH32 " + salt +
                        " PUSH32 " + inSize +
                        " PUSH32 " + inOffset +
                        " PUSH32 " + "0x" + DataWord.valueOf(value) +
                        " CREATE2", 8, blockchainConfig);
        Stack stack = program.getStack();
        String result = Hex.toHexString(Arrays.copyOfRange(stack.peek().getData(), 12, stack.peek().getData().length));

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals(expected.toUpperCase(), result.toUpperCase());
        Assert.assertEquals(gasExpected, program.getResult().getGasUsed());
    }

    private void callCreate2(String address, String salt, String pushInitCode, int size, int intOffset, String expected, long gasExpected) {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip125()).thenReturn(true);
        callCreate2WithBlockchainConfig(address, salt, pushInitCode, size, intOffset, expected, gasExpected, blockchainConfig);
    }

    @Test
    public void testCREATE2_0() {
        callCreate2("0x0f6510583d425cfcf94b99f8b073b44f60d1912b",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                "PUSH32 " + "0x600b000000000000000000000000000000000000000000000000000000000000",
                2,
                0,
                "3BC3EFA1C487A1EBFC911B47B548E2C82436A212",
                32033);
    }

    @Test
    public void testCREATE2_1() {
        callCreate2("0x0f6510583d425cfcf94b99f8b073b44f60d1912b",
                "0x00000000000000000000000000000000000000000000000000000000cafebabe",
                "PUSH32 0x601b000000000000000000000000000000000000000000000000000000000000",
                2,
                0,
                "19542B03F2D5D4E1910DBE096FAF0842D928883D",
                32033);
    }

    @Test
    public void testCREATE2_2() {
        callCreate2("0xdeadbeef00000000000000000000000000000000",
                "0x00000000000000000000000000000000000000000000000000000000cafebabe",
                "PUSH32 0x601b000000000000000000000000000000000000000000000000000000000000",
                2,
                0,
                "3BA1DC70CC17E740F4BD85052AF074B2B2A49E06",
                32033);
    }

    @Test
    public void testCREATE2_3() {
        callCreate2("0xdeadbeef00000000000000000000000000000000",
                "0x00000000000000000000000000000000000000000000000000000000cafebabe",
                "PUSH32 0x601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b",
                32,
                0,
                "D1FB828980EC250DD0A350E59108ECC63C2C4B36",
                32078);
    }

    @Test
    public void testCREATE2_4() {
        callCreate2("0x0f6510583d425cfcf94b99f8b073b44f60d1912b",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                "PUSH32 0x601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b",
                0,
                0,
                "65BD0714DEFB919BC02F9507D6F9D9CD21195ECC",
                32024);
    }

    @Test
    public void testCREATE2_5() {
        callCreate2("0x0f6510583d425cfcf94b99f8b073b44f60d1912b",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                "PUSH32 0x601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b",
                10,
                8,
                "A992CD9E3E78C0A6BBBB4F06B52B3AD8924B0916",
                32045);
    }

    @Test
    public void testCREATE2_6() {
        callCreate2("0x0f6510583d425cfcf94b99f8b073b44f60d1912b",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                "",
                12,
                0,
                "16F27A604035007FA9925DB8CC2CAFDCFFC6278C",
                32021);
    }

    @Test
    public void testCREATE2_7() {
        callCreate2("0x0f6510583d425cfcf94b99f8b073b44f60d1912b",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                "",
                0,
                0,
                "65BD0714DEFB919BC02F9507D6F9D9CD21195ECC",
                32012);
    }

    @Test
    public void testCREATE2_InvalidInitCode() {
        // INIT_CODE fails so it returns a ZERO address
        // as it fails, it spends all the gas
        callCreate2("0x0000000000000000000000000000000000000000",
                "0xAE00000000000000000000000000000000000000000000000000000000000000",
                "PUSH32 0xAE00000000000000000000000000000000000000000000000000000000000000",
                1,
                0,
                "0000000000000000000000000000000000000000",
                1000000);
    }

    @Test(expected = Program.IllegalOperationException.class)
    public void testCREATE2ShouldFailInvalidOpcode() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip125()).thenReturn(false);
        callCreate2WithBlockchainConfig("0x0000000000000000000000000000000000000000",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                "PUSH32 0x601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b601b",
                10,
                8,
                "A992CD9E3E78C0A6BBBB4F06B52B3AD8924B0916",
                32045,
                blockchainConfig);
    }

    @Test
    public void callDataCopyBasicGasCost() {
        Program program = executeCode(
                // push some values for CALLDATACOPY
                "PUSH1 0x00 PUSH1 0x00 PUSH1 0x01 " +
                // call CALLDATACOPY
                "0x37",
        4);

        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getResult());
        Assert.assertNull(program.getResult().getException());
        Assert.assertEquals(12, program.getResult().getGasUsed());
    }

    private Program executeCode(String code, int nsteps) {
        return executeCodeWithBlockchainConfig(compiler.compile(code), nsteps, mock(BlockchainConfig.class));
    }

    private void testCode(byte[] code, int nsteps, String expected) {
        Program program = executeCodeWithBlockchainConfig(code, nsteps, mock(BlockchainConfig.class));

        assertEquals(expected, Hex.toHexString(program.getStack().peek().getData()).toUpperCase());
    }


    private Program executeCodeWithBlockchainConfig(String code, int nsteps, BlockchainConfig blockchainConfig) {
        return executeCodeWithBlockchainConfig(compiler.compile(code), nsteps, blockchainConfig);
    }


    private static Transaction createTransaction(int number) {
        AccountBuilder acbuilder = new AccountBuilder();
        acbuilder.name("sender" + number);
        Account sender = acbuilder.build();
        acbuilder.name("receiver" + number);
        Account receiver = acbuilder.build();
        TransactionBuilder txbuilder = new TransactionBuilder();
        return txbuilder.sender(sender).receiver(receiver).value(BigInteger.valueOf(number * 1000 + 1000)).build();
    }


    private Program executeCodeWithBlockchainConfig(byte[] code, int nsteps, BlockchainConfig blockchainConfig) {
        VM vm = new VM(vmConfig, precompiledContracts);
        Program program = new Program(vmConfig, precompiledContracts, blockFactory, blockchainConfig, code, invoke, createTransaction(1) );

        for (int k = 0; k < nsteps; k++) {
            vm.step(program);
        }

        return program;
    }
}
