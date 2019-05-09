/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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

package org.ethereum.core;

import co.rsk.config.TestSystemProperties;
import co.rsk.core.RskAddress;
import co.rsk.core.bc.BlockChainImpl;
import co.rsk.core.bc.BlockExecutor;
import co.rsk.core.bc.TransactionPoolImpl;
import co.rsk.db.MutableTrieImpl;
import co.rsk.db.StateRootHandler;
import co.rsk.trie.Trie;
import co.rsk.trie.TrieConverter;
import co.rsk.trie.TrieStoreImpl;
import co.rsk.validators.DummyBlockValidator;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.db.MutableRepository;
import org.ethereum.db.ReceiptStore;
import org.ethereum.db.ReceiptStoreImpl;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.TestCompositeEthereumListener;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anton Nashatyrev on 29.12.2015.
 */
public class ImportLightTest {

    public static BlockChainImpl createBlockchain(Genesis genesis, TestSystemProperties config) {
        BlockFactory blockFactory = new BlockFactory(config.getActivationConfig());
        IndexedBlockStore blockStore = new IndexedBlockStore(blockFactory, new HashMap<>(), new HashMapDB(), null);

        Repository repository = new MutableRepository(new MutableTrieImpl(new Trie(new TrieStoreImpl(new HashMapDB()))));

        CompositeEthereumListener listener = new TestCompositeEthereumListener();

        KeyValueDataSource ds = new HashMapDB();
        ds.init();
        ReceiptStore receiptStore = new ReceiptStoreImpl(ds);

        TransactionPoolImpl transactionPool = new TransactionPoolImpl(config, repository, null, receiptStore,
                                                                      blockFactory,null, listener, 10, 100
        );

        final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();
        StateRootHandler stateRootHandler = new StateRootHandler(config.getActivationConfig(), new TrieConverter(), new HashMapDB(), new HashMap<>());
        BlockChainImpl blockchain = new BlockChainImpl(
                repository,
                blockStore,
                receiptStore,
                transactionPool,
                listener,
                new DummyBlockValidator(),
                false,
                1,
                new BlockExecutor(repository, (tx1, txindex1, coinbase, track1, block1, totalGasUsed1) -> new TransactionExecutor(
                        tx1,
                        txindex1,
                        block1.getCoinbase(),
                        track1,
                        blockStore,
                        receiptStore,
                        blockFactory,
                        programInvokeFactory,
                        block1,
                        listener,
                        totalGasUsed1,
                        config.getVmConfig(),
                        config.getBlockchainConfig(),
                        config.playVM(),
                        config.isRemascEnabled(),
                        config.vmTrace(),
                        new PrecompiledContracts(config),
                        config.databaseDir(),
                        config.vmTraceDir(),
                        config.vmTraceCompressed()
                ), stateRootHandler, config.getActivationConfig()),
                stateRootHandler
        );

        blockchain.setNoValidation(true);

        Repository track = repository.startTracking();

        for (Map.Entry<RskAddress, AccountState> accountsEntry : genesis.getAccounts().entrySet()) {
            RskAddress accountAddress = accountsEntry.getKey();
            track.createAccount(accountAddress);
            track.addBalance(accountAddress, accountsEntry.getValue().getBalance());
        }

        track.commit();

        genesis.setStateRoot(repository.getRoot());
        genesis.flushRLP();

        blockStore.saveBlock(genesis, genesis.getCumulativeDifficulty(), true);
        blockchain.setStatus(genesis, genesis.getCumulativeDifficulty());

        return blockchain;
    }
}

