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

package co.rsk.core;

import co.rsk.crypto.Keccak256;
import co.rsk.peg.PegTestUtils;
import org.ethereum.TestUtils;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.core.BlockFactory;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Bloom;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlockFactoryTest {

    @Test
    public void newHeaderWithNoForkDetectionDataAndRskip110On() {
        long number = 20L;
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip92()).thenReturn(true);
        when(blockchainConfig.isRskip110()).thenReturn(true);

        BlockchainNetConfig config = mock(BlockchainNetConfig.class);
        when(config.getConfigForBlock(number)).thenReturn(blockchainConfig);

        BlockFactory factory = new BlockFactory(config);
        BlockHeader header = createBlockHeader(factory, number, new byte[0]);

        Keccak256 hash = header.getHash();
        byte[] hashForMergedMining = header.getHashForMergedMining();

        assertThat(hash.getBytes(), is(hashForMergedMining));
    }

    @Test
    public void decodeWithNoForkDetectionDataAndRskip110On() {
        long number = 20L;
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip92()).thenReturn(true);
        when(blockchainConfig.isRskip110()).thenReturn(true);

        BlockchainNetConfig config = mock(BlockchainNetConfig.class);
        when(config.getConfigForBlock(number)).thenReturn(blockchainConfig);

        BlockFactory factory = new BlockFactory(config);
        BlockHeader header = createBlockHeaderWithMergedMiningFields(factory, number, new byte[0]);

        byte[] encodedHeader = header.getEncoded();
        RLPList headerRLP = RLP.decodeList(encodedHeader);
        assertThat(headerRLP.size(), is(19));

        BlockHeader decodedHeader = factory.decodeHeader(encodedHeader);
        assertThat(header.getHash(), is(decodedHeader.getHash()));
    }

    @Test
    public void decodeWithNoForkDetectionDataAndRskip110Off() {
        long number = 20L;
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip92()).thenReturn(true);
        when(blockchainConfig.isRskip110()).thenReturn(false);

        BlockchainNetConfig config = mock(BlockchainNetConfig.class);
        when(config.getConfigForBlock(number)).thenReturn(blockchainConfig);

        BlockFactory factory = new BlockFactory(config);
        BlockHeader header = createBlockHeaderWithMergedMiningFields(factory, number, new byte[0]);

        byte[] encodedHeader = header.getEncoded();
        RLPList headerRLP = RLP.decodeList(encodedHeader);
        assertThat(headerRLP.size(), is(19));

        BlockHeader decodedHeader = factory.decodeHeader(encodedHeader);
        assertThat(header.getHash(), is(decodedHeader.getHash()));
    }

    @Test
    public void decodeWithForkDetectionDataAndRskip110Off() {
        long number = 20L;
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip92()).thenReturn(true);
        when(blockchainConfig.isRskip110()).thenReturn(false);

        BlockchainNetConfig config = mock(BlockchainNetConfig.class);
        when(config.getConfigForBlock(number)).thenReturn(blockchainConfig);

        BlockFactory factory = new BlockFactory(config);
        byte[] forkDetectionData = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        BlockHeader header = createBlockHeaderWithMergedMiningFields(factory, number, forkDetectionData);

        byte[] encodedHeader = header.getEncoded();
        RLPList headerRLP = RLP.decodeList(encodedHeader);
        assertThat(headerRLP.size(), is(19));

        BlockHeader decodedHeader = factory.decodeHeader(encodedHeader);
        assertThat(header.getHash(), is(decodedHeader.getHash()));
    }

    @Test
    public void decodeWithForkDetectionDataAndRskip110On() {
        long number = 20L;
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip92()).thenReturn(true);
        when(blockchainConfig.isRskip110()).thenReturn(true);

        BlockchainNetConfig config = mock(BlockchainNetConfig.class);
        when(config.getConfigForBlock(number)).thenReturn(blockchainConfig);

        BlockFactory factory = new BlockFactory(config);
        byte[] forkDetectionData = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        BlockHeader header = createBlockHeaderWithMergedMiningFields(factory, number, forkDetectionData);

        byte[] encodedHeader = header.getEncoded();
        RLPList headerRLP = RLP.decodeList(encodedHeader);
        assertThat(headerRLP.size(), is(20));

        BlockHeader decodedHeader = factory.decodeHeader(encodedHeader);
        assertThat(header.getHash(), is(decodedHeader.getHash()));
    }

    @Test
    public void decodeWithNoMergedMiningData() {
        long number = 20L;
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        when(blockchainConfig.isRskip92()).thenReturn(true);

        BlockchainNetConfig config = mock(BlockchainNetConfig.class);
        when(config.getConfigForBlock(number)).thenReturn(blockchainConfig);

        BlockFactory factory = new BlockFactory(config);
        BlockHeader header = createBlockHeader(factory, number, new byte[0]);

        byte[] encodedHeader = header.getEncoded(false, false);
        RLPList headerRLP = RLP.decodeList(encodedHeader);
        assertThat(headerRLP.size(), is(16));

        BlockHeader decodedHeader = factory.decodeHeader(encodedHeader);
        assertThat(header.getHash(), is(decodedHeader.getHash()));
    }

    private BlockHeader createBlockHeaderWithMergedMiningFields(
            BlockFactory factory,
            long number,
            byte[] forkDetectionData) {
        byte[] difficulty = BigInteger.ONE.toByteArray();
        byte[] gasLimit = BigInteger.valueOf(6800000).toByteArray();
        long timestamp = 7731067; // Friday, 10 May 2019 6:04:05

        return factory.newHeader(
                PegTestUtils.createHash3().getBytes(),
                HashUtil.keccak256(RLP.encodeList()),
                TestUtils.randomAddress().getBytes(),
                HashUtil.EMPTY_TRIE_HASH,
                "tx_trie_root".getBytes(),
                HashUtil.EMPTY_TRIE_HASH,
                new Bloom().getData(),
                difficulty,
                number,
                gasLimit,
                3000000L,
                timestamp,
                null,
                Coin.ZERO,
                new byte[80],
                new byte[32],
                new byte[128],
                forkDetectionData,
                Coin.valueOf(10L).getBytes(),
                0);
    }

    private BlockHeader createBlockHeader(
            BlockFactory factory,
            long number,
            byte[] forkDetectionData) {
        byte[] difficulty = BigInteger.ONE.toByteArray();
        byte[] gasLimit = BigInteger.valueOf(6800000).toByteArray();
        long timestamp = 7731067; // Friday, 10 May 2019 6:04:05

        return factory.newHeader(
                PegTestUtils.createHash3().getBytes(),
                HashUtil.keccak256(RLP.encodeList()),
                TestUtils.randomAddress().getBytes(),
                HashUtil.EMPTY_TRIE_HASH,
                "tx_trie_root".getBytes(),
                HashUtil.EMPTY_TRIE_HASH,
                new Bloom().getData(),
                difficulty,
                number,
                gasLimit,
                3000000L,
                timestamp,
                null,
                Coin.ZERO,
                null,
                null,
                null,
                forkDetectionData,
                Coin.valueOf(10L).getBytes(),
                0);
    }
}
