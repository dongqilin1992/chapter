package com.smart.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: dongql
 * @date: 2018/3/15 14:03
 */
public class TestCuratorLock {
    public static void main(String[] args) throws InterruptedException {
        // TODO Auto-generated method stub
        CountDownLatch latch = new CountDownLatch(5);
        String zookeeperConnectionString = "localhost:2181";
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                zookeeperConnectionString, retryPolicy);
        client.start();
        System.out.println("客户端启动。。。。");
        ExecutorService exec = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            exec.submit(new MyLock("client" + i, client, latch));
        }
        exec.shutdown();
        latch.await();
        System.out.println("所有任务执行完毕");
        client.close();
        System.out.println("客户端关闭。。。。");
    }
    static class MyLock implements Runnable {
        private String name;
        private CuratorFramework client;
        private CountDownLatch latch;
        public MyLock(String name, CuratorFramework client, CountDownLatch latch) {
            this.name = name;
            this.client = client;
            this.latch = latch;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public void run() {
            // TODO Auto-generated method stub
            InterProcessMutex lock = new InterProcessMutex(client,
                    "/test_group");
            try {
                if (lock.acquire(120, TimeUnit.SECONDS)) {
                    try {
                        // do some work inside of the critical section here
                        System.out.println("----------" + this.name
                                + "获得资源----------");

                        latch.countDown();
                    } finally {
                        lock.release();
                        System.out.println("----------" + this.name
                                + "释放----------");
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
