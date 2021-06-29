package com.example.grpc.server.grpcserver;

import com.example.grpc.server.grpcserver.MatrixRequest;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.Block;
import com.example.grpc.server.grpcserver.MatrixMultServiceGrpc.MatrixMultServiceImplBase;
import net.devh.boot.grpc.server.service.GrpcService;

import io.grpc.stub.StreamObserver;
import io.grpc.stub.StreamObservers;
import java.io.*;

@GrpcService
public class MatrixMultServiceImpl extends MatrixMultServiceImplBase {
        
        // Uses bi-directional streaming 
        @Override
        public StreamObserver<MatrixRequest> addblock(StreamObserver<MatrixReply> reply)
        {
              return new StreamObserver<MatrixRequest>() {
                @Override
                public void onNext(MatrixRequest request) {
                        System.out.println("Request received from client:\n" +request);
                        Block E = request.getA();
                        Block F = request.getB();

                // Add each (i,j)th element from each block
                        int i1 = E.getI1() + F.getI1();
                        int i2 = E.getI2() + F.getI2();
                        int i3 = E.getI3() + F.getI3();
                        int i4 = E.getI4() + F.getI4();

                // Construct a new 2x2 block to return
                        Block C = Block.newBuilder().setI1(i1).setI2(i2).setI3(i3).setI4(i4).build();

                        MatrixReply response=MatrixReply.newBuilder().setC(C).build();
                        reply.onNext(response);
                }

                @Override
                public void onError(Throwable t) {
                        reply.onError(t);
                }

                @Override
                public void onCompleted() {
                        reply.onCompleted();
                }
              };
        }

        // Uses bi-directional streaming
        @Override
        public StreamObserver<MatrixRequest> multblock(StreamObserver<MatrixReply> reply)
        {
             return new StreamObserver<MatrixRequest>() {
                @Override
                public void onNext(MatrixRequest request){
                        System.out.println("Request received from client:\n" +request);

                        Block A = request.getA();
                        Block B = request.getB();
                        int[] C = new int[4];

                // Compute the block multiplication (assuming both blocks are 2x2 matrices)
                        C[0]=A.getI1()*B.getI1()+A.getI2()*B.getI3();
                        C[1]=A.getI1()*B.getI2()+A.getI2()*B.getI4();
                        C[2]=A.getI3()*B.getI1()+A.getI4()*B.getI3();
                        C[3]=A.getI3()*B.getI2()+A.getI4()*B.getI4();

                // Construct a new 2x2 block to return
                        Block C2 = Block.newBuilder().setI1(C[0]).setI2(C[1]).setI3(C[2]).setI4(C[3]).build();

                        MatrixReply response= MatrixReply.newBuilder().setC(C2).build();
                        reply.onNext(response);
                }

                @Override
                public void onError(Throwable t) {
                        reply.onError(t);
                }

                @Override
                public void onCompleted() {
                        reply.onCompleted();
                }
              };
        }
}