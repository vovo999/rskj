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

package org.ethereum.config.blockchain.upgrades;

public enum ConsensusRule {
    ARE_BRIDGE_TXS_PAID("areBridgeTxsPaid"),
    DIFFICULTY_DROP_ENABLED("difficultyDropEnabled"), // TODO remove after testnet reset
    RSKIP85("rskip85"),
    RSKIP87("rskip87"),
    RSKIP88("rskip88"),
    RSKIP89("rskip89"),
    RSKIP90("rskip90"),
    RSKIP91("rskip91"),
    RSKIP92("rskip92"),
    RSKIP93("rskip93"),
    RSKIP94("rskip94"),
    RSKIP97("rskip97"),
    RSKIP98("rskip98"),
    RSKIP103("rskip103"),
    RSKIP120("rskip120"),
    RSKIP123("rskip123"),
    RSKIP_UNITRIE("rskipunitrie");

    private String configKey;

    ConsensusRule(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static ConsensusRule fromConfigKey(String configKey) {
        for (ConsensusRule consensusRule : ConsensusRule.values()) {
            if (consensusRule.configKey.equals(configKey)) {
                return consensusRule;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown consensus rule %s", configKey));
    }
}
