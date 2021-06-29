package com.example.grpc.client.grpcclient;

import io.grpc.stub.StreamObserver;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.Block;
import java.lang.Throwable;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Future;

public class OutputStreamObserver implements StreamObserver<MatrixReply> {

        final SettableFuture<Triple<Block, Integer, Integer>> future;
        private final int i;
        private final int j;

        OutputStreamObserver(SettableFuture<Triple<Block, Integer, Integer>> future, int i, int j){
                this.future = future;
                this.i = i;
                this.j = j;
        }

        // Handles replies from a GRPC server stub.
        @Override
        public void onNext(MatrixReply reply){
                Block newC = reply.getC();
                Triple<Block, Integer, Integer> triple = new Triple<>(newC, this.i, this.j);
                future.set(triple);
        }

        @Override
        public void onError(Throwable t) {
                future.setException(new RuntimeException("Encountered an error in streaming call", t));
        }

        @Override
        public void onCompleted() {
        }
}