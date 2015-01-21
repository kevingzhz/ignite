/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.grid.tests.p2p;

import org.apache.ignite.lang.*;
import org.apache.ignite.resources.*;

import java.util.*;

/**
 * Closure that returns cache query reducer, which uses
 * {@link GridP2PAwareTestUserResource}.
 */
public class GridExternalCacheQueryReducerClosure implements IgniteReducer<Map.Entry<Integer, Integer>, Integer> {
    /** {@inheritDoc} */
    @Override public boolean collect(Map.Entry<Integer, Integer> e) {
        return true;
    }

    /** {@inheritDoc} */
    @Override public Integer reduce() {
        return 0;
    }
}
