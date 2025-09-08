package com.tricketteh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class AccountService {
    private static final Logger logger = LogManager.getLogger(AccountService.class);
    private final List<Account> accounts;
    private final AtomicInteger transactionCount;
    private final int maxTransactions;
    private final Random random = new Random();

    public AccountService(List<Account> accounts, AtomicInteger transactionCount, int maxTransactions) {
        this.accounts = accounts;
        this.transactionCount = transactionCount;
        this.maxTransactions = maxTransactions;
    }

    public boolean performRandomTransfer() {
        if (transactionCount.get() >= maxTransactions) return false;

        int fromIndex = random.nextInt(accounts.size());
        int toIndex;
        do {
            toIndex = random.nextInt(accounts.size());
        } while (toIndex == fromIndex);

        Account aFrom = accounts.get(fromIndex);
        Account aTo = accounts.get(toIndex);

        int amount;
        synchronized (aFrom) {
            if (aFrom.getMoney() == 0) {
                return true;
            }
            amount = 1 + random.nextInt(aFrom.getMoney());
        }

        Account first = aFrom.getId().compareTo(aTo.getId()) <= 0 ? aFrom : aTo;
        Account second = first == aFrom ? aTo : aFrom;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (aFrom.getMoney() < amount) {
                    logger.warn("Skipping transfer: insufficient funds after locking. from={} to={} requested={}", aFrom, aTo, amount);
                    return true;
                }

                int beforeFrom = aFrom.getMoney();
                int beforeTo = aTo.getMoney();

                aFrom.setMoney(beforeFrom - amount);
                aTo.setMoney(beforeTo + amount);

                int txNum = transactionCount.incrementAndGet();
                logger.info("TX#{}: {} -> {} : amount={} | beforeFrom={} beforeTo={} afterFrom={} afterTo={}",
                        txNum, aFrom.getId(), aTo.getId(), amount,
                        beforeFrom, beforeTo, aFrom.getMoney(), aTo.getMoney());

                if (txNum >= maxTransactions) {
                    logger.info("Reached max transactions ({}).", maxTransactions);
                }
                return true;
            } finally {
                second.getLock().unlock();
            }
        } catch (Exception e) {
            logger.error("Error during transfer from {} to {} amount={}", aFrom.getId(), aTo.getId(), amount, e);
            return true;
        } finally {
            first.getLock().unlock();
        }
    }

    public int totalMoney() {
        int sum = 0;
        for (Account a : accounts) {
            sum += a.getMoney();
        }
        return sum;
    }
}
