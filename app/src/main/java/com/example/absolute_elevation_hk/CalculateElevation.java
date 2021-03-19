package com.example.absolute_elevation_hk;

import android.location.Location;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 *
This class for the elevation calculation includes:
1. Location information of the stations;
2. Get station pressure from the website;
 */
public class CalculateElevation {
    /**
     * Function to link the web pressure info to the location info
     * @return location and pressure pairs
     */
    public static List<PointType> getStartionLocationList(){
        List<PointType> stationLocations = new ArrayList<>();

        stationLocations.add(PointType.init(810000.803000000,818963.741000000,0));
        stationLocations.add(PointType.init(820779.176000000,806953.069000000,0));
        stationLocations.add(PointType.init(835988.994000000,818111.059000000,0));
        stationLocations.add(PointType.init(816377.449000000,836610.563000000,0));
        stationLocations.add(PointType.init(822506.571000000,816917.513000000,0));
        stationLocations.add(PointType.init(839678.888000000,829246.465000000,0));
        stationLocations.add(PointType.init(826781.530000000,832970.960000000,0));
        stationLocations.add(PointType.init(829501.293000000,840259.703000000,0));
        stationLocations.add(PointType.init(834189.125000000,843211.354000000,0));
        stationLocations.add(PointType.init(836475.365000000,834075.415000000,0));
        stationLocations.add(PointType.init(849309.699000000,804859.219000000,0));
        stationLocations.add(PointType.init(818978.754000000,836361.347000000,0));
        List<Double> webInfoList = getInfoFromWeb("http://www.weather.gov.hk/wxinfo/ts/text_readings_e.htm","1234");
        for (int i = 0; i < webInfoList.size();i++){
            double tempPressure = webInfoList.get(i);
            stationLocations.set(i,PointType.init(stationLocations.get(i).getX(),stationLocations.get(i).getY(),tempPressure));
        }
        return  stationLocations;
    }

    /**
     * Get the pressure info from the website
     * @param website: website
     * @param timeStamp: time to request the info
     * @return pressrue information with N/A to 0.0
     */
    public static List<Double> getInfoFromWeb(String website,String timeStamp) {
        try {
            //建立连接
            URL url = new URL(website);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoInput(true);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            //获取输入流
            InputStream input = httpUrlConn.getInputStream();
            //将字节输入流转换为字符输入流
            InputStreamReader read = new InputStreamReader(input, "utf-8");
            //为字符输入流添加缓冲
            BufferedReader br = new BufferedReader(read);
            // 读取返回结果
            int index = 0;
            List<String> LocationList = new ArrayList<>();
            List<Double> PressureList = new ArrayList<>();
            String data = br.readLine();
            while(data!=null)  {
                Log.i("ouputInfo:", String.valueOf(index));
                Log.i("ouputInfo:",data);
                if (index >= 101 && index <= 112){
                    String[] split = data.split("         ");
                    LocationList.add(split[0]);
                    String PressureString = split[split.length-1];
                    if (!PressureString.contains("N/A")){
                        PressureList.add(Double.valueOf(PressureString));
                    }else{
                        PressureList.add(Double.valueOf("0.0"));
                    }
                }
                data=br.readLine();
                index = index +1;
            }
            // 释放资源
            br.close();
            read.close();
            input.close();
            httpUrlConn.disconnect();
            writeWebInfo2File(LocationList,PressureList,timeStamp);
            return PressureList;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Write the web info to the local file with format "stationName1, pressure1; ''' ,stationNameN,pressureN"
     * @param LocationList: name list of the station
     * @param PressureList: pressure list
     * @param timeStamp: operation time
     */
    private static void writeWebInfo2File(List<String> LocationList, List<Double> PressureList, String timeStamp){
        String context = "";
        for (int i = 0;i<LocationList.size();i++){
            String location = LocationList.get(i);
            Double pressure = PressureList.get(i);
            if (i < LocationList.size()-1){
                context = context + location + "," + pressure + ";";
            }else{
                context = context + location + "," + pressure;
            }

        }
        context = context + "\n";
        //String fileName = timeStamp + "_Pressure_Data_Fromweb.txt";
        String fileName = "Pressure_Data_Fromweb.txt";
        WriteFile.writeTxtToFiles("",fileName,context);
    }

    /**
     * Estabish the DT based the pointset
     * @param pointSet: input pointset
     * @return: DT
     */
    static DelaunayTriangulator estibish2DDT(List<Vector2D> pointSet){
        DelaunayTriangulator delaunayTriangulator;
        List<PointType> locationinfor = getStartionLocationList();
        delaunayTriangulator = new DelaunayTriangulator(pointSet);
        try {
            delaunayTriangulator.triangulate();
        } catch (NotEnoughPointsException e1) {
        }
        return delaunayTriangulator;
    }
}
