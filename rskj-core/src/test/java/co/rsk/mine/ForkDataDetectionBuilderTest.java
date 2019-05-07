/*
 * This file is part of RskJ
 * Copyright (C) 2019 RSK Labs Ltd.
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

package co.rsk.mine;

import co.rsk.bitcoinj.core.BtcBlock;
import co.rsk.bitcoinj.core.Context;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.params.RegTestParams;
import co.rsk.crypto.Keccak256;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.core.Block;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ForkDataDetectionBuilderTest {

    @Test
    public void creationIsCorrectWithMinPossibleBlockchainHeight() {
        List<Block> lastBlockchainBlocks = createBlockchainAsList(449);

        ForkDetectionDataBuilder builder = new ForkDetectionDataBuilder(lastBlockchainBlocks);

        byte[] forkDetectionData = builder.build();

        assertThat(forkDetectionData.length, is(12));

        assertThat(forkDetectionData[0],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(0).getBitcoinMergedMiningHeader())));
        assertThat(forkDetectionData[1],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(64).getBitcoinMergedMiningHeader())));
        assertThat(forkDetectionData[2],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(128).getBitcoinMergedMiningHeader())));
        assertThat(forkDetectionData[3],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(192).getBitcoinMergedMiningHeader())));
        assertThat(forkDetectionData[4],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(256).getBitcoinMergedMiningHeader())));
        assertThat(forkDetectionData[5],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(320).getBitcoinMergedMiningHeader())));
        assertThat(forkDetectionData[6],
                is(getBtcBlockHashLeastSignificantByte(lastBlockchainBlocks.get(384).getBitcoinMergedMiningHeader())));

        assertThat(forkDetectionData[7], is((byte)0));

        assertThat(forkDetectionData[8], is((byte)0));
        assertThat(forkDetectionData[9], is((byte)0));
        assertThat(forkDetectionData[10], is((byte)1));
        assertThat(forkDetectionData[11], is((byte)193));
    }

    private byte getBtcBlockHashLeastSignificantByte(byte[] array) {
        NetworkParameters params = RegTestParams.get();
        new Context(params);

        byte[] blockHash = params.getDefaultSerializer().makeBlock(array).getHash().getBytes();

        return blockHash[blockHash.length - 1];
    }

    private List<Block> createBlockchainAsList(int height) {
        List<Block> blockchainAsList = new ArrayList<>();

        Block previousBlock = createGenesisBlock();
        blockchainAsList.add(previousBlock);

        long bitcoinBlockTime = 1557185216L;
        for(long i = 1; i < height; i++) {
            Block block = createBlock(i, previousBlock.getHash(), bitcoinBlockTime);
            blockchainAsList.add(block);

            // There are 20 RSK blocks per BTC block
            if(i % 20 == 0) {
                bitcoinBlockTime++;
            }

            previousBlock = block;
        }

        Collections.reverse(blockchainAsList);

        return blockchainAsList;
    }

    private Block createGenesisBlock(){
        Block block = mock(Block.class);
        when(block.isGenesis()).thenReturn(Boolean.TRUE);
        when(block.getNumber()).thenReturn(Long.valueOf(0));
        byte[] rawBlockHash = getRandomHash();
        Keccak256 blockHash = new Keccak256(rawBlockHash);
        when(block.getHash()).thenReturn(blockHash);
        when(block.getBitcoinMergedMiningHeader()).thenReturn(getRandomHash());

        return block;
    }

    private Block createBlock(long number, Keccak256 parentHash, long bitcoinBlockTime){
        Block block = mock(Block.class);
        when(block.getNumber()).thenReturn(number);
        byte[] rawBlockHash = getRandomHash();
        Keccak256 blockHash = new Keccak256(rawBlockHash);
        when(block.getHash()).thenReturn(blockHash);
        when(block.getParentHash()).thenReturn(parentHash);
        byte[] bitcoinHeader = getBtcBlock(bitcoinBlockTime).cloneAsHeader().bitcoinSerialize();
        when(block.getBitcoinMergedMiningHeader()).thenReturn(bitcoinHeader);

        return block;
    }

    private byte[] getRandomHash() {
        byte[] byteArray = new byte[32];
        try {
            SecureRandom.getInstanceStrong().nextBytes(byteArray);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return byteArray;
    }

    private BtcBlock getBtcBlock(long blockTime) {
        // Use a BTC mainnet header as template for creating BTC blocks
        String bitcoinBlockHeaderHex = "0000002031dfbd80218f575b9155c7dabe7245519e7f308fb61f0e0000000000000000003b" +
                "feed041498b5ea8227871ddee7a19a0bba806a2fe755d48a1fa48b6a04dc89c0c2d05c38ff29172ca971f7";
        NetworkParameters params = RegTestParams.get();
        new Context(params);
        byte[] bitcoinBlockByteArray = Hex.decode(bitcoinBlockHeaderHex);

        BtcBlock bitcoinBlock = params.getDefaultSerializer().makeBlock(bitcoinBlockByteArray);
        bitcoinBlock.setTime(blockTime);

        return bitcoinBlock;
    }
}
