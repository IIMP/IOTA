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
package com.iota.iri.vm.program.invoke;

// import org.ethereum.core.Block;
import com.iota.iri.vm.Repository;
import com.iota.iri.vm.EVMTransaction;
// import org.ethereum.db.BlockStore;
import com.iota.iri.vm.vm.DataWord;
import com.iota.iri.vm.program.Program;

import java.math.BigInteger;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public interface ProgramInvokeFactory {

    ProgramInvoke createProgramInvoke(EVMTransaction tx,
                                      Repository repository, Repository origRepository);

    ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                             DataWord inValue,
                                             BigInteger balanceInt, byte[] dataIn,
                                             Repository repository, Repository origRepository, 
                                            boolean staticCall, boolean byTestingSuite);


}
