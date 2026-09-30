package com.example.genz.transaction.enums;

public enum TransactionType {
    TOP_UP,
    GOAL_DEPOSIT,
    GOAL_WITHDRAWAL,
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER;

    public TransactionDirection getDefaultDirection() {
        return switch (this) {
            case TOP_UP, GOAL_WITHDRAWAL, WITHDRAWAL -> TransactionDirection.IN;
            case GOAL_DEPOSIT, DEPOSIT, TRANSFER -> TransactionDirection.OUT;
        };
    }
}
