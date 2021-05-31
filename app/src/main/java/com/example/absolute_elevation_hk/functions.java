package com.example.absolute_elevation_hk;

import android.location.Location;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static java.lang.Double.parseDouble;

public class functions {
    /**
     * Load true elevation data from DTM slices
     * @param filename name of the DTM slices
     * @return Elevation data in Matrix format
     */
    public static Matrix loadfile(String filename){

        File file = new File("/sdcard/elevation/"+filename);
        String content = "";
        if (!file.isDirectory()) {  //检查此路径名的文件是否是一个目录(文件夹)
            if (file.getName().endsWith("txt")) {//文件格式为""文件
                try {
                    InputStream instream = new FileInputStream(file);
                    if (instream != null) {
                        InputStreamReader inputreader
                                = new InputStreamReader(instream, "UTF-8");
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line = "";
                        //分行读取
                        while ((line = buffreader.readLine()) != null) {
                            content += line + "\n";
                        }
                        instream.close();//关闭输入流
                    }
                } catch (java.io.FileNotFoundException e) {
                    Log.d("TestFile", "The File doesn't not exist.");
                } catch (IOException e) {
                    Log.d("TestFile", e.getMessage());
                }
            }
        }

        String[] lines = content.split("\n");
        String[] words0 = lines[0].split("\t");
        double[][] wordstemp = new double[lines.length][words0.length];
        for (int i = 0; i<lines.length;i++){
            String templine = lines[i];
            String[] words = templine.split("\t");
            for (int j = 0; j<words.length; j++){
                wordstemp[i][j] = parseDouble(words[j]);
            }
        }
        return new Matrix(wordstemp);
    }

    /**
     * Record value to the special direction (add)
     * @param value : value
     * @param fileName: store direction
     */
    public static void writeValueToFile(double value, String fileName){

        long CurrentTime = System.currentTimeMillis();
        String context = CurrentTime + "," + value + "\n";
        WriteFile.writeTxtToFiles("",fileName,context);
    }

    public static void writeTestdataValueToFile(String value, String fileName) {

        long CurrentTime = System.currentTimeMillis();
        String context = CurrentTime + "," + value + "\n";
        WriteFile.writeTxtToFiles("", fileName, context);
    }

    /**
     * Record value to the special direction (rewrite)
     * @param value : value
     * @param fileName: store direction
     */
    public static void rewriteValueToFile(double value, String fileName){

        long CurrentTime = System.currentTimeMillis();
        String context = CurrentTime + "," + value + "\n";
        WriteFile.rewriteTxtToFiles("",fileName,context);
    }

    /**
     * Create Polygon based on created holes
     * @param holes: input holes
     * @return Polygon ready to be shown in the map
     */
    static PolygonOptions createPolygonWithHoles(List<List<LatLng>> holes){
        PolygonOptions polyOptions = new PolygonOptions()
                .fillColor(0x33000000)
                .addAll(createBoundsOfEntireMap())
                .strokeColor(0xFF000000)
                .strokeWidth(5);
        for (List<LatLng> hole : holes){
            polyOptions.addHole(hole);
        }
        return polyOptions;
    }

    /**
     * Get the boundary point of the whole map
     * @return Point list in LatLng
     */
    public static List<LatLng> createBoundsOfEntireMap(){
        final float delta = 0.01f;
        return new ArrayList<LatLng>(){{
            add(new LatLng(90-delta,-180+delta));
            add(new LatLng(0,-180+delta));
            add(new LatLng(-90+delta,-180+delta));
            add(new LatLng(-90+delta,-0));
            add(new LatLng(-90+delta,180-delta));
            add(new LatLng(0,180-delta));
            add(new LatLng(90-delta,0));
            add(new LatLng(90-delta,-180+delta));
        }};
    }

    /**
     * When the point is inside the convex hell, we get the which Triangle constains the point
     * @param delaunayTriangulator: input convex hell
     * @param myLocaiton: user locaiton
     * @return Triangle contains the point
     */
    public static Triangle2D PointInWhichTriangle2D(DelaunayTriangulator[] delaunayTriangulator, Location myLocaiton){
        Transform mytransform = new Transform();
        GridData myHK80 = mytransform.GEOHK(1,myLocaiton.getLatitude(),myLocaiton.getLongitude());
        Vector2D point = new Vector2D(myHK80.getE(),myHK80.getN());
        TriangleSoup soup = new TriangleSoup();
        for (int i = 0; i < delaunayTriangulator[0].getTriangles().size();i++){
            soup.add(delaunayTriangulator[0].getTriangles().get(i));
        }
        return soup.findContainingTriangle(point);
    }

    /**
     * When the point is outside the convex hull, we get the nearst edge of the point
     * @param delaunayTriangulator: Triangular mesh
     * @return 2D Edge
     */
    public static Edge2D PointNearstEdge(DelaunayTriangulator[] delaunayTriangulator, Location myLocation){
        Transform mytransform = new Transform();
        GridData myHK80 = mytransform.GEOHK(1,myLocation.getLatitude(),myLocation.getLongitude());
        Vector2D point = new Vector2D(myHK80.getE(),myHK80.getN());
        TriangleSoup soup = new TriangleSoup();
        for (int i = 0; i < delaunayTriangulator[0].getTriangles().size();i++){
            soup.add(delaunayTriangulator[0].getTriangles().get(i));
        }
        return soup.findNearestEdge(point);
    }

    /**
     * Calculate the absolute elevation based on MSL pressure and calibrated barometer output
     * @param mysensroPressure
     * @param mycalcuatedPressure
     * @return Absolute elevation of the location
     */
    public static double calculateElevation(Double mysensroPressure, Double mycalcuatedPressure){
        return (1-Math.pow(mysensroPressure/mycalcuatedPressure,(1/5.255)))*44330;
    }

    /**
     * Calculate the MSL pressure of the location based on the Triangle Mesh (Simple Linear algorithm)
     * @param delaunayTriangulator: Triangular Mesh
     * @return MSL of input location
     */
    public static double CalculatePressureBasedOnSL(DelaunayTriangulator[] delaunayTriangulator,
                                             List<Double> webPressureList,
                                             Location myLocation){
        List<Vector2D> pointSet = delaunayTriangulator[0].getPointSet();
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        double a0 = 0.0;
        double a1 = 0.0;
        double a2 = 0.0;

        for (int i = 0; i < pointSet.size();i++){
            xList.add(pointSet.get(i).x);
            yList.add(pointSet.get(i).y);
        }

        double[][] AD =  new double[pointSet.size()][3];
        for (int i = 0; i<pointSet.size();i++){
            AD[i][0] = 1;
            AD[i][1] = xList.get(i);
            AD[i][2] = yList.get(i);
        }

        double[][] webP = new double[webPressureList.size()][1];
        for (int i = 0; i < webPressureList.size();i++){
            webP[i][0] = webPressureList.get(i);
        }
        Matrix webPM = new Matrix(webP);
        Matrix A = new Matrix(AD);
        Matrix AT = A.transpose();
        Matrix ATA = AT.times(A);
        Matrix ATAInv = ATA.inverse();
        Matrix ATAInvAT = ATAInv.times(AT);
        Matrix ATAInvWebP = ATAInvAT.times(webPM);
        double[][] AValue = ATAInvWebP.getArrayCopy();

        a0 = AValue[0][0];
        a1 = AValue[1][0];
        a2 = AValue[2][0];

        Transform mytransform = new Transform();
        GridData myHK80 = mytransform.GEOHK(1,myLocation.getLatitude(),myLocation.getLongitude());
        return (a0+a1*myHK80.getN()+a2*myHK80.getE());
    }

    /**
     * Calculate the MSL pressure of the location based on the Triangle Mesh (Simple Linear algorithm based on triangle)
     * @param triangle: point in this triangle
     * @param delaunayTriangulator:Triangular Mesh
     * @param myLocation
     * @return MSL of input location
     */
    public static double CalculatePressureBasedOnSLTriangle(Triangle2D triangle,
                                                     DelaunayTriangulator[] delaunayTriangulator,
                                                     Location myLocation,
                                                     List<Double> webPressureList){

        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        xList.add(triangle.a.x);
        xList.add(triangle.b.x);
        xList.add(triangle.c.x);

        yList.add(triangle.a.y);
        yList.add(triangle.b.y);
        yList.add(triangle.c.y);

        double a0 = 0.0;
        double a1 = 0.0;
        double a2 = 0.0;

        double[][] AD =  new double[xList.size()][3];
        for (int i = 0; i<xList.size();i++){
            AD[i][0] = 1;
            AD[i][1] = xList.get(i);
            AD[i][2] = yList.get(i);
        }

        double[][] webP = new double[xList.size()][1];
        List<Vector2D> pointSet = delaunayTriangulator[0].getPointSet();
        for (int i =0; i < pointSet.size(); i++){

            for (int j = 0; j < xList.size(); j++){
                if ((pointSet.get(i).x == xList.get(j))&& (pointSet.get(i).y == yList.get(j))){
                    Log.i("IndexOfTest i :",String.valueOf(i));
                    Log.i("IndexOfTest j :",String.valueOf(j));
                    webP[j][0] = webPressureList.get(i);
                }
            }
        }

        Matrix webPM = new Matrix(webP);
        Matrix A = new Matrix(AD);
        Matrix AT = A.transpose();
        Matrix ATA = AT.times(A);
        Matrix ATAInv = ATA.inverse();
        Matrix ATAInvAT = ATAInv.times(AT);
        Matrix ATAInvWebP = ATAInvAT.times(webPM);
        double[][] AValue = ATAInvWebP.getArrayCopy();

        a0 = AValue[0][0];
        a1 = AValue[1][0];
        a2 = AValue[2][0];

        Transform mytransform = new Transform();
        GridData myHK80 = mytransform.GEOHK(1,myLocation.getLatitude(),myLocation.getLongitude());
        return (a0+a1*myHK80.getN()+a2*myHK80.getE());
    }

    /**
     * Calculate the MSL pressure of the location based on the Triangle Mesh (IDW algorithm)
     * @param delaunayTriangulator:Triangular Mesh
     * @param myLocaiton
     * @return MSL of input location
     */
    public static double CalculatePressureBasedOnIDW(DelaunayTriangulator[] delaunayTriangulator,
                                              Location myLocaiton,
                                              List<Double> webPressureList){
        List<Vector2D> pointSet = delaunayTriangulator[0].getPointSet();
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        List<Double> wList = new ArrayList<>();
        Transform mytransform = new Transform();
        GridData myHK80 = mytransform.GEOHK(1,myLocaiton.getLatitude(),myLocaiton.getLongitude());
        double x0 = myHK80.getE();
        double y0 = myHK80.getN();
        for (int i = 0; i < pointSet.size();i++){
            xList.add(pointSet.get(i).x);
            yList.add(pointSet.get(i).y);
        }
        for (int i = 0; i < webPressureList.size();i++){
            double tempW = 1/((xList.get(i)-x0)*(xList.get(i)-x0)+(yList.get(i)-y0)*(yList.get(i)-y0));
            wList.add(tempW);
        }
        double sumWM = 0.0;
        double sumW = 0.0;
        for (int i = 0; i < wList.size();i++){
            sumWM = sumWM + wList.get(i)*webPressureList.get(i);
            sumW = sumW + wList.get(i);
        }

        return (sumWM/sumW);
    }

    /**
     * Calculate the STD of the true elevation from DTM with grid span "5"
     * @param myMatrix DTM data in Matrix format 1000*1000
     * @param indexN index in North (from 0-999)
     * @param indexE index in East (from 0-999)
     * @return STD value
     */
    public static double[] CalcualteSTDofTrueElevation(Matrix myMatrix, int indexN, int indexE){

        int span = 5;
        int startIndexN = indexN-span;
        int endIndexN = indexN+span;
        int startIndexE = indexE-span;
        int endIndexE = indexE+span;
        startIndexN = Math.max(0,startIndexN);
        endIndexN = Math.min(999,endIndexN);
        startIndexE = Math.max(0,startIndexE);
        endIndexE = Math.min(999,endIndexE);
        double[] data = new double[(endIndexN-startIndexN+1)*(endIndexE-startIndexE+1)];
        int k = 0;
        for(int i = startIndexN;i<=endIndexN;i++){
            for (int j = startIndexE;j<=endIndexE;j++){
                data[k] = myMatrix.get(i,j);
                k = k+1;
            }
        }
        double myvalue = myMatrix.get(indexN,indexE);

        return StandardDiviation(data,myvalue);
    }

    /**
     * Calcualte the standard deviation of the array list
     * @param x: input data array
     * @return Standard deviation of the array
     */
    public static double [] StandardDiviation(double[] x, double myValue) {
        double[] result = new double[2];
        int m=x.length;
        double sum=0;
        for(int i=0;i<m;i++){
            sum+=x[i];
        }
        double dAve=sum/m;//求平均值
        double dVar=0;
        for(int i=0;i<m;i++){//求方差
            dVar+=(x[i]-dAve)*(x[i]-dAve);
        }
        result[0] = Math.sqrt(dVar/m);
        result[1] = Math.abs(myValue-dAve);
        return result;
    }

    /**
     * Check the whether the manual calibration have being done or not
     * @param fileName file to store the manual calibration result
     * @return statement
     */
    public static boolean fileisExist(String fileName){

        File parent_path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);

        String dirPath = parent_path.getAbsolutePath() + "/";
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            return false;
        }
        String strFilePath = dirPath + "/" + fileName;

        File mFile = new File(strFilePath);
        if (!mFile.exists()) {
            return false;
        }else{
            return true;
        }
    }

    public static double readManualCalibResult(String fileName){
        File file = new File("/sdcard/Download/"+fileName);
        String content = "";
        if (!file.isDirectory()) {  //检查此路径名的文件是否是一个目录(文件夹)
            if (file.getName().endsWith("txt")) {//文件格式为""文件
                try {
                    InputStream instream = new FileInputStream(file);
                    if (instream != null) {
                        InputStreamReader inputreader
                                = new InputStreamReader(instream, "UTF-8");
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line = "";
                        //分行读取
                        while ((line = buffreader.readLine()) != null) {
                            content += line + "\n";
                        }
                        instream.close();//关闭输入流
                    }
                } catch (java.io.FileNotFoundException e) {
                    Log.d("TestFile", "The File doesn't not exist.");
                } catch (IOException e) {
                    Log.d("TestFile", e.getMessage());
                }
            }
        }

        String[] words = content.split(",");

        return parseDouble(words[1]);
    }

}
