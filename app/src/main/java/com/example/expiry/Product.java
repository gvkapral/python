package com.example.expiry;

public class Product {
    public String name;
    public long expiryEpochDay; // дата истечения в "epoch days"

    public Product(String name, long expiryEpochDay) {
        this.name = name;
        this.expiryEpochDay = expiryEpochDay;
    }
}
