/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.tx;

import com.hazelcast.map.TransactionItem;
import com.hazelcast.map.TransactionKey;
import com.hazelcast.nio.IOUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;

import java.io.IOException;

public class TxReplaceIfSameOperation extends BaseTxPutOperation {
    transient boolean same = false;
    Data testValue;

    public TxReplaceIfSameOperation() {
    }

    public TxReplaceIfSameOperation(String name, Data dataKey, Data testValue, Data dataValue) {
        super(name, dataKey, dataValue);
        this.testValue = testValue;
    }

    protected void innerProcess() {
        TransactionItem transactionItem = partitionContainer.getTransactionItem(new TransactionKey(getTransactionId(), name, dataKey));
        if (transactionItem != null) {
            same = mapService.compare(name, testValue, recordStore.get(dataKey));
        } else {
            same = mapService.compare(name, recordStore.get(dataKey), testValue);
        }
        if (same) {
            partitionContainer.addTransactionItem(new TransactionItem(getTransactionId(), name, getKey(), getValue(), false));
        }
    }

    protected void innerOnCommit() {
        if (same) {
            partitionContainer.removeTransactionItem(new TransactionKey(getTransactionId(), name, dataKey));
        }
        same = recordStore.replace(dataKey, testValue, dataValue);
    }

    protected void innerOnRollback() {
        if (same) {
            partitionContainer.removeTransactionItem(new TransactionKey(getTransactionId(), name, dataKey));
        }
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        IOUtil.writeNullableData(out, testValue);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        testValue = IOUtil.readNullableData(in);
    }

    @Override
    public Object getResponse() {
        return same;
    }

    @Override
    public String toString() {
        return "TxReplaceIfSameOperation{" + name + "}";
    }

}
