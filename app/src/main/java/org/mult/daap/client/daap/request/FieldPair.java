package org.mult.daap.client.daap.request;

class FieldPair {
    public FieldPair(int s, int p) {
        size = s;
        position = p;
    }

    public final int position;
    public final int size;
}