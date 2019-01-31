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

package co.rsk.db;

import co.rsk.core.bc.BlockResult;
import co.rsk.crypto.Keccak256;
import co.rsk.trie.Trie;
import co.rsk.trie.TrieConverter;
import org.ethereum.config.blockchain.upgrades.ActivationConfig;
import org.ethereum.config.blockchain.upgrades.ConsensusRule;
import org.ethereum.core.BlockHeader;
import org.ethereum.datasource.KeyValueDataSource;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class StateRootHandler {
    private final ActivationConfig activationConfig;
    private final TrieConverter trieConverter;
    private final StateRootTranslator stateRootTranslator;

    public StateRootHandler(
            ActivationConfig activationConfig,
            TrieConverter trieConverter,
            KeyValueDataSource stateRootDB,
            Map<Keccak256, Keccak256> stateRootCache) {
        this.activationConfig = activationConfig;
        this.trieConverter = trieConverter;
        this.stateRootTranslator = new StateRootTranslator(stateRootDB, stateRootCache);
    }

    public Keccak256 translate(BlockHeader block) {
        boolean isRskipUnitrieEnabled = activationConfig.isActive(ConsensusRule.RSKIP_UNITRIE, block.getNumber());
        Keccak256 blockStateRoot = new Keccak256(block.getStateRoot());
        if (isRskipUnitrieEnabled) {
            return blockStateRoot;
        }

        return Objects.requireNonNull(
                stateRootTranslator.get(blockStateRoot),
                "Reset database or continue syncing with previous version"
        );
    }

    public Keccak256 convert(BlockHeader minedBlock, Trie executionResult) {
        boolean isRskipUnitrieEnabled = activationConfig.isActive(ConsensusRule.RSKIP_UNITRIE, minedBlock.getNumber());
        if (isRskipUnitrieEnabled) {
            return executionResult.getHash();
        }

        //we shouldn't be converting blocks before orchid in stable networks
        return new Keccak256(trieConverter.getOrchidAccountTrieRoot(executionResult));
    }

    public void register(BlockHeader executedBlock, Trie executionResult) {
        boolean isRskipUnitrieEnabled = activationConfig.isActive(ConsensusRule.RSKIP_UNITRIE, executedBlock.getNumber());
        if (isRskipUnitrieEnabled) {
            return;
        }

        if (executedBlock.isGenesis()) {
            Keccak256 genesisStateRoot = convert(executedBlock, executionResult);
            stateRootTranslator.put(genesisStateRoot, executionResult.getHash());
        } else {
            boolean isRskip85Enabled = activationConfig.isActive(ConsensusRule.RSKIP85, executedBlock.getNumber());
            if (isRskip85Enabled) {
                Keccak256 orchidStateRoot = convert(executedBlock, executionResult);
                stateRootTranslator.put(orchidStateRoot, executionResult.getHash());
            } else {
                Keccak256 blockStateRoot = new Keccak256(executedBlock.getStateRoot());
                stateRootTranslator.put(blockStateRoot, executionResult.getHash());
            }
        }
    }

    public boolean validate(BlockHeader block, BlockResult result) {
        boolean isRskip85Enabled = activationConfig.isActive(ConsensusRule.RSKIP85, block.getNumber());
        if (!isRskip85Enabled) {
            return true;
        }

        boolean isRskipUnitrieEnabled = activationConfig.isActive(ConsensusRule.RSKIP_UNITRIE, block.getNumber());
        if (!isRskipUnitrieEnabled) {
            byte[] orchidStateRoot = trieConverter.getOrchidAccountTrieRoot(result.getFinalState());
            return Arrays.equals(orchidStateRoot, block.getStateRoot());
        }

        // we only validate state roots of blocks newer than 0.5.0 activation
        return Arrays.equals(result.getStateRoot(), block.getStateRoot());
    }
}
