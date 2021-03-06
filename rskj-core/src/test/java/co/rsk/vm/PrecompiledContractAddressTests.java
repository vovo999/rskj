package co.rsk.vm;
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

import co.rsk.config.TestSystemProperties;
import co.rsk.core.RskAddress;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Sergio Demian Lerner on 12/10/2018.
 */
public class PrecompiledContractAddressTests {

    public static final String ECRECOVER_ADDR = "0000000000000000000000000000000000000001";
    public static final String SHA256_ADDR = "0000000000000000000000000000000000000002";
    public static final String RIPEMPD160_ADDR = "0000000000000000000000000000000000000003";
    public static final String IDENTITY_ADDR_STR = "0000000000000000000000000000000000000004";
    public static final String BIG_INT_MODEXP_ADDR = "0000000000000000000000000000000000000005";
    public static final String SAMPLE_ADDR_STR = "0000000000000000000000000000000001000005";
    public static final String BRIDGE_ADDR_STR = "0000000000000000000000000000000001000006";
    public static final String REMASC_ADDR_STR = "0000000000000000000000000000000001000008";

    private final TestSystemProperties config = new TestSystemProperties();

    @Test
    public void testGetPrecompile() {
        PrecompiledContracts pcList = new PrecompiledContracts(config);
        checkAddr(pcList,ECRECOVER_ADDR, "ECRecover");
        checkAddr(pcList,SHA256_ADDR, "Sha256");
        checkAddr(pcList,RIPEMPD160_ADDR ,"Ripempd160");
        checkAddr(pcList,IDENTITY_ADDR_STR ,"Identity");
        checkAddr(pcList,BIG_INT_MODEXP_ADDR ,"BigIntegerModexp");
        //checkAddr(pcList,SAMPLE_ADDR_STR ,"SamplePrecompiledContract");
        checkAddr(pcList,BRIDGE_ADDR_STR ,"Bridge");
        checkAddr(pcList,REMASC_ADDR_STR ,"RemascContract");
    }

    void checkAddr(PrecompiledContracts pcList,String addr,String className) {
        RskAddress a;
        a = new RskAddress(addr);
        PrecompiledContracts.PrecompiledContract pc = pcList.getContractForAddress(null, DataWord.valueOf(a.getBytes()));
        Assert.assertEquals(className,pc.getClass().getSimpleName());
    }
}
