package codepig.ideal.mppwhater;

import codepig.ideal.mppwhater.api.Partition;
import codepig.ideal.mppwhater.api.function.Foreach;
import codepig.ideal.mppwhater.api.operator.AbstractDataSet;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Local achieve
 */
public class LocalMppContext
        implements MppContext
{
    @Override
    public <E> List<E> collect(AbstractDataSet<E> dataSet)
    {
        Partition[] partitions = dataSet.getPartitions();
        ExecutorService executors = Executors.newFixedThreadPool(partitions.length);
        try {
            return Stream.of(partitions).parallel().map(partition -> CompletableFuture.supplyAsync(() -> {
                Iterator<E> iterator = dataSet.compute(partition);
                return ImmutableList.copyOf(iterator);
            }, executors)).flatMap(x -> x.join().stream())
                    .collect(Collectors.toList());
        }
        finally {
            executors.shutdown();
        }
    }

    @Override
    public <E> void execJob(AbstractDataSet<E> dataSet, Foreach<Iterator<E>> partitionForeach)
    {
        Partition[] partitions = dataSet.getPartitions();
        ExecutorService executors = Executors.newFixedThreadPool(partitions.length);
        try {
            Stream.of(partitions).parallel().map(partition -> CompletableFuture.runAsync(() -> {
                Iterator<E> iterator = dataSet.compute(partition);
                partitionForeach.apply(iterator);
            }, executors)).forEach(CompletableFuture::join);
        }
        finally {
            executors.shutdown();
        }
    }
}
