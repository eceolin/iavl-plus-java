package com.eceolin.iavl;

public class LeafNode extends Node {

    private String value;
    private Node innerNode;

    @Override
    boolean isLeaf() {
        return true;
    }
}
