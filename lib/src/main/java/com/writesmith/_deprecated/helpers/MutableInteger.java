package com.writesmith._deprecated.helpers;

public class MutableInteger extends Number implements IMutableInteger {
    Integer i;

    public MutableInteger(Integer i) {
        this.i = i;
    }

    @Override
    public void set(Integer i) {
        this.i = i;
    }

    @Override
    public int intValue() {
        return i;
    }

    @Override
    public long longValue() {
        return i;
    }

    @Override
    public float floatValue() {
        return i;
    }

    @Override
    public double doubleValue() {
        return i;
    }
}
