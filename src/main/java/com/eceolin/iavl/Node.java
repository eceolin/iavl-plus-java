package com.eceolin.iavl;

public abstract class Node {
    private String key;
    private String hash;
    private Chunk chunk;
    private Integer height;

    abstract boolean isLeaf();
}
