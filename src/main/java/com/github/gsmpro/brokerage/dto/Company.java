package com.github.gsmpro.brokerage.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Company {
    @Getter
    private final String name;
    @Getter
    private final String ticker;
    private final Queue<Double> prices = new LinkedList<>();
    @Getter
    private double totalClosedBuy = 0;
    @Getter
    private double totalClosedSell = 0;
    @Getter
    private Double lastBuyPrice = null;
    @Getter
    private Double lastSellPrice = null;
    @Getter
    private int currentQty = 0;
    @Getter
    private LocalDateTime minDate = LocalDateTime.MAX;
    @Getter
    private LocalDateTime maxDate = LocalDateTime.MIN;
    @Getter
    private boolean wasShorted = false;
    private Boolean firstOperationBuyFlag = null;
    private Boolean lastOperationBuyFlag = null;
    private boolean canAlter = false;

    public void startCompany() {
        checkCannotAlter();
        canAlter = true;
    }

    public void endCompany() {
        checkCanAlter();
        canAlter = false;
    }

    public boolean isActive() {
        checkCannotAlter();
        return prices.size() > 0;
    }

    public double getTotalClosedBalance() {
        checkCannotAlter();
        return totalClosedSell - totalClosedBuy;
    }

    public double getTotalBalance() {
        checkCannotAlter();
        return prices.stream().reduce(Double::sum).orElse(.0);
    }

    public String getFirstOperation() {
        checkCannotAlter();
        return getOperation(firstOperationBuyFlag);
    }

    public String getLastOperation() {
        checkCannotAlter();
        return getOperation(lastOperationBuyFlag);
    }

    public void buyOne(LocalDateTime time, double value) {
        checkCanAlter();
        if (prices.isEmpty()) {
            prices.add(-value);
        } else {
            if (prices.peek() * (-value) > 0) { // whether the same operation is at the top of the queue
                prices.add(-value);
            } else {
                totalClosedBuy += value;
                totalClosedSell += prices.poll();
            }
        }
        lastBuyPrice = value;
        balance(time, true);
    }

    public void sellOne(LocalDateTime time, double value) {
        checkCanAlter();
        if (prices.isEmpty()) {
            prices.add(value);
        } else {
            if (prices.peek() * value > 0) { // whether the same operation is at the top of the queue
                prices.add(value);
            } else {
                totalClosedBuy -= prices.poll();
                totalClosedSell += value;
            }
        }
        lastSellPrice = value;
        balance(time, false);
    }

    private void balance(LocalDateTime date, boolean isBuy) {
        minDate = Stream.of(minDate, date).min(LocalDateTime::compareTo).get();
        if (minDate.equals(date)) {
            firstOperationBuyFlag = isBuy;
        }
        maxDate = Stream.of(maxDate, date).max(LocalDateTime::compareTo).get();
        if (maxDate.equals(date)) {
            lastOperationBuyFlag = isBuy;
        }
        currentQty += isBuy ? 1 : -1;
        if (currentQty < 0) {
            wasShorted = true;
        }
    }

    private String getOperation(Boolean operationBuyFlag) {
        return operationBuyFlag == null
                ? ""
                : Boolean.TRUE.equals(operationBuyFlag)
                ? "B"
                : "S";
    }

    private void checkCanAlter() {
        if (!canAlter) {
            throw new IllegalStateException("Company is closed");
        }
    }

    private void checkCannotAlter() {
        if (canAlter) {
            throw new IllegalStateException("Company is opened");
        }
    }
}