package com.example.grpc.client.grpcclient;

import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import com.example.grpc.server.grpcserver.MatrixMultServiceGrpc;
import com.example.grpc.server.grpcserver.MatrixMultServiceGrpc.MatrixMultServiceStub;
import com.example.grpc.server.grpcserver.MatrixRequest;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.Block;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.lang.*;
import io.grpc.stub.StreamObserver;

@Service
public class GrpcClientService {

        ArrayList<String> ServerIPAddresses = new ArrayList<String>();

        ArrayList<MatrixMultServiceStub> stublist = new ArrayList<MatrixMultServiceStub>();
        int CurrentStub = 0;

        public int footprinting(Block X, Block Y, int deadline, int numOfBlocksToProcess) throws InterruptedException {

                ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
                MatrixMultServiceStub stub = MatrixMultServiceGrpc.newStub(channel);

                final SettableFuture<Triple<Block, Integer, Integer>> future = SettableFuture.create();

                long startTime = System.nanoTime();

                // Measure the time taken to make an asychronous call.
                StreamObserver<MatrixRequest> requestObserver = stub.multblock(new OutputStreamObserver(future, 0, 0));
                requestObserver.onNext(MatrixRequest.newBuilder().setA(X).setB(Y).build());
                requestObserver.onCompleted();

                // This line will block, which will allow the endTime variable to measure the time taken to multiply one block.
                Triple<Block, Integer, Integer> b = future.get(); 

                long endTime = System.nanoTime();

                channel.shutdown();

                // Computes the number of machines needed.
                long elapsedTime = endTime-startTime;
                long computeTime = elapsedTime * numOfBlocksToProcess;
                int numOfMachinesNeeded = (int) Math.ceil(computeTime/deadline);

                return numOfMachinesNeeded;
        }

        public ArrayList<Block> multiStub(ArrayList<Block> A, ArrayList<Block> B, int sizeM, int numOfStubs, int numOfBlockMultiplications) throws FileNotFoundException, InterruptedException, ExecutionException{

                // Appends IP addresses from 'ips' file, into ServerIPAddresses ArrayList.
                Scanner scanner = new Scanner(new File("ips.txt"));
                for(Scanner sc = scanner; sc.hasNext(); ){
                        String address = sc.nextLine();
                        ServerIPAddresses.add(address);
                }

                int size = sizeM/2;
                Block[][] aa = new Block[size][size];
                Block[][] bb = new Block[size][size];
                Block[][] cc = new Block[size][size];
                ArrayList<Block> C = new ArrayList<Block>();
                int numBlocks = A.size();
                int t=1;

                ArrayList<ManagedChannel> channels = new ArrayList<ManagedChannel>();

                // Futures that will be added for multi block calls.
                ArrayList<SettableFuture<Triple<Block, Integer, Integer>>> mult_futures = new ArrayList<SettableFuture<Triple<Block, Integer, Integer>>>();

                if(numOfStubs == -1){
                        numOfStubs = ServerIPAddresses.size();
                }
                int counter = 0;

                // Initializes channels and stubs, using all server IP addresses that are added.
                for(String address: ServerIPAddresses){
                        if(counter == numOfStubs){
                                break;
                        }
                        ManagedChannel channel = ManagedChannelBuilder.forAddress(address, 9090).usePlaintext().build();
                        stublist.add(MatrixMultServiceGrpc.newStub(channel));
                        channels.add(channel);
                        counter++;
                }

                // Initializes Futures
                for(int i=0; i<numOfBlockMultiplications; i++){
                        SettableFuture<Triple<Block, Integer, Integer>> empty_multiply_future = SettableFuture.create();
                        mult_futures.add(empty_multiply_future);
                }

                // Initializes Resultant Matrix values
                for(int v=0; v<numBlocks; v++){
                        Block zeros = Block.newBuilder().setI1(0).setI2(0).setI3(0).setI4(0).build();
                        C.add(zeros);
                }

                // Converts block lists A, B and resultant into java matrices, that contain a 2x2 block in each entry.
                for(int r=0; r<size; r++){
                        for(int s=0; s<size; s++){
                                aa[r][s] = A.get(t*r + s);
                                bb[r][s] = B.get(t*r + s);
                                cc[r][s] = C.get(0);
                        }
                        t++;
                }

                List<Quad<Block, Block, Integer, Integer>> listOfMultJobs = new ArrayList<>();

                // Decides which blocks need to be multiplied together. I call this a 'multiply job'.
                for(int i=0; i<size; i++){
                        for(int j=0; j<size; j++){
                                for(int k=0; k<size; k++){
                                        Quad<Block, Block, Integer, Integer> q = new Quad<>(aa[i][k], bb[k][j], i, j);
                                        listOfMultJobs.add(q);
                                }
                        }
                }

                ArrayList<StreamObserver<MatrixRequest>> mult_observers = new ArrayList<StreamObserver<MatrixRequest>>();
                int i = 0;

                // Iterates through all multiply jobs
                for(Quad<Block, Block, Integer, Integer> job: listOfMultJobs){
                        // Creates an observer that, that takes in a tuple.
                        StreamObserver<MatrixRequest> requestObserver = getAStub().multblock(new OutputStreamObserver(mult_futures.get(i), job.getThird(), job.getFourth()));
                        // Asynchronously sends the blocks that need to be multiplied through the stub.
                        requestObserver.onNext(MatrixRequest.newBuilder().setA(job.getFirst()).setB(job.getSecond()).build());
                        mult_observers.add(requestObserver);
                        i++;
                }

                // Sums values within the ith and jth position blocks
                for(Future<Triple<Block, Integer, Integer>> future: mult_futures){
                        Triple<Block, Integer, Integer> triple = future.get();
                        Block newC = triple.getFirst();
                        int pos1 = triple.getSecond();
                        int pos2 = triple.getThird();

                        cc[pos1][pos2] = Block.newBuilder().setI1(cc[pos1][pos2].getI1() + newC.getI1()).setI2(cc[pos1][pos2].getI2() + newC.getI2()).setI3(cc[pos1][pos2].getI3() + newC.getI3()).setI4(cc[pos1][pos2].getI4() + newC.getI4()).build();
                }

                t=1;

                // Converts java block matrix into an ArrayList of blocks.
                for(int r=0; r<size; r++){
                        for(int s=0; s<size; s++){
                                C.set(t*r + s, cc[r][s]);
                        }
                        t++;
                }

                // Calls off all request observers.
                for(StreamObserver<MatrixRequest> observer: mult_observers){
                        observer.onCompleted();
                }

                return C;       // Return resultant matrix.
        }


        // Return a stub by iterating through the stub list. Eg. stub 0, 1, 2, 3, 0, 1, 2, 3, etc. 
        public MatrixMultServiceStub getAStub(){
                MatrixMultServiceStub stub = stublist.get(CurrentStub);

                if(CurrentStub+1 < stublist.size()){
                        CurrentStub++;
                }
                else{
                        CurrentStub = 0;
                }

                return stub;
        }
}