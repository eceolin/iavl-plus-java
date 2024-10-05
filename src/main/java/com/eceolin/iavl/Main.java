package com.eceolin.iavl;

public class Main {

    public static void main(String[] args) {

        IAVL tree = new IAVL(2, 50);

        Node n = tree.insert(null, "10", "10");
        n = tree.insert(n, "20", "20");
        n = tree.insert(n, "30", "30");
        n = tree.insert(n, "25", "25");

        System.out.println(n);
    }
}
