package com.example.grpc.client.grpcclient;
import com.example.grpc.server.grpcserver.Block;

// Used for creating multiply block jobs.
public class Quad<T, U, V, W> {

    private T first;
    private U second;
    private V third;
    private W fourth;

    public Quad() {
        this.first = null;
        this.second = null;
        this.third = null;
        this.fourth = null;
    }

    public Quad(T first, U second, V third, W fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public T getFirst() { return first; }
    public U getSecond() { return second; }
    public V getThird() { return third; }
    public W getFourth() { return fourth; }
    public void setFirst(T first) { this.first = first; }
    public void setSecond(U second) { this.second = second; }
    public void setThird(V third) { this.third = third; }
    public void setFourth(W fourth) { this.fourth = fourth; }
}