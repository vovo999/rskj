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

import co.rsk.bitcoinj.core.Context;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.params.RegTestParams;
import org.ethereum.core.Block;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.IntStream;

public class ForkDetectionDataBuilder {

    private static final int CPV_SIZE = 7;

    private static final int CPV_JUMP_FACTOR = 64;

    private static final int NUMBER_OF_UNCLES = 32;

    private final List<Block> mainchainBlocks;

    ForkDetectionDataBuilder(List<Block> mainchainBlocks) {
        this.mainchainBlocks = mainchainBlocks;
    }

    public byte[] build() {
        // + 1 because genesis block can't be used since it does not contain a valid BTC header
        if (mainchainBlocks.size() < CPV_SIZE * CPV_JUMP_FACTOR + 1) {
            return new byte[0];
        }

        NetworkParameters params = RegTestParams.get();
        new Context(params);

        long bestBlockHeight = mainchainBlocks.get(0).getNumber();
        long blockBeingMinedHeight = bestBlockHeight + 1;
        long cpvStartHeight = isMultipleOf64(blockBeingMinedHeight) ? (long)Math.floor((blockBeingMinedHeight - 1)/(double)64) * 64 : (long)Math.floor(blockBeingMinedHeight/(double)64) * 64;

        byte[] forkDetectionData = new byte[12];
        for(int i = 0; i < CPV_SIZE; i++){
            long currentCpvElement = bestBlockHeight - cpvStartHeight + i * 64;
            Block block = mainchainBlocks.get((int)currentCpvElement);
            byte[] bitcoinBlock = block.getBitcoinMergedMiningHeader();

            byte[] bitcoinBlockHash = params.getDefaultSerializer().makeBlock(bitcoinBlock).getHash().getBytes();
            byte leastSignificantByte = bitcoinBlockHash[bitcoinBlockHash.length - 1];

            forkDetectionData[i] = leastSignificantByte;
        }

        short numberOfUncles = (short)IntStream.range(0, 32).map(i -> mainchainBlocks.get(0).getUncleList().size()).sum();

        forkDetectionData[7] = ByteBuffer.allocate(2).putShort(numberOfUncles).array()[0];

        byte[] blockBeingMinedNumber = ByteBuffer.allocate(4).putInt((int)blockBeingMinedHeight).array();
        System.arraycopy(blockBeingMinedNumber, 0, forkDetectionData, 8, 4);

        return forkDetectionData;
    }

    private boolean isMultipleOf64(long number){
        return number % 64 == 0;
    }
}