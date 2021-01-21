package com.github.harbby.astarte.operator;

import com.github.harbby.astarte.TaskContext;
import com.github.harbby.astarte.api.Partition;
import com.github.harbby.astarte.api.function.Mapper;
import com.github.harbby.gadtry.base.Iterators;

import java.util.Iterator;

public class FlatMapIteratorOperator<IN, OUT>
        extends Operator<OUT>
{
    private final Mapper<IN, Iterator<OUT>> flatMapper;
    private final Operator<IN> dataSet;

    protected FlatMapIteratorOperator(Operator<IN> dataSet, Mapper<IN, Iterator<OUT>> flatMapper)
    {
        super(dataSet);
        this.flatMapper = flatMapper;
        this.dataSet = unboxing(dataSet);
    }

    @Override
    public Iterator<OUT> compute(Partition split, TaskContext taskContext)
    {
        return Iterators.concat(Iterators.map(dataSet.computeOrCache(split, taskContext),
                flatMapper::map));
    }
}
