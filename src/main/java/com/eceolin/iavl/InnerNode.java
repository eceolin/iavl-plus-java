package com.eceolin.iavl;

public class InnerNode extends Node {
    private Node leftNode;
    private Node rightNode;

    @Override
    public boolean isLeaf() {
        return false;
    }
}
