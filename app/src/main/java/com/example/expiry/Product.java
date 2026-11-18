package com.example.expiry;

/**
 * Сохранённый расчёт срока хранения.
 * amount + unit задают длительность от startEpochDay до expiryEpochDay.
 * name — только для legacy-записей старого формата (может быть null).
 */
public class Product {
    public static final String UNIT_DAY = "day";
    public static final String UNIT_WEEK = "week";
    public static final String UNIT_MONTH = "month";
    public static final String UNIT_YEAR = "year";

    public final String name;          // legacy, null для новых записей
    public final long startEpochDay;    // дата начала
    public final int amount;            // длительность
    public final String unit;           // day | week | month | year
    public final long expiryEpochDay;   // вычисленная дата окончания

    public Product(String name, long startEpochDay, int amount, String unit, long expiryEpochDay) {
        this.name = name;
        this.startEpochDay = startEpochDay;
        this.amount = amount;
        this.unit = unit;
        this.expiryEpochDay = expiryEpochDay;
    }

    public boolean hasDuration() {
        return amount > 0;
    }
}
