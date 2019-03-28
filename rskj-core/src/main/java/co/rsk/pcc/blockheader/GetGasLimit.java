/*
 * This file is part of RskJ
 * Copyright (C) 2019 RSK Labs Ltd.
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

package co.rsk.pcc.blockheader;

import co.rsk.pcc.ExecutionEnvironment;
import org.ethereum.core.Block;
import org.ethereum.core.CallTransaction;

/**
 * This implements the "getGasLimit" method
 * that belongs to the BlockHeaderContract native contract.
 *
 * @author Diego Masini
 */
public class GetGasLimit extends BlockHeaderContractMethod {
    private final CallTransaction.Function function = CallTransaction.Function.fromSignature(
            "getGasLimit",
            new String[]{"int256"},
            new String[]{"bytes"}
    );

    public GetGasLimit(ExecutionEnvironment executionEnvironment, BlockAccessor blockAccessor) {
        super(executionEnvironment, blockAccessor);
    }

    @Override
    public CallTransaction.Function getFunction() {
        return function;
    }

    @Override
    protected Object internalExecute(Block block, Object[] arguments) {
        return block.getGasLimit();
    }
}

