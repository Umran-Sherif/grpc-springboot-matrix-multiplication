package com.example.grpc.client.grpcclient;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.grpc.server.grpcserver.MatrixRequest;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.Block;
import com.example.grpc.server.grpcserver.MatrixMultServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.*;
import java.io.*;
import java.lang.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class MatrixMultController
{
        private ArrayList<Block> listOfBlocksInA;
        private ArrayList<Block> listOfBlocksInB;
        private ArrayList<Block> resultantMatrixBlocks;
        private int SizeOfMatrices;

        GrpcClientService grpcClientService;

        @Autowired
        public MatrixMultController(GrpcClientService grpcClientService){
                this.grpcClientService = grpcClientService;
        }

        @Autowired
        FileUploadService fileUploadService;

        // Upload matrix A and B in the format 'MatrixA.txt' and 'MatrixB.txt'.
        @PostMapping("/upload")
        public void uploadFile(@RequestParam("file") MultipartFile file) throws IOException, IllegalStateException {
                fileUploadService.uploadFile(file);
        }

        // Initialize the matrices in the rest controller once the files have been uploaded.
        @GetMapping("/build")
        public String buildMatricesFromFile() throws FileNotFoundException
        {
            // Check if the uploaded files are square, have a height/width of power 2 and that both have the same size.
            this.SizeOfMatrices = checkUploadedFiles();

            // If one of these criterias are not met, then return an error.
            if(this.SizeOfMatrices == -1){
                return "Check the console to see which criterions your files have not met.";
            }

            // Fetch matrix A and B from the files.
            int[][] matrix_a = getMatrix("A");
            int[][] matrix_b = getMatrix("B");

            // Split the matrices into a list of blocks.
            this.listOfBlocksInA = this.splitMatrix(matrix_a);
            this.listOfBlocksInB = this.splitMatrix(matrix_b);

            return "Successfully built matrix blocks from files! <br> A and B are " + this.SizeOfMatrices +
                "x" + this.SizeOfMatrices + " matrices." + "<br> All matrices contain " + this.listOfBlocksInA.size() + " block(s) <br> Go to /multistub?deadline=, /original, /originalblocks or /result.";
        }

        @GetMapping("/multistub")
        public String callClientService(@RequestParam(value = "deadline") String deadline) throws InterruptedException, ExecutionException, FileNotFoundException
        {
                int numOfStubsNeeded = -1;
                int pow = (int)(Math.log(this.SizeOfMatrices)/Math.log(2)) - 1;
                int numOfBlockMultiplications = (int) Math.pow(8, pow);

                // If a deadline is set, then proceed to the footprinting function, to find out how many stubs are needed.
                if(! (deadline=="")){
                        int deadl = Integer.parseInt(deadline);
                        numOfStubsNeeded = grpcClientService.footprinting(this.listOfBlocksInA.get(0), this.listOfBlocksInB.get(0), deadl, numOfBlockMultiplications);
                }

                // Call the multiStub method from the client service, and retrieve the resultant matrix as a list of blocks.
                this.resultantMatrixBlocks = grpcClientService.multiStub(this.listOfBlocksInA, this.listOfBlocksInB, this.SizeOfMatrices, numOfStubsNeeded, numOfBlockMultiplications);
                return printMatrix("Resultant Matrix");
        }

        // Print Matrix A and B
        @GetMapping("/original")
        public String originalMatrices(){
                return printMatrix("Matrix A") + printMatrix("Matrix B");
        }

        // Print Matrix A and B as a list of blocks
        @GetMapping("/originalblocks")
        public String originalBlocks()
        {
            String matrixBlocks = "Matrix A blocks: <br> <br>";

            for(Block block: this.listOfBlocksInA){
                matrixBlocks = matrixBlocks.concat(block.getI1() + " " + block.getI2() + "<br>");
                matrixBlocks = matrixBlocks.concat(block.getI3() + " " + block.getI4() + "<br>" + "<br>");
            }

            matrixBlocks = matrixBlocks.concat("<br> Matrix B blocks: <br> <br>");

            for(Block block: this.listOfBlocksInB){
                matrixBlocks = matrixBlocks.concat(block.getI1() + " " + block.getI2() + "<br>");
                matrixBlocks = matrixBlocks.concat(block.getI3() + " " + block.getI4() + "<br>" + "<br>");
            }

            return matrixBlocks;
        }

        // Print the resultant matrix again
        @GetMapping("/result")
        public String resultMatrix(){
                return printMatrix("Resultant Matrix");
        }

        public int checkUploadedFiles() throws FileNotFoundException{
            int fixed_widthA = -1;
            int fixed_widthB = -1;
            int heightA = 0;
            int heightB = 0;

            Scanner sc1 = new Scanner(new File("MatrixA.txt"));
            Scanner sc2 = new Scanner(new File("MatrixB.txt"));

            // Check if the width is the same for each row for matrix A
            for(Scanner sc = sc1; sc.hasNext(); ){
                String line = sc.nextLine();
                int widthA = line.length() - line.replaceAll(" ", "").length() + 1;

                if(fixed_widthA == -1){
                    fixed_widthA = line.length() - line.replaceAll(" ", "").length() + 1;
                }
                else{
                    if(widthA != fixed_widthA){
                        System.out.println("Inconsistent widths!");
                        return -1;
                    }
                }
                heightA++;
            }

            // Check if the width is the same for each row for matrix B
            for(Scanner sc = sc2; sc.hasNext(); ){
                String line = sc.nextLine();
                int widthB = line.length() - line.replaceAll(" ", "").length() + 1;

                if(fixed_widthB == -1){
                    fixed_widthB = line.length() - line.replaceAll(" ", "").length() + 1;
                }
                else{
                    if(widthB != fixed_widthB){
                        System.out.println("Inconsistent widths!");
                        return -1;
                    }
                }
                heightB++;
            }

            if(fixed_widthA != heightA){
                System.out.println("A is not a square matrix!");
                return -1;
            }

            if(fixed_widthB != heightB){
                System.out.println("B is not a square matrix!");
                return -1;
            }

            if(fixed_widthA != fixed_widthB && heightA != heightB){
                System.out.println("A and B are not the same size matrices!");
                return -1;
            }

            int size = heightA;

            // Given that we know it is a square matrix, check if it is of a power of 2:
            if ( (size != 0) && ((size & (size - 1)) == 0) )
            {
                System.out.println("Both matrices have size of power 2!");
                return size;
            }

            System.out.println("Both matrices do not have a size of power 2");
            return -1;
        }

        public int[][] getMatrix(String type) throws FileNotFoundException{
            try{
                Scanner sc = null;
                int size = this.SizeOfMatrices;

                if(type == "A"){
                    sc = new Scanner(new File("MatrixA.txt"));
                }
                else if(type == "B"){
                    sc = new Scanner(new File("MatrixB.txt"));
                }

                int[][] matrix = new int[size][size];

                int i = 0;

                while( sc.hasNext() ){
                    String line = sc.nextLine();
                    int[] values = Arrays.stream(line.split(" ")).mapToInt(Integer::parseInt).toArray();
                    int j = 0;
                    for(int val: values){
                        matrix[i][j] = val;
                        j++;
                    }
                    i++;
                }
                return matrix;

            } catch(FileNotFoundException e){
                System.out.println("Matrix " + type + "'s file does not exist");
            }

            catch(Exception e){
                System.out.println(e);
            }
            return null;
        }

        public ArrayList<Block> splitMatrix(int[][] m){
            ArrayList<Block> blocks = new ArrayList<Block>();
            int size = this.SizeOfMatrices;

            for(int i=0; i<size; i=i+2){
                for(int j=0; j<size; j=j+2){
                    blocks.add( Block.newBuilder().setI1(m[i][j]).setI2(m[i][j+1]).setI3(m[i+1][j]).setI4(m[i+1][j+1]).build() );
                }
            }

            return blocks;
        }

        public String printMatrix(String type){
                int size = this.SizeOfMatrices;
                Block[][] M = new Block[size][size];
                ArrayList<Block> listOfBlocks = new ArrayList<Block>();

                if(type=="Matrix A"){
                        listOfBlocks = this.listOfBlocksInA;
                }

                else if(type=="Matrix B"){
                        listOfBlocks = this.listOfBlocksInB;
                }

                else if(type=="Resultant Matrix"){
                        listOfBlocks = this.resultantMatrixBlocks;
                }

                else{
                        return "That list does not exist";
                }

                int t=1;

                for(int i = 0; i<size/2; i++){
                        for(int j = 0; j<size/2; j++){
                                M[i][j] = listOfBlocks.get(t*i + j);
                        }
                        t++;
                }

                String result = type + ": <br> <br>";

                for (int i=0; i<size/2; i++)
                {
                    for (int j=0; j<size/2;j++)
                    {
                        result = result.concat(M[i][j].getI1() + " " + M[i][j].getI2() + " ");
                    }
                    result = result.concat("<br>");

                    for (int k=0; k<size/2; k++){
                        result = result.concat(M[i][k].getI3() + " " + M[i][k].getI4() + " ");
                    }
                    result = result.concat("<br>");
                }

                return result.concat("<br>");
        }
}