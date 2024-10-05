package com.eceolin.iavl;

import java.util.concurrent.atomic.AtomicInteger;

public class IAVL {

    private final Integer maxChunkSize;
    private final Chunk[] chunks;

    public IAVL(Integer maxChunkSize, Integer maxChunksAllowed) {
        this.maxChunkSize = maxChunkSize;
        chunks = new Chunk[maxChunksAllowed];
    }

    private AtomicInteger nextCid = new AtomicInteger(0);

    public Node insert(Node node, String key, String val) {
        return insertAux(node, key, val, nextCid.get() == 0 ? null : chunks[nextCid.get()]);
    }

    private Node insertAux(Node node, String key, String val, Chunk lastChunk) {
        if (node == null) {
            Chunk chunk = newChunk(nextCid.getAndIncrement());
            node = insertInChunk(chunk, key, val);
            node.chunk = chunk;
            chunk.root = node;
            return node;
        }

        Chunk c;
        if (node.chunk != null) {
            c = node.chunk;
            if (c.size == maxChunkSize) {
                splitChunk(node);
            }
        } else {
            c = lastChunk;
        }

        if (node.isLeaf()) {
            Chunk nodeChunk = node.chunk;
            node.chunk = null;
            if (key.compareTo(node.key) < 0) {
                Node left = insertInChunk(c, key, val);
                node = newInner(node.key, left, node, 1);
            } else {
                Node right = insertInChunk(c, key, val);
                node = newInner(key, node, right, 1);
                node.right.inner = node;
                node.chunk = nodeChunk;
            }
            if (node.chunk != null) {
                c.root = node;
            }
        } else {
            if (key.compareTo(node.key) < 0) {
                node.left = insertAux(node.left, key, val, c);
            } else {
                node.right = insertAux(node.right, key, val, c);
            }
        }

        updateHeight(node);
        return balance(node);
    }

    private void updateHeight(Node node) {
        node.height = Math.max(getNodeHeight(node.left), getNodeHeight(node.right)) + 1;
    }

    private int getNodeHeight(Node node) {
        if (node != null && node.height != null) {
            return node.height;
        } else {
            return 0;
        }
    }

    private Node balance(Node node) {
        int h = (node.left != null && node.left.height != null ? node.left.height : 0) - (node.right != null && node.right.height != null ? node.right.height : 0);
        if (h < -1) { // if right branch is bigger than left branch
            return rotateRL(node); // rotate [right] left
        }
        if (h > 1) { // if left branch is bigger than right branch
            return rotateLR(node); // rotate [left] right
        }
        return node;
    }

    private Node newInner(String key, Node ln, Node rn, int ht) {
        Node newInode = new Node();
        newInode.key = key;
        newInode.left = ln;
        newInode.right = rn;
        newInode.height = ht;
        return newInode;
    }

    private Chunk newChunk(int cid) {
        Chunk newC = new Chunk();
        newC.cid = cid;
        newC.size = 0;
        newC.root = null;
        newC.leaf = new Node[maxChunkSize];
        chunks[cid] = newC;
        return newC;
    }

    private Node insertInChunk(Chunk c, String key, String val) {
        Node node = new Node();
        node.key = key;
        node.value = val;
        c.leaf[c.size] = node;
        c.size += 1;
        return node;
    }

    private void deleteFromChunk(Chunk c, String key) {
        for (int i = 0; i < c.size; i++) {
            Node node = c.leaf[i];
            if (node.key.equals(key)) { // found leaf
                c.leaf[i] = c.leaf[c.size - 1]; // swap with the last
                c.leaf[c.size - 1] = null; // free last leaf
                c.size -= 1; // decrement number of leaves
                if (c.size == 0) { // chunk is empty
                    nextCid.decrementAndGet(); // decrement number of chunks
                    Chunk lastChunk = chunks[nextCid.get()]; // get chunk with highest id
                    lastChunk.cid = c.cid; // replace chunk’s id
                    c = null; // free empty chunk
                }
                return;
            }
        }
    }

    private void splitChunk(Node node) { // split chunk rooted at node
        Chunk newC = newChunk(node.chunk.cid);
        DFS(node.left, node, newC);
        node.left.chunk = newC;
        newC.root = node.left; // assign left chunk
        nextCid.incrementAndGet();
        newC = newChunk(nextCid.get());
        DFS(node.right, node, newC);
        newC.root = node.right; // assign right chunk
        node.right.chunk = newC;
        node.chunk = null; // x is no longer chunk root
    }

    private void DFS(Node ptr, Node pnt, Chunk c) {
        if (ptr.isLeaf()) {
            c.leaf[c.size] = ptr;
            if (ptr.key.compareTo(pnt.key) < 0) { // assign parent
                pnt.left = c.leaf[c.size];
            } else {
                pnt.right = c.leaf[c.size];
            }
            c.size += 1; // one more leaf in chunk
        } else {
            DFS(ptr.left, ptr, c);
            DFS(ptr.right, ptr, c);
        }
    }

    public void delete(Node node, String key) {
        if (node.key.equals(key) && node.isLeaf()) { // only one node
            deleteFromChunk(node.chunk, key);
            return;
        }
        node = deleteAux(node, key, null); // return ptr to new node
    }

    private Node deleteAux(Node node, String key, Chunk lastChunk) {
        Chunk c;
        if (node.chunk != null) { // if node is the chunk root:
            c = node.chunk; // let c be this chunk
        } else {
            c = lastChunk; // chunk root is a higher node
        }

        if (node.left != null && node.left.key.equals(key) && node.left.isLeaf()) {
            if (c == null) { // chunk is necessarily on the left
                c = node.left.chunk;
            }
            deleteFromChunk(c, key);
            Node promoted = node.right;
            if (node.chunk != null) {
                promoted.chunk = node.chunk; // node is a chunk root
            }
            node = null; // deallocate node
            return promoted;
        }

        if (node.right != null && node.right.key.equals(key) && node.right.isLeaf()) {
            if (c == null) { // chunk is necessarily on the right
                c = node.right.chunk;
            }
            deleteFromChunk(c, key);
            Node promoted = node.left;
            if (node.chunk != null) {
                promoted.chunk = node.chunk; // node is a chunk root
            }
            node = null; // deallocate node
            return promoted;
        }

        if (node.right != null && node.right.left != null && node.right.left.key.equals(key) && node.right.left.isLeaf()) {
            if (c == null) { // chunk is lower
                if (node.right.chunk != null) { // chunk on right
                    c = node.right.chunk;
                    node.right.right.chunk = c;
                    node.chunk = null;
                } else {
                    c = node.right.left.chunk; // chunk on the left
                }
            }
            deleteFromChunk(c, key);
            Node aux = node.right;
            node.key = aux.key;
            node.right = aux.right;
            aux = null; // deallocate aux
            return node;
        }

        if (node.key.equals(key)) {
            Node[] result = deleteLeaf(node.right, c);
            Node n = result[0];
            Node p = result[1];
            node.key = n.key;
            node.right = p;
            n = null; // deallocate n
        } else {
            if (key.compareTo(node.key) < 0) {
                node.left = deleteAux(node.left, key, c);
            } else {
                node.right = deleteAux(node.right, key, c);
            }
        }

        updateHeight(node); // new height from children heights
        return balance(node);
    }

    private Node[] deleteLeaf(Node node, Chunk c) {
        if (node.isLeaf()) {
            if (c == null) { // chunk is necessarily on the right
                c = node.chunk;
            }
            deleteFromChunk(c, node.key);
            Node[] result = new Node[2];
            result[0] = node;
            result[1] = null;
            return result;
        }
        Node[] result = deleteLeaf(node.left, c);
        Node n = result[0];
        Node p = result[1];
        if (n == null) {
            node.left = p;
            return new Node[]{node, null};
        }
        node.left = n;
        return new Node[]{node, p};
    }

    private Node rotateRL(Node node) {
        int rlHeight = getNodeHeight(node.right.left);
        int rrHeight = getNodeHeight(node.right.right);
        if (rlHeight > rrHeight) {
            node.right = rotateR(node.right);
        }
        updateHeight(node);
        return rotateL(node);
    }

    private Node rotateLR(Node node) {
        int llHeight = getNodeHeight(node.left.left);
        int lrHeight = getNodeHeight(node.left.right);
        if (llHeight < lrHeight) {
            node.left = rotateL(node.left);
        }
        updateHeight(node);
        return rotateR(node);
    }

    private Node rotateL(Node node) {
        if (node.chunk != null) {
            node.chunk.root = node.right;
            node.right.chunk = node.chunk;
            node.chunk = null;
        } else {
            if (node.right.chunk != null) {
                splitChunk(node.right);
            }
        }
        Node newPivot = node.right;
        node.right = newPivot.left;
        newPivot.left = node;
        updateHeight(node);
        updateHeight(newPivot);
        return newPivot;
    }

    private Node rotateR(Node node) {
        if (node.chunk != null) {
            node.left.chunk = node.chunk;
            node.chunk = null;
        } else {
            if (node.left.chunk != null) {
                splitChunk(node.left);
            }
        }
        Node newPivot = node.left;
        node.left = newPivot.right;
        newPivot.right = node;
        updateHeight(node);
        updateHeight(newPivot);
        return newPivot;
    }
}
