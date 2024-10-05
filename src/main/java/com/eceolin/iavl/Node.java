package com.eceolin.iavl;

public class Node {
    String key;
    String hash;
    Chunk chunk;
    Integer height;
    String value;
    Node inner;
    Node left;
    Node right;

    public boolean isLeaf() {
        return left == null && right == null;
    }
}
