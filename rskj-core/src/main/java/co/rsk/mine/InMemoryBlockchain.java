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

public class InMemoryBlockchain {

    private static final int STORE_SIZE = 448;

    private final Object readWriteLock = new Object();

    private Blockchain blockchain;

    private Block bestBlock;

    @GuardedBy("readWriteLock")
    private Map<Keccak256, Block> blocksByHash;

    @GuardedBy("readWriteLock")
    private Map<Long, List<Block>> blocksByNumber;

    public InMemoryBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.bestBlock = blockchain.getBestBlock();
        this.blocksByHash = new ConcurrentHashMap<>();
        this.blocksByNumber = new ConcurrentHashMap<>();
        fillBlockStoreWithMissingBlocks(bestBlock, STORE_SIZE);
    }

    public synchronized void add(Block blockToAdd) {
        bestBlock = blockToAdd;

        synchronized (readWriteLock) {
            blocksByHash.put(blockToAdd.getHash(), blockToAdd);
            addToBlockByNumberMap(blockToAdd);

            // there was at least one block at the level of blockToAdd
            if (!blocksByHash.containsKey(blockToAdd.getParentHash())) {
                fillBlockStoreWithMissingBlocks(blockchain.getBlockByHash(blockToAdd.getParentHash().getBytes()),
                        STORE_SIZE - 1);
            }

            deleteEntriesOutOfBoundaries();
        }
    }

    public synchronized List<Block> get() {
        ArrayList<Block> resultBlockchain = new ArrayList<>();

        synchronized (readWriteLock){
            Block currentBlock = bestBlock;
            for(int i = 0; i < STORE_SIZE; i++) {
                resultBlockchain.add(currentBlock);
                currentBlock = blocksByHash.get(currentBlock.getParentHash());
            }
        }

        return resultBlockchain;
    }

    private void fillBlockStoreWithMissingBlocks(Block bestBlock, int numberOfBlocksToFill) {
        Block currentBlock = bestBlock;
        for(int i=0; i < numberOfBlocksToFill; i++) {
            blocksByHash.put(currentBlock.getHash(), currentBlock);
            addToBlockByNumberMap(currentBlock);
            currentBlock = blockchain.getBlockByHash(bestBlock.getParentHash().getBytes());
        }
    }

    private void addToBlockByNumberMap(Block blockToAdd) {
        long currentBlockNumber = blockToAdd.getNumber();
        if (blocksByNumber.containsKey(currentBlockNumber)) {
            blocksByNumber.get(currentBlockNumber).add(blockToAdd);
        } else {
            blocksByNumber.put(blockToAdd.getNumber(), new ArrayList<>(Collections.singletonList(blockToAdd)));
        }
    }

    private void deleteEntriesOutOfBoundaries() {
        long blocksHeightToDelete = bestBlock.getNumber() - STORE_SIZE;
        if(blocksHeightToDelete > 0) {
            blocksByNumber.get(blocksHeightToDelete).forEach(blockToDelete -> blocksByHash.remove(blockToDelete.getHash()));
            blocksByNumber.remove(blocksHeightToDelete);
        }
    }
}
