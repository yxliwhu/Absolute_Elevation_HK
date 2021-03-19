package com.example.absolute_elevation_hk;

import java.io.IOException;


public class Transformate {
    public static void main(String[] args) throws IOException {
    }
}

// Just for holding the return value from function "Transform"
// For holding Geographical coordinates Data (WGS84 and Hayford)

class GeoData {
    public double phi;
    public double flam;

    public GeoData() {
        phi = 0;
        flam = 0;
    }

    double getPhi() {
        return phi;
    }

    double getFlam() {
        return flam;
    }

    void setPhi(double input) {
        phi = input;
    }

    void setFlam(double input) {
        flam = input;
    }
}

// Just for holding the return value from function "Transform"
// For holding Grid coordinates data (HK1980 Grid)

class GridData {
    public double n;
    public double e;

    public GridData() {
        n = 0;
        e = 0;
    }

    double getN() {
        return n;
    }

    double getE() {
        return e;
    }

    void setN(double input) {
        n = input;
    }

    void setE(double input) {
        e = input;
    }
}

// Just for holding the return value from "Transform"
// For holding Radius Data

class RadiusData {
    double rho;
    double rmu;

    public RadiusData() {
        rho = 0;
        rmu = 0;
    }
}


// !!! CODE: Do transformation between three systems:  WGS84, Hayford, HKGrid

class Transform {
    public double pi;

    public Transform() {
        pi = 3.14159265359;
    }

    /**COBVERT HK1980 GRID COORDINATES TO CEOGETIC COORDINTES
     *
     * @param IG: 1 FOR HAYFORD SPHEROID, 2 FOR WGS84 SPHEROID
     * @param X: NORTHING (HK1980 DATAM)
     * @param Y: EASTING RETURN (HK1980 DATAM)
     * @param PHI: LATITUDE
     * @param FLAM: LONGITUDE
     * @return
     */
    public GeoData HKGEO(double IG, double X, double Y) {
        double RAD, PHI0, FLAM0;
        double a, b, C, D;
        double WX, WY, AA, BB, DPHI;
        double PHIF, DPH, SM, CR;
        double TPHI, TPHI2, TPHI4, TPHI6;
        double TT, TT2, TT3, TT4;
        double DUE, DX, CPHI1, CPHI2;
        double CPHI3, CPHI4, CLAM1, CLAM2;
        double CLAM3, CLAM4, DY, DX2;
        double RHO, RMU;
        double FLAM;
        double PHI;

        GeoData Result = new GeoData();
        RadiusData RadiusResult = new RadiusData();

        RAD = pi / 180;

        if (IG == 1) {
            PHI0 = (22 + (double) 18 / 60 + 43.68 / (double) 3600) * RAD;
            FLAM0 = (114 + (double) 10 / 60 + 42.8 / (double) 3600) * RAD;
        } else {
            PHI0 = (22 + (double) 18 / 60 + 38.17 / (double) 3600) * RAD;
            FLAM0 = (114 + (double) 10 / 60 + 51.65 / (double) 3600) * RAD;

            // --- TRANSFORM HK 1980 GRID TO WGS84 GRID
            a = 1.0000001619;
            b = 0.000027858;
            C = 23.098979;
            D = -23.149125;
            WX = a * X - b * Y + C;
            WY = b * X + a * Y + D;
            X = WX;
            Y = WY;
        }

        // --- REMOVE FALSE GRID ORIGIN COORDINATES

        DX = X - 819069.8;
        DY = Y - 836694.05;

        // --- COMPUTE PROVISIONAL PHIF (APPROXIMATE)
        AA = 6.853561524;
        BB = 110736.3925;
        DPHI = ((Math.sqrt(DX * AA * 4 + Math.pow(BB, 2)) - BB) * 0.5 / AA)
                * RAD;
        PHIF = PHI0 + DPHI;
        DPH = 0;

        // --- EVALUATE PHIF, ITERATE UNTIL CR IS NEAR ZERO
        do {
            PHIF = PHIF + DPH;
            SM = SMER(IG, PHI0, PHIF);
            CR = DX - SM;
            RadiusResult = RADIUS(IG, PHIF);
            RHO = RadiusResult.rho;
            RMU = RadiusResult.rmu;
            DPH = CR / RHO;
        } while (Math.abs(CR) >= 0.00001);

        // --- COMPUTE RADII
        RadiusResult = RADIUS(IG, PHIF);
        RHO = RadiusResult.rho;
        RMU = RadiusResult.rmu;
        TPHI = Math.tan(PHIF);
        TPHI2 = TPHI * TPHI;
        TPHI4 = TPHI2 * TPHI2;
        TPHI6 = TPHI2 * TPHI4;
        TT = RMU / RHO;
        TT2 = Math.pow(TT, 2);
        TT3 = Math.pow(TT, 3);
        TT4 = Math.pow(TT, 4);

        // --- COMPUTE LATITUDE
        DUE = DY;
        DX = DUE / RMU;
        DX2 = DX * DX;
        CPHI1 = DUE / RHO * DX * TPHI / 2;
        CPHI2 = CPHI1 / 12 * DX2* (9 * TT * (1 - TPHI2) - 4 * TT2 + 12 * TPHI2);
        CPHI3 = CPHI1/ 360* DX2* DX2* (8 * TT4 * (11 - 24 * TPHI2) - 12 * TT3 * (21 - 71 * TPHI2)+ 15 * TT2 * (15 - 98 * TPHI2 + 15 * TPHI4) + 180 * TT* (5 * TPHI2 - 3 * TPHI4) + 360 * TPHI4);
        CPHI4 = CPHI1 / 20160 * DX2 * DX2 * DX2* (1385 + 3633 * TPHI2 + 4095 * TPHI4 + 1575 * TPHI2 * TPHI4);
        PHI = PHIF - CPHI1 + CPHI2 - CPHI3 + CPHI4;

        // --- COMPUTE LONGITUDE
        CLAM1 = DX / Math.cos(PHIF);
        CLAM2 = CLAM1 * DX2 / 6 * (TT + 2 * TPHI2);
        CLAM3 = CLAM1* DX2* DX2/ 120* (TT2 * (9 - 68 * TPHI2) - 4 * TT3 * (1 - 6 * TPHI2) + 72 * TT* TPHI2 + 24 * TPHI4);
        CLAM4 = CLAM1 * DX2 * DX2 * DX2 / 5040* (61 + 662 * TPHI2 + 1320 * TPHI4 + 720 * TPHI2 * TPHI4);
        FLAM = FLAM0 + CLAM1 - CLAM2 + CLAM3 - CLAM4;

        // --- CONVERT TO DECIMAL DEGREES
        PHI = PHI / RAD;
        FLAM = FLAM / RAD;

        Result.setPhi(PHI);
        Result.setFlam(FLAM);
        return Result;
    }
    /**
     * CONVERT GEODETIC COORDINTES TO HK METRIC GRID COORDINATES
     * @param IG: 1 FOR HAYFORD SPHEROID, 2 FOR WG282 SPHEROID
     * @param PHI: LATITUDE IN DECIMAL DEGREES
     * @param FLAM: LONGITUDE IN DECIMAL DEGREES
     * @param X : NORTHING (HK 1980 METRIC DATAM)
     * @param Y : EASTING (HK 1980 METRIC DATAM)
     * @return
     */

    public GridData GEOHK(int IG, double PHI, double FLAM) {
        double RAD, PHI0, FLAM0, RPHI;
        double RLAM, SM0, SM1, CJ;
        double TPHI, TPHI2, TPHI4, TPHI6;
        double TT, TT2, TT3, TT4;
        double XF, X1, X2, X3, X4;
        double YF, Y1, Y2, Y3;
        double WX, WY, a, b, C, D;
        double RHO, RMU;
        double X, Y;
        GridData Result = new GridData();
        RadiusData RadiusResult = new RadiusData();

        RAD = pi / 180;

        // --- CONVERT PROJECTION ORIGIN TO RADIANS
        if (IG == 1) {
            PHI0 = ((double) 22 + (double) 18 / 60 + 43.68 / (double) 3600)
                    * RAD;
            FLAM0 = ((double) 114 + (double) 10 / 60 + 42.8 / (double) 3600)
                    * RAD;
        } else {
            PHI0 = ((double) 22 + (double) 18 / 60 + 38.17 / (double) 3600)
                    * RAD;
            FLAM0 = ((double) 114 + (double) 10 / 60 + 51.65 / (double) 3600)
                    * RAD;
        }

        // --- CONVERT LATITUDE AND LONGITUDE TO RADIANS

        RPHI = PHI * RAD;
        RLAM = FLAM * RAD;

        // --- COMPUTE MERIDIAN ARCS
        SM0 = SMER(IG, 0, PHI0);
        SM1 = SMER(IG, 0, RPHI);

        // --- COMPUTE RADII
        RadiusResult = RADIUS(IG, RPHI);
        RHO = RadiusResult.rho;
        RMU = RadiusResult.rmu;

        // --- COMPUTE CJ (IN RADIANS)
        CJ = (RLAM - FLAM0) * Math.cos(RPHI);
        TPHI = Math.tan(RPHI);
        TPHI2 = TPHI * TPHI;
        TPHI4 = TPHI2 * TPHI2;
        TPHI6 = TPHI2 * TPHI4;
        TT = RMU / RHO;
        TT2 = Math.pow(TT, 2);
        TT3 = Math.pow(TT, 3);
        TT4 = Math.pow(TT, 4);

        // --- COMPUTE NORTHING
        XF = SM1 - SM0;
        X1 = RMU / 2 * Math.pow(CJ, 2) * TPHI;
        X2 = X1 / 12 * Math.pow(CJ, 2) * (4 * TT2 + TT - TPHI2);
        X3 = X2
                / 30
                * Math.pow(CJ, 2)
                * (8 * TT4 * (11 - 24 * TPHI2) - 28 * TT3 * (1 - 6 * TPHI2)
                + TT2 * (1 - 32 * TPHI2) - 2 * TT * TPHI2 + TPHI4);
        X4 = X3 / 56 * Math.pow(CJ, 2)
                * (1385 - 3111 * TPHI2 + 543 * TPHI4 - TPHI6);
        X = XF + X1 + X2 + X3 + X4 + 819069.8;

        // --- COMPUTE EASTING
        YF = RMU * CJ;
        Y1 = YF / 6 * Math.pow(CJ, 2);
        Y2 = Y1 / 20 * Math.pow(CJ, 2);
        Y3 = Y2 / 42 * Math.pow(CJ, 2);
        Y1 = Y1 * (TT - TPHI2);
        Y2 = Y2* (4 * TT3 * (1 - 6 * TPHI2) + TT2 * (1 + 8 * TPHI2) - TT * 2
                * TPHI2 + TPHI4);
        Y3 = Y3 * (61 - 479 * TPHI2 + 179 * TPHI4 - TPHI6);
        Y = YF + Y1 + Y2 + Y3 + 836694.05;

        if (IG == 2) {
            WX = X;
            WY = Y;

            // --- TRANSFROM WGS84 GRID TO HK 1980 GRID
            a = 0.9999998373;
            b = -0.000027858;
            C = -23.098331;
            D = 23.149765;
            X = a * WX - b * WY + C;
            Y = b * WX + a * WY + D;
        }

        Result.setN(X);
        Result.setE(Y);
        return Result;

    } // --- GEOHK
    /**
     * COMPUTE MERIDIAN ARC
     * @param IG: 1 FOR HAYFORD SPHEROID, 2 FOR WG282 SPHEROID
     * @param PHI0: LATITUDE OF ORIGIN
     * @param PHIF: LATITUDE OF PROJECTION TO CENTRAL MERIDIAN
     * @param SMER: MERIDIAN ARC
     * @return
     */

    private double SMER(double IG, double PHI0, double PHIF) {
        double AXISM, FLAT, ECC;
        double a, b, C, D, DP0;
        double DPO, DP2;
        double DP4, DP6;
        double SMER;

        if (IG == 1) {
            AXISM = (double) 6378388;
            FLAT = (double) 1 / 297;
        } else {
            AXISM = (double) 6378137;
            FLAT = (double) 1 / 298.2572235634;
        }

        ECC = (double) 2 * FLAT - Math.pow(FLAT, 2);
        ECC = Math.sqrt(ECC);
        a = 1 + 3 / (double) 4 * Math.pow(ECC, 2) + 45 / (double) 64
                * Math.pow(ECC, 4) + 175 / (double) 256 * Math.pow(ECC, 6);
        b = 3 / (double) 4 * Math.pow(ECC, 2) + 15 / (double) 16
                * Math.pow(ECC, 4) + 525 / (double) 512 * Math.pow(ECC, 6);
        C = 15 / (double) 64 * Math.pow(ECC, 4) + 105 / (double) 256
                * Math.pow(ECC, 6);
        D = 35 / (double) 512 * Math.pow(ECC, 6);
        DP0 = PHIF - PHI0;
        DP2 = Math.sin(2 * PHIF) - Math.sin(2 * PHI0);
        DP4 = Math.sin(4 * PHIF) - Math.sin(4 * PHI0);
        DP6 = Math.sin(6 * PHIF) - Math.sin(6 * PHI0);
        SMER = AXISM * (1 - Math.pow(ECC, 2));
        SMER = SMER * (a * DP0 - b * DP2 / 2 + C * DP4 / 4 - D * DP6 / 6);

        return SMER;
    } // --- SMER

    /**
     * COMPUTE RADII OF CURVATURE OF A GIVEN LATITUDE
     * @param IG: 1 FOR HAYFORD SPHEROID, 2 FOR WG282 SPHEROID
     * @param PHI: LATITUDE
     * @param RHO : RADIUS OF MERIDIAN
     * @param PMU : RADIUS OF PRIME VERTICAL
     * @return
     */
    private RadiusData RADIUS(double IG, double PHI) {
        double AXISM, FLAT, ECC;
        double FAC;
        double RHO, RMU;

        RadiusData RadiusResult = new RadiusData();

        if (IG == 1) {
            AXISM = 6378388;
            FLAT = 1 / (double) 297;
        } else {
            AXISM = 6378137;
            FLAT = 1 / 298.2572235634;
        }
        ECC = 2 * FLAT - Math.pow(FLAT, 2);
        FAC = 1 - ECC * (Math.pow(Math.sin(PHI), 2));
        RHO = (double) AXISM * (1 - ECC) / Math.pow(FAC, 1.5);
        RMU = AXISM / Math.sqrt(FAC);

        RadiusResult.rho = RHO;
        RadiusResult.rmu = RMU;
        return RadiusResult;
    } // --- Radius

}