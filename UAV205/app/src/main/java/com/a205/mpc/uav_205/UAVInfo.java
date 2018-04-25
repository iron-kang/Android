package com.a205.mpc.uav_205;

public class UAVInfo {
    byte status;
    float bat;
    int   height;
    float latitude;
    float longitude;
    float altitude;

    public UAVInfo(byte[] info, int offset)
    {
        status    = info[offset];
        bat       = byte2float(info, 4+offset);
        height    = byte2int(info, 8+offset);
        latitude  = byte2float(info, 12+offset);
        longitude = byte2float(info, 16+offset);
        altitude  = byte2float(info, 20+offset);
    }

    private float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

    private int byte2int(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return l;
    }
}
