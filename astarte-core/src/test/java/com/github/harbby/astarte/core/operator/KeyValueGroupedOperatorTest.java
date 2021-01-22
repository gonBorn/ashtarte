/*
 * Copyright (C) 2018 The Astarte Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.harbby.astarte.core.operator;

import com.github.harbby.astarte.core.BatchContext;
import com.github.harbby.astarte.core.api.DataSet;
import com.github.harbby.gadtry.collection.MutableMap;
import com.github.harbby.gadtry.collection.tuple.Tuple2;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyValueGroupedOperatorTest
{
    private final BatchContext mppContext = BatchContext.builder()
            .local(2)
            .getOrCreate();

    @Test
    public void keyGroupedTest()
    {
        DataSet<String> ds = mppContext.makeDataSet(Arrays.asList(
                "a",
                "a",
                "b",
                "b",
                "b"), 2);

        Map<Integer, String> result = ds.groupByKey(x -> x.charAt(0) % 2)
                .<StringBuilder>partitionGroupsWithState(keyGroupState -> (record) -> {
                    if (keyGroupState.getState() == null) {
                        keyGroupState.update(new StringBuilder(keyGroupState.getKey()));
                    }
                    StringBuilder builder = keyGroupState.getState();
                    builder.append(record);
                }).collect().stream().collect(Collectors.toMap(k -> k.f1, v -> v.f2.toString()));
        Assert.assertEquals(result, MutableMap.of(0, "bbb", 1, "aa"));
    }

    @Test
    public void keyByMapPartitionTest()
            throws IOException
    {
        DataSet<String> ds = mppContext.makeDataSet(Arrays.asList(
                "a",
                "a",
                "b",
                "b",
                "b"), 2);

        DataSet<Tuple2<Integer, StringBuilder>> dataSet = ds.groupByKey(x -> x.charAt(0) % 2)
                .mapPartition(partition -> {
                    Map<Integer, StringBuilder> keyGroup = new HashMap<>();
                    while (partition.hasNext()) {
                        Tuple2<Integer, String> row = partition.next();
                        keyGroup.computeIfAbsent(row.f1, k -> new StringBuilder()).append(row.f2);
                    }
                    return keyGroup.entrySet().stream().map(x -> new Tuple2<>(x.getKey(), x.getValue())).iterator();
                });
        Map<Integer, String> result = dataSet.collect().stream().collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue().toString()));
        Assert.assertEquals(result, MutableMap.of(0, "bbb", 1, "aa"));
    }
}
