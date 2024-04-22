package com.jsls.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.jsls.util.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
public class Progress {

    protected static final Logger logger = LoggerFactory.getLogger(Progress.class);

    private final Recorder recorder;
    private final AsyncTaskExecutor taskExecutor;

    public Progress(String taskName, int batchSize) {
        this(new Recorder(taskName, batchSize));
    }

    public Progress(Recorder recorder) {
        this(recorder, SpringContextHolder.getBean(AsyncTaskExecutor.class));
    }

    public Progress(Recorder recorder, AsyncTaskExecutor taskExecutor) {
        this.recorder = recorder;
        this.taskExecutor = taskExecutor;
    }

    @SuppressWarnings("unchecked")
    public <P extends Progress> P copy() {
        return (P) new Progress(recorder.copy(), taskExecutor);
    }

    /**
     * 生成数据迭代器
     * 
     * @param <T>
     * @param supplier
     * @return
     */
    public <T> Iterator<T> useIterator(Supplier<? extends Collection<T>> supplier) {
        return new Iterator<T>() {
            Iterator<T> internal;
            int batchNumber = recorder.getBatchNumber();

            @Override
            public boolean hasNext() {
                if (internal == null || !internal.hasNext()) {
                    if (internal == null) {
                        recorder.doStart();
                    }
                    recorder.batchNumber = batchNumber++;
                    Collection<T> batch = supplier.get();
                    if (CollectionUtils.isEmpty(batch)) {
                        recorder.doFinish();
                        return false;
                    } else {
                        recorder.applyBatch(batch.size(), 0);
                        recorder.doProgress();
                    }
                    internal = batch.iterator();
                    return internal.hasNext();
                }
                return true;
            }

            @Override
            public T next() {
                return internal.next();
            }
        };
    }

    /**
     * 生成数据迭代器
     * 
     * @param <T>
     * @param fetcher
     * @return
     */
    public <T> Iterator<T> useIterator(Function<? super Recorder, ? extends Collection<T>> fetcher) {
        return new Iterator<T>() {
            Iterator<T> internal;
            int batchNumber = recorder.getBatchNumber();

            @Override
            public boolean hasNext() {
                if (internal == null || !internal.hasNext()) {
                    if (internal == null) {
                        recorder.doStart();
                    }
                    recorder.batchNumber = batchNumber++;
                    Collection<T> batch = fetcher.apply(recorder);
                    if (CollectionUtils.isEmpty(batch)) {
                        recorder.doFinish();
                        return false;
                    } else {
                        recorder.applyBatch(batch.size(), 0);
                        recorder.doProgress();
                    }
                    internal = batch.iterator();
                    return internal.hasNext();
                }
                return true;
            }

            @Override
            public T next() {
                return internal.next();
            }
        };
    }

    /**
     * 生成数据迭代器封装以支持java for 循环
     * 
     * @param <T>
     * @param fetcher
     * @return
     */
    public <T> Iterable<T> useIterable(Function<? super Recorder, ? extends Collection<T>> fetcher) {
        Iterator<T> iterator = useIterator(fetcher);
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }

    /**
     * 生成数据迭代器封装以支持java for 循环
     * 
     * @param <T>
     * @param supplier
     * @return
     */
    public <T> Iterable<T> useIterable(Supplier<? extends Collection<T>> supplier) {
        Iterator<T> iterator = useIterator(supplier);
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }

    /**
     * 生成数据Collection封装以支持从模板分页导出
     * 
     * @param <T>
     * @param fetcher
     * @return
     */
    public <T> Collection<T> useCollection(Function<? super Recorder, ? extends Collection<T>> fetcher) {
        Iterator<T> iterator = useIterator(fetcher);
        return useCollection(iterator, recorder);
    }

    /**
     * 生成数据Collection封装以支持从模板分页导出
     * 
     * @param <T>
     * @param supplier
     * @return
     */
    public <T> Collection<T> useCollection(Supplier<? extends Collection<T>> supplier) {
        Iterator<T> iterator = useIterator(supplier);
        return useCollection(iterator, recorder);
    }

    public static <T> Collection<T> useCollection(Iterator<T> iterator, Recorder recorder) {
        return new Collection<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterator;
            }

            @Override
            public int size() {
                return Long.valueOf(recorder.getTotal()).intValue();
            }

            @Override
            public boolean isEmpty() {
                return size() <= 0;
            }

            @Override
            public boolean contains(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T2> T2[] toArray(T2[] a) {
                throw new UnsupportedOperationException();
            }

            public boolean add(T e) {
                throw new UnsupportedOperationException();
            }

            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            public boolean containsAll(Collection<?> coll) {
                throw new UnsupportedOperationException();
            }

            public boolean addAll(Collection<? extends T> coll) {
                throw new UnsupportedOperationException();
            }

            public boolean removeAll(Collection<?> coll) {
                throw new UnsupportedOperationException();
            }

            public boolean retainAll(Collection<?> coll) {
                throw new UnsupportedOperationException();
            }

            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * 
     * @param <T>
     * @param supplier
     * @param consumer
     * @param async    true 异步 false 同步
     */
    public <T> void processBatch(Supplier<? extends Collection<T>> supplier, Consumer<T> consumer, boolean async) {
        processBatch(supplier, batch -> executeBatch(batch, consumer, async));
    }

    public <T> void processBatch(Supplier<? extends Collection<T>> supplier,
            Consumer<? super Collection<T>> batchConsumer) {
        int batchNumber = recorder.getBatchNumber();
        recorder.doStart();
        for (;;) {
            recorder.batchNumber = batchNumber++;
            if (!execBatch(supplier.get(), batchConsumer)) {
                break;
            }
        }
        recorder.doFinish();
    }

    /**
     * 
     * @param <T>
     * @param fetcher
     * @param consumer
     * @param async    true 异步 false 同步
     */
    public <T> void processBatch(Function<? super Recorder, ? extends Collection<T>> fetcher, Consumer<T> consumer,
            boolean async) {
        processBatch(fetcher, batch -> executeBatch(batch, consumer, async));
    }

    public <T> void processBatch(Function<? super Recorder, ? extends Collection<T>> fetcher,
            Consumer<? super Collection<T>> batchConsumer) {
        int batchNumber = recorder.getBatchNumber();
        recorder.doStart();
        for (;;) {
            recorder.batchNumber = batchNumber++;
            if (!execBatch(fetcher.apply(recorder), batchConsumer)) {
                break;
            }
        }
        recorder.doFinish();
    }

    public <T> void processBatch(Collection<T> data, Consumer<? super Collection<T>> batchConsumer) {
        int batchNumber = recorder.getBatchNumber();
        recorder.setTotal(data.size());
        recorder.doStart();
        List<T> batch = new ArrayList<>();
        for (T t : data) {
            batch.add(t);
            if ((batch.size() % recorder.batchSize) == 0) {
                recorder.batchNumber = batchNumber++;
                boolean flag = execBatch(batch, batchConsumer);
                batch.clear();
                if (!flag) {
                    break;
                }
            }
        }
        if (!batch.isEmpty()) {
            recorder.batchNumber = batchNumber++;
            execBatch(batch, batchConsumer);
            batch.clear();
        }
        recorder.doFinish();
    }

    private <T> boolean execBatch(Collection<T> batch, Consumer<? super Collection<T>> batchConsumer) {
        if (CollectionUtils.isEmpty(batch)) {
            return false;
        }
        int batchCount = batch.size();
        long beforeFailCount = recorder.getFailCount();
        batchConsumer.accept(batch);
        recorder.doProgress();
        if (batchCount < recorder.batchSize) {
            return false;
        } else if (recorder.getFailCount() == beforeFailCount + batchCount) {
            recorder.doBreak();
            return false;
        }
        return true;
    }

    /**
     * 调用批处理
     * 
     * @param <T>
     * @param <R>
     * @param fn
     * @return
     */
    public <T, R> List<R> callBatch(Collection<T> batch, Function<T, R> fn, boolean async) {
        final int batchSize = recorder.getBatchSize();
        int batchCount = 0;
        int failCount = 0;
        List<R> resultList = new ArrayList<>();
        if (!async) {
            for (T item : batch) {
                resultList.add(fn.apply(item));
                batchCount++;
                if (batchCount % batchSize == 0) {
                    recorder.applyBatch(batchCount, failCount);
                    recorder.doProgress();
                    batchCount = 0;
                    failCount = 0;
                }
            }
        } else {
            List<Future<R>> list = new ArrayList<>();
            for (T item : batch) {
                list.add(taskExecutor.submit(() -> fn.apply(item)));
            }
            for (Future<R> future : list) {
                R r = null;
                try {
                    r = future.get();
                } catch (Exception e) {
                    failCount++;
                    recorder.log(e);
                }
                resultList.add(r);
                batchCount++;
                if (batchCount % batchSize == 0) {
                    recorder.applyBatch(batchCount, failCount);
                    recorder.doProgress();
                    batchCount = 0;
                    failCount = 0;
                }
            }
        }
        if (batchCount > 0) {
            recorder.applyBatch(batchCount, failCount);
            recorder.doProgress();
            batchCount = 0;
            failCount = 0;
        }
        return resultList;
    }

    /**
     * 调用批处理
     * 
     * @param <T>
     * @param batch
     * @param consumer
     * @param taskExecutor
     * @return
     */
    public <T extends Parallelizable, R> List<R> callBatch(
            Collection<T> batch,
            Parallelizable.ParallelStrategy<T, R> fn) {
        final int batchSize = recorder.getBatchSize();
        int batchCount = 0;
        int failCount = 0;
        List<Future<R>> list = new ArrayList<>();
        for (T item : batch) {
            list.add(taskExecutor.submit(() -> fn.wrapper(item)));
        }
        List<R> resultList = new ArrayList<>();
        for (Future<R> future : list) {
            R r = null;
            try {
                r = future.get();
            } catch (Exception e) {
                failCount++;
                logger.error(e.getMessage(), e);
            }
            resultList.add(r);
            batchCount++;
            if (batchCount % batchSize == 0) {
                recorder.applyBatch(batchCount, failCount);
                batchCount = 0;
                failCount = 0;
            }
        }
        if (batchCount > 0) {
            recorder.applyBatch(batchCount, failCount);
            batchCount = 0;
            failCount = 0;
        }
        fn.onBatchEnd();
        return resultList;
    }

    /**
     * 执行批处理
     * 
     * @param <T>
     * @param batch
     * @param consumer
     * @return
     */
    public <T> void executeBatch(Collection<T> batch, Consumer<T> consumer, boolean async) {
        final int batchSize = recorder.getBatchSize();
        int batchCount = 0;
        int failCount = 0;
        if (!async) {
            for (T item : batch) {
                consumer.accept(item);
                batchCount++;
                if (batchCount % batchSize == 0) {
                    recorder.applyBatch(batchCount, failCount);
                    recorder.doProgress();
                    batchCount = 0;
                    failCount = 0;
                }
            }
        } else {
            List<Future<?>> list = new ArrayList<>();
            for (T item : batch) {
                list.add(taskExecutor.submit(() -> consumer.accept(item)));
            }
            for (Future<?> future : list) {
                try {
                    future.get();
                } catch (Exception e) {
                    failCount++;
                    recorder.log(e);
                }
                batchCount++;
                if (batchCount % batchSize == 0) {
                    recorder.applyBatch(batchCount, failCount);
                    recorder.doProgress();
                    batchCount = 0;
                    failCount = 0;
                }
            }
        }
        if (batchCount > 0) {
            recorder.applyBatch(batchCount, failCount);
            recorder.doProgress();
            batchCount = 0;
            failCount = 0;
        }
    }

    /**
     * 执行批处理
     * 
     * @param <T>
     * @param batch
     * @param consumer
     * @return
     */
    public <T extends Parallelizable> void executeBatch(Collection<T> batch,
            Parallelizable.ParallelStrategy<T, Boolean> consumer) {
        final int batchSize = recorder.getBatchSize();
        int batchCount = 0;
        int failCount = 0;
        List<Future<Boolean>> list = new ArrayList<>();
        for (T item : batch) {
            list.add(taskExecutor.submit(() -> consumer.wrapper(item)));
        }
        for (Future<Boolean> future : list) {
            try {
                future.get();
            } catch (Exception e) {
                failCount++;
                logger.error(e.getMessage(), e);
            }
            batchCount++;
            if (batchCount % batchSize == 0) {
                recorder.applyBatch(batchCount, failCount);
                recorder.doProgress();
                batchCount = 0;
                failCount = 0;
            }
        }
        if (batchCount > 0) {
            recorder.applyBatch(batchCount, failCount);
            recorder.doProgress();
            batchCount = 0;
            failCount = 0;
        }
        consumer.onBatchEnd();
    }

    @Getter
    public static class Recorder {
        private final String taskName;
        private int batchSize;
        private int batchNumber = 1;
        private long total;
        private long count;
        private long failCount;
        private long skipCount;
        private String stage;

        public Recorder(String taskName, int batchSize) {
            this.taskName = taskName;
            this.batchSize = batchSize;
        }

        public Progress useProgress() {
            return new Progress(this);
        }

        public Progress useProgress(AsyncTaskExecutor taskExecutor) {
            return new Progress(this, taskExecutor);
        }

        public Recorder copy() {
            return new Recorder(taskName, batchSize);
        }

        public void applyBatch(int batchCount, int failCount) {
            this.count += batchCount;
            this.failCount += failCount;
        }

        public void applyBatch(int batchCount, int failCount, int skipCount) {
            this.count += batchCount;
            this.failCount += failCount;
            this.skipCount += skipCount;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getSuccessCount() {
            return count - failCount - skipCount;
        }

        public void reset() {
            reset(null);
        }

        public void reset(String stage) {
            this.batchNumber = 1;
            this.total = 0;
            this.count = 0;
            this.failCount = 0;
            this.skipCount = 0;
            this.stage = stage;
        }

        public String useTaskName() {
            if (StringUtils.hasText(stage)) {
                return taskName + "-" + stage;
            }
            return taskName;
        }

        public void doStart() {
            log("{} start", useTaskName());
        }

        public void doFinish() {
            log("{} finish", useTaskName());
        }

        public void doBreak() {
            log("{} break", useTaskName());
        }

        public void doProgress() {
            if (total > 0) {
                doProgress(total);
                return;
            }
            if (skipCount > 0) {
                log("{} 已执行:{},失败:{},忽略:{}", useTaskName(), count, failCount, skipCount);
                return;
            }
            log("{} 已执行:{},失败:{}", useTaskName(), count, failCount);
        }

        public void doProgress(long total) {
            if (skipCount > 0) {
                log("{} 总数量:{},已执行:{},失败:{},忽略:{}", useTaskName(), total, count, failCount, skipCount);
                return;
            }
            log("{} 总数量:{},已执行:{},失败:{}", useTaskName(), total, count, failCount);
        }

        public void doProgress(String step) {
            if (total > 0) {
                doProgress(total, step);
                return;
            }
            if (skipCount > 0) {
                log("{} {} 已执行:{},失败:{},忽略:{}", useTaskName(), step, count, failCount, skipCount);
                return;
            }
            log("{} {} 已执行:{},失败:{}", useTaskName(), step, count, failCount);
        }

        public void doProgress(long total, String step) {
            if (skipCount > 0) {
                log("{} {} 总数量:{},已执行:{},失败:{},忽略:{}", useTaskName(), step, total, count, failCount, skipCount);
                return;
            }
            log("{} {} 总数量:{},已执行:{},失败:{}", useTaskName(), step, total, count, failCount);
        }

        public void doBatchStep(String step, int batchCount, int batchFailCount, int batchSkipCount) {
            if (total > 0) {
                doBatchStep(total, step, batchCount, batchFailCount, batchSkipCount);
                return;
            }
            log("{} {} 执行:{}-{},失败:{},忽略:{}", useTaskName(), step, this.count + 1,
                    this.count + batchCount, batchFailCount, batchSkipCount);
        }

        public void doBatchStep(String step, int batchCount, int batchFailCount) {
            if (total > 0) {
                doBatchStep(total, step, batchCount, batchFailCount);
                return;
            }
            log("{} {} 执行:{}-{},失败:{}", useTaskName(), step, this.count + 1, this.count + batchCount, batchFailCount);
        }

        public void doBatchStep(long total, String step, int batchCount, int batchFailCount, int batchSkipCount) {
            log("{} {} 总数量:{},执行:{}-{},失败:{},忽略:{}", useTaskName(), step, total,
                    this.count + 1, this.count + batchCount, batchFailCount, batchSkipCount);
        }

        public void doBatchStep(long total, String step, int batchCount, int batchFailCount) {
            log("{} {} 总数量:{},执行:{}-{},失败:{}", useTaskName(), step, total,
                    this.count + 1, this.count + batchCount, batchFailCount);
        }

        public void log(String format, Object... arguments) {
            logger.info(format, arguments);
        }

        public void log(Throwable e) {
            logger.error(useTaskName() + "异常:" + e.getMessage(), e);
        }
    }

    public static class DefaultFunctionStrategy<T extends Parallelizable, R>
            implements Parallelizable.ParallelStrategy<T, R> {
        private final Function<T, R> fn;
        private final ConcurrentHashMap<String, T> mutex = new ConcurrentHashMap<>();
        private final boolean skipWhenMutex;

        private DefaultFunctionStrategy(Function<T, R> fn, boolean skipWhenMutex) {
            this.fn = fn;
            this.skipWhenMutex = skipWhenMutex;
        }

        public R wrapper(T item) {
            String key = item.useKey();
            for (;;) {
                T pre = mutex.putIfAbsent(key, item);
                if (pre == null) {
                    R r = null;
                    try {
                        r = fn.apply(item);
                        mutex.remove(key);
                        item.complete(r);
                    } catch (Throwable e) {
                        mutex.remove(key);
                        item.complete(e);
                        throw e;
                    }
                    return r;
                } else if (skipWhenMutex) {
                    item.complete(null);
                    return null;
                }
                if (mutex.replace(key, pre, item)) {
                    pre.acquire();
                }
            }
        }

        public void onBatchEnd() {
            if (skipWhenMutex) {
                mutex.clear();
            } else {
                for (; !mutex.isEmpty();) {
                    Set<String> keys = new HashSet<>(mutex.keySet());
                    for (String key : keys) {
                        T item = mutex.remove(key);
                        if (item != null) {
                            item.notifyAll();
                        }
                    }
                }
            }
        }
    }

    public static class DefaultConsumerStrategy<T extends Parallelizable>
            implements Parallelizable.ParallelStrategy<T, Boolean> {
        private final Consumer<T> consumer;
        private final ConcurrentHashMap<String, T> mutex = new ConcurrentHashMap<>();
        private final boolean skipWhenMutex;

        private DefaultConsumerStrategy(Consumer<T> consumer, boolean skipWhenMutex) {
            this.consumer = consumer;
            this.skipWhenMutex = skipWhenMutex;
        }

        public Boolean wrapper(T item) {
            String key = item.useKey();
            for (;;) {
                T pre = mutex.putIfAbsent(key, item);
                if (pre == null) {
                    try {
                        consumer.accept(item);
                        mutex.remove(key);
                        item.complete(true);
                    } catch (Throwable e) {
                        mutex.remove(key);
                        item.complete(e);
                        throw e;
                    }
                    return true;
                } else if (skipWhenMutex) {
                    item.complete(null);
                    return true;
                }
                if (mutex.replace(key, pre, item)) {
                    pre.acquire();
                }
            }
        }

        public void onBatchEnd() {
            if (skipWhenMutex) {
                mutex.clear();
            } else {
                for (; !mutex.isEmpty();) {
                    Set<String> keys = new HashSet<>(mutex.keySet());
                    for (String key : keys) {
                        T item = mutex.remove(key);
                        if (item != null) {
                            item.notifyAll();
                        }
                    }
                }
            }
        }
    }

    public static abstract class Parallelizable {

        public static interface ParallelStrategy<T extends Parallelizable, R> {
            R wrapper(T item);

            void onBatchEnd();

            public static <T extends Parallelizable> ParallelStrategy<T, Boolean> of(Consumer<T> consumer,
                    boolean skipWhenMutex) {
                return new DefaultConsumerStrategy<>(consumer, skipWhenMutex);
            }

            public static <T extends Parallelizable, R> ParallelStrategy<T, R> of(Function<T, R> fn,
                    boolean skipWhenMutex) {
                return new DefaultFunctionStrategy<T, R>(fn, skipWhenMutex);
            }
        }

        private AtomicReference<Object> atomicReference;

        public synchronized void complete(Object r) {
            atomicReference = new AtomicReference<Object>(r);
            this.notifyAll();
        }

        public synchronized void acquire() {
            if (atomicReference == null)
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }

        public abstract String useKey();
    }
}