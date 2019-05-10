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

import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AbstractBlockchainImpl implements AbstractBlockchain {
    private final Object internalBlockStoreReadWriteLock = new Object();

    private final int height;

    private Blockchain realBlockchain;

    @GuardedBy("internalBlockStoreReadWriteLock")
    private Map<Keccak256, Block> blocksByHash;

    @GuardedBy("internalBlockStoreReadWriteLock")
    private Map<Long, List<Block>> blocksByNumber;

    @GuardedBy("internalBlockStoreReadWriteLock")
    private List<Block> blockchain;

    public AbstractBlockchainImpl(Blockchain realBlockchain, int height) {
        this.height = height;
        this.realBlockchain = realBlockchain;
        this.blocksByHash = new ConcurrentHashMap<>();
        this.blocksByNumber = new ConcurrentHashMap<>();
        fillInternalsBlockStore(realBlockchain.getBestBlock());
    }

    @Override
    public void addBestBlock(Block bestBlock) {
        synchronized (internalBlockStoreReadWriteLock) {
            blocksByHash.put(bestBlock.getHash(), bestBlock);
            addToBlockByNumberMap(bestBlock);

            fillInternalsBlockStore(bestBlock);
            deleteEntriesOutOfBoundaries(bestBlock.getNumber());
        }
    }

    @Override
    public List<Block> get() {
        synchronized (internalBlockStoreReadWriteLock) {
            return blockchain;
        }
    }

    @Override
    public Block getBestBlock() {
        synchronized (internalBlockStoreReadWriteLock) {
            return blockchain.get(0);
        }
    }

    @Override
    public Block getBlockByNumber(long number) {
        synchronized (internalBlockStoreReadWriteLock) {
            if (blocksByNumber.containsKey(number)) {
                return blocksByHash.get(number);
            }
        }

        return realBlockchain.getBlockByNumber(number);
    }

    private void fillInternalsBlockStore(Block bestBlock) {
        List<Block> newBlockchain = new ArrayList<>(height);
        Block currentBlock = bestBlock;
        for(int i = 0; i < height; i++) {
            newBlockchain.add(currentBlock);

            if(!blocksByHash.containsKey(currentBlock.getHash())) {
                blocksByHash.put(currentBlock.getHash(), currentBlock);
                addToBlockByNumberMap(currentBlock);
            }

            if(currentBlock.isGenesis()) {
                break;
            }
            currentBlock = realBlockchain.getBlockByHash(currentBlock.getParentHash().getBytes());
        }

        blockchain = newBlockchain;
    }

    private void addToBlockByNumberMap(Block blockToAdd) {
        long currentBlockNumber = blockToAdd.getNumber();
        if (blocksByNumber.containsKey(currentBlockNumber)) {
            blocksByNumber.get(currentBlockNumber).add(blockToAdd);
        } else {
            blocksByNumber.put(blockToAdd.getNumber(), new ArrayList<>(Collections.singletonList(blockToAdd)));
        }
    }

    private void deleteEntriesOutOfBoundaries(long bestBlockNumber) {
        long blocksHeightToDelete = bestBlockNumber - height;
        if(blocksHeightToDelete >= 0) {
            blocksByNumber.get(blocksHeightToDelete).forEach(blockToDelete -> blocksByHash.remove(blockToDelete.getHash()));
            blocksByNumber.remove(blocksHeightToDelete);
        }
    }
}
