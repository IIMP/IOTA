/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.iota.iri.vm;

import com.iota.iri.vm.config.BlockchainConfig;
import com.iota.iri.vm.config.SystemProperties;
import org.ethereum.crypto.HashUtil;
import com.iota.iri.vm.util.ByteUtil;
import com.iota.iri.vm.util.FastByteComparisons;
import com.iota.iri.vm.util.RLP;
import com.iota.iri.vm.util.RLPList;

import java.math.BigInteger;

import javax.xml.bind.DatatypeConverter;

import static org.ethereum.crypto.HashUtil.*;
import static com.iota.iri.vm.util.FastByteComparisons.equal;
import static com.iota.iri.vm.util.ByteUtil.toHexString;

public class AccountMap {

    private byte[] EVMAddr;
    private byte[] rlpEncoded;
    /* A value equal to the number of transactions sent
     * from this address, or, in the case of contract accounts,
     * the number of contract-creations made by this account */
    private final BigInteger nonce;

    public AccountMap(byte[] addr, BigInteger non, boolean isRLP){
        // System.out.println("AccountMap "+DatatypeConverter.printHexBinary(addr)+" to be created");
        if(isRLP){
            this.rlpEncoded = addr;
            RLPList items = (RLPList) RLP.decode2(rlpEncoded).get(0);
            this.nonce = ByteUtil.bytesToBigInteger(items.get(0).getRLPData());
            this.EVMAddr = items.get(1).getRLPData();
        }
        else{
        this.EVMAddr = addr;
        this.nonce = non;
        }
        // System.out.println("AccountMap "+DatatypeConverter.printHexBinary(EVMAddr)+" created");
    }
    public AccountMap(byte[] addr, BigInteger non){
        this(addr, non, false);
    }
    public AccountMap(byte[] addr){
        this(addr,BigInteger.ZERO,false);
    }


    public byte[] getEVMAddr(){
        return EVMAddr;
    }
    public BigInteger getNonce() {
        return nonce;
    }
    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[] nonce = RLP.encodeBigInteger(this.nonce);
            byte[] evmAddr = RLP.encodeElement(this.EVMAddr);
            this.rlpEncoded = RLP.encodeList(nonce, evmAddr);
        }
        return rlpEncoded;
    }
}
