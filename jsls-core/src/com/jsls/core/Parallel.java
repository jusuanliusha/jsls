package com.jsls.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.jsls.util.SpringContextHolder;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.CollectionUtils;

public class Parallel {
    private static final AsyncTaskExecutor taskExecutor =SpringContextHolder.getBean(AsyncTaskExecutor.class);
    public static void exec(Runnable task){
        taskExecutor.execute(task);
    }
    public static  Spliter newSpliter(){
        return new Spliter(null);
    }
    public  static class Spliter implements Hierarchical<Spliter>{
        private final Spliter parent;
        private final List<Future<?>> futureList=new ArrayList<>();
        private final List<Spliter> subSpliters=new ArrayList<>();
        private Spliter(Spliter parent){
            this.parent=parent;
        }
        @Override
        public Spliter getParent() {
            return parent;
        }
        
        public Spliter sub(){
            Spliter sub=new Spliter(this);
            synchronized(subSpliters){
                subSpliters.add(sub);
            }
            return sub;
        }
        public synchronized <V> Future<V> call(Callable<V> task){
            Future<V> temp=taskExecutor.submit(task);
            futureList.add(temp);
            return temp;
        }
        public <V> Future<Void> call(Callable<V> task,Consumer<V> consumer){
            Spliter mutex=this;
            return call(()->{
                V v=task.call();
                synchronized (mutex){
                    consumer.accept(v);
                }
                return null;
            });
        }
        
        public synchronized Future<Void> callRsultList(Consumer<List<Object>> consumer){
            List<Future<?>> curr=new ArrayList<>(futureList);
            futureList.clear();
            return call(()->{
                return Parallel.getRsultList(curr);
            },consumer);
        }
        public synchronized List<Object> getRsultList(){
            List<Object> temp=Parallel.getRsultList(futureList);
            futureList.clear();
            return temp;
        }
        public boolean waitComplete(){
            int size=getRsultList().size();
            synchronized(subSpliters){
                if(!CollectionUtils.isEmpty(subSpliters)){
                    for(Spliter sub:subSpliters){
                        sub.waitComplete();
                    }
                }
            }
            return size>0;
        }
    }
    public static List<Object> getRsultList(List<Future<?>> futureList){
        List<Object>  rl=new ArrayList<>();
        for(Future<?> item:futureList){
            try{
                rl.add(item.get());
            }catch(Throwable e){
                throw new RuntimeException("子任务异常："+e.getMessage(),e);
            }
        }
        return rl;
    }
}