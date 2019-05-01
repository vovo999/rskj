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

import co.rsk.crypto.Keccak256;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AbstractBlockchainTest {

    @Test
    public void createWithLessBlocksThanMaxHeight() {
        AbstractBlockchain testBlockchain = new AbstractBlockchain(createBlockchain(10), 11);

        List<Block> result = testBlockchain.get();

        assertNotNull(result);
        assertThat(result.size(), is(10));
    }

    private Blockchain createBlockchain(int height) {
        Blockchain blockchain = mock(Blockchain.class);

        Block previousBlock = createGenesisBlock();
        when(blockchain.getBlockByHash(previousBlock.getHash().getBytes())).thenReturn(previousBlock);

        for(long i = 1; i < height; i++) {
            Block block = createBlock(i, previousBlock.getHash());
            when(blockchain.getBlockByHash(block.getHash().getBytes())).thenReturn(block);

            if(i == height - 1) {
                when(blockchain.getBestBlock()).thenReturn(block);
            }

            previousBlock = block;
        }

        return blockchain;
    }

    private Block createGenesisBlock(){
        Block block = mock(Block.class);
        when(block.isGenesis()).thenReturn(Boolean.TRUE);
        when(block.getNumber()).thenReturn(Long.valueOf(0));
        byte[] rawBlockHash = getRandomHash();
        Keccak256 blockHash = new Keccak256(rawBlockHash);
        when(block.getHash()).thenReturn(blockHash);

        return block;
    }

    private Block createBlock(long number, Keccak256 parentHash){
        Block block = mock(Block.class);
        when(block.getNumber()).thenReturn(number);
        byte[] rawBlockHash = getRandomHash();
        Keccak256 blockHash = new Keccak256(rawBlockHash);
        when(block.getHash()).thenReturn(blockHash);
        when(block.getParentHash()).thenReturn(parentHash);

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
}
