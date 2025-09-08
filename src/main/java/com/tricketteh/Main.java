package com.tricketteh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        final int initialAccounts = 4;
        final int initialMoneyPerAccount = 10000;
        final int workerThreads = 3;
        final int maxTransactions = 30;

        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < initialAccounts; i++) {
            String id = UUID.randomUUID().toString();
            accounts.add(new Account(id, initialMoneyPerAccount));
        }

        int initialTotal = initialAccounts * initialMoneyPerAccount;
        logger.info("Initialized {} accounts, each with {} money. Total={}", initialAccounts, initialMoneyPerAccount, initialTotal);

        AtomicInteger txCounter = new AtomicInteger(0);
        AccountService service = new AccountService(accounts, txCounter, maxTransactions);

        ExecutorService executor = Executors.newFixedThreadPool(workerThreads);

        Runnable worker = () -> {
            Random rand = new Random();
            while (txCounter.get() < maxTransactions) {
                try {
                    int sleep = 1000 + rand.nextInt(1001);
                    Thread.sleep(sleep);
                    boolean ok = service.performRandomTransfer();
                    if (!ok) break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Worker interrupted");
                    break;
                } catch (Exception e) {
                    logger.error("Unexpected error in worker", e);
                }
            }
        };

        for (int i = 0; i < workerThreads; i++) {
            executor.submit(worker);
        }

        while (txCounter.get() < maxTransactions) {
            Thread.sleep(200);
        }

        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.warn("Executor didn't terminate timely.");
        }

        logger.info("Final transaction count: {}", txCounter.get());
        for (Account a : accounts) {
            logger.info("Final balance: {}", a);
        }

        int finalTotal = service.totalMoney();
        logger.info("Initial total = {}, Final total = {}", initialTotal, finalTotal);
        if (initialTotal != finalTotal) {
            logger.error("Total money mismatch! Data corruption detected.");
        } else {
            logger.info("Total money preserved.");
        }

        logger.info("Application finished.");
    }
}