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

package co.rsk.core.bc;

import co.rsk.config.RskSystemProperties;
import co.rsk.db.StateRootHandler;
import org.ethereum.core.BlockFactory;
import org.ethereum.core.Repository;
import org.ethereum.core.TransactionExecutor;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.ethereum.listener.EthereumListener;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.trace.ProgramTraceProcessor;

/**
 * Created by ajlopez on 23/04/2019.
 */
public class BlockExecutorFactory {
    private final RskSystemProperties config;
    private final Repository repository;
    private final StateRootHandler stateRootHandler;
    private final BlockStore blockStore;
    private final ReceiptStore receiptStore;
    private final ProgramInvokeFactory programInvokeFactory;
    private final EthereumListener ethereumListener;
    private final BlockFactory blockFactory;

    public BlockExecutorFactory(RskSystemProperties config,
            Repository repository,
            BlockStore blockStore,
            ReceiptStore receiptStore,
            ProgramInvokeFactory programInvokeFactory,
            EthereumListener ethereumListener,
            StateRootHandler stateRootHandler,
            BlockFactory blockFactory) {
        this.config = config;
        this.repository = repository;
        this.blockStore = blockStore;
        this.receiptStore = receiptStore;
        this.programInvokeFactory = programInvokeFactory;
        this.ethereumListener = ethereumListener;
        this.stateRootHandler = stateRootHandler;
        this.blockFactory = blockFactory;
    }

    public BlockExecutor newInstance(ProgramTraceProcessor programTraceProcessor) {
        return new BlockExecutor(
                repository,
                (tx1, txindex1, coinbase, track1, block1, totalGasUsed1) -> new TransactionExecutor(
                        tx1,
                        txindex1,
                        block1.getCoinbase(),
                        track1,
                        blockStore,
                        receiptStore,
                        blockFactory,
                        programInvokeFactory,
                        block1,
                        ethereumListener,
                        totalGasUsed1,
                        config.getVmConfigWithTraceOn(),
                        config.getBlockchainConfig(),
                        config.playVM(),
                        config.isRemascEnabled(),
                        new PrecompiledContracts(config),
                        programTraceProcessor
                ),
                stateRootHandler
        );
    }
}
