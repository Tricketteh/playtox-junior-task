package com.tricketteh;

import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private final String id;
    private int money;
    private final ReentrantLock lock = new ReentrantLock();

    public Account(String id, int money) {
        this.id = id;
        this.money = money;
    }

    public String getId() {
        return id;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return "Account{id='" + id + "', money=" + money + "}";
    }
}
