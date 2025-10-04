package com.example.sighner;

import java.security.SecureRandom;


final class UInt64 {
    private UInt64() {} // static only

    static void xor16(short[] a, short[] b, short[] out) {
        for (short i = 0; i < 4; i++) out[i] = (short)(a[i] ^ b[i]);
    }

    static void and16(short[] a, short[] b, short[] out) {
        for (short i = 0; i < 4; i++) out[i] = (short)(a[i] & b[i]);
    }

    static void not16(short[] a, short[] out) {
        for (short i = 0; i < 4; i++) out[i] = (short)(~a[i]);
    }


    // out = a + b (in-place allowed: out may be a or b)
    // shr: logical right shift by n (0<=n<64)
    static void shr16(short[] x, int n, short[] out) {
    	if (n == 0) { System.arraycopy(x, 0, out, 0, 4); return; }

    	if (n == 0) { System.arraycopy(x, 0, out, 0, 4); return; }

        // intで一時的に64bit値に組み立て
        int hi = ((x[0] & 0xFFFF) << 16) | (x[1] & 0xFFFF);
        int lo = ((x[2] & 0xFFFF) << 16) | (x[3] & 0xFFFF);

        int outHi, outLo;

        if (n < 32) {
            outLo = (lo >>> n) | (hi << (32 - n));
            outHi = hi >>> n;
        } else {
            outLo = hi >>> (n - 32);
            outHi = 0;
        }

        // int2 -> short4
        out[0] = (short)(outHi >>> 16);
        out[1] = (short)(outHi & 0xFFFF);
        out[2] = (short)(outLo >>> 16);
        out[3] = (short)(outLo & 0xFFFF);
    }
    static void shl16(short[] x, int n, short[] out) {
        if (n == 0) { System.arraycopy(x, 0, out, 0, 4); return; }

        int hi = ((x[0] & 0xFFFF) << 16) | (x[1] & 0xFFFF);
        int lo = ((x[2] & 0xFFFF) << 16) | (x[3] & 0xFFFF);
        int outHi, outLo;

        if (n < 32) {
            outHi = (hi << n) | (lo >>> (32 - n));
            outLo = lo << n;
        } else {
            outHi = lo << (n - 32);
            outLo = 0;
        }

        out[0] = (short)(outHi >>> 16);
        out[1] = (short)(outHi & 0xFFFF);
        out[2] = (short)(outLo >>> 16);
        out[3] = (short)(outLo & 0xFFFF);
    }

    static void rotr16(short[] x, int n, short[] out) {
    	if (n == 0) {
            System.arraycopy(x, 0, out, 0, 4);
            return;
        }

        short[] t1 = new short[4];
        short[] t2 = new short[4];

        shr16(x, n, t1);
        shl16(x, 64 - n, t2);

        for (int i = 0; i < 4; i++) {
            out[i] = (short)((t1[i] & 0xFFFF) | (t2[i] & 0xFFFF));
        }
    }
}


final class Matrix {
    short defrow;
	short defcol;
	short[] data = new short[(short)(defrow * defcol)];
	
	Matrix(short row, short col) {
		this.defrow = row;
		this.defcol = col;
		this.data = new short[(short)(row * col)]; 
		
		
	}

    int get(short row, short col) {
        return data[(short)(row * defcol + col)];
    }

    void set(short row, short col, short value) {
        data[(short)(row * defcol + col)] = value;
    }
    
 // get row into int[2] (hi/lo)
    public void getRow(short row, short[] out) {
    	for(short i=0;i<defcol; i++) {
    		out[i] = data[(short)(row * defcol + i)];
    		}
    }
    public void setRow(short row, short[] in) {
    	for(short i = 0; i<defcol; i++) {
			data[(short)(row * defcol + i)] = in[i];
		}
    }
 
    //a[i+j] = b[i]; => a.copyRowFrom(i+j,i,b);
    
    void copyRowFrom(short dstRow, short srcRow, Matrix src) {
    		short[]set = new short[defcol];    
    		src.getRow(srcRow, set);
        this.setRow(dstRow, set);
    }
    
    void clear() {
        for (int i = 0; i < data.length; i++) data[i] = 0;
    }
}

public class TweetNacl {
		
	private final static byte iv[] = {
			0x6a,0x09,(byte) 0xe6,0x67,(byte) 0xf3,(byte) 0xbc,(byte) 0xc9,0x08,
			(byte) 0xbb,0x67,(byte) 0xae,(byte) 0x85,(byte) 0x84,(byte) 0xca,(byte) 0xa7,0x3b,
			0x3c,0x6e,(byte) 0xf3,0x72,(byte) 0xfe,(byte) 0x94,(byte) 0xf8,0x2b,
			(byte) 0xa5,0x4f,(byte) 0xf5,0x3a,0x5f,0x1d,0x36,(byte) 0xf1,
			0x51,0x0e,0x52,0x7f,(byte) 0xad,(byte) 0xe6,(byte) 0x82,(byte) 0xd1,
			(byte) 0x9b,0x05,0x68,(byte) 0x8c,0x2b,0x3e,0x6c,0x1f,
			0x1f,(byte) 0x83,(byte) 0xd9,(byte) 0xab,(byte) 0xfb,0x41,(byte) 0xbd,0x6b,
			0x5b,(byte) 0xe0,(byte) 0xcd,0x19,0x13,0x7e,0x21,0x79
	} ;
	private static final short [] D2v = {
			0x0000,0x0000,0x0000, (short)0xf159,0x0000,0x0000, 0x0000, (short)0x26b2,
			0x0000,0x0000, 0x0000, (short)0x9b94,0x0000,0x0000, 0x0000, (short)0xebd6, 
			0x0000,0x0000,0x0000, (short)0xb156,0x0000,0x0000, 0x0000, (short)0x8283,
			0x0000,0x0000, 0x0000, (short)0x149a,0x0000,0x0000, 0x0000, (short)0x00e0,
			0x0000,0x0000,0x0000, (short)0xd130,0x0000,0x0000, 0x0000, (short)0xeef3,
			0x0000,0x0000, 0x0000, (short)0x80f2,0x0000,0x0000, 0x0000, (short)0x198e,
			0x0000,0x0000,0x0000, (short)0xfce7,0x0000,0x0000, 0x0000, (short)0x56df,
			0x0000,0x0000, 0x0000, (short)0xd9dc,0x0000,0x0000, 0x0000, (short)0x2406
	};
	// X と Y を hi/lo に分けて Matrix にセット
		private static Matrix D2_MATRIX= new Matrix((short)16,(short)4);
		
	
	private static final short []  Xv =  {
			0x0000,0x0000,0x0000, (short)0xd51a,0x0000,0x0000, 0x0000, (short)0x8f25,
			0x0000,0x0000,0x0000, (short)0x2d60,0x0000,0x0000, 0x0000, (short)0xc956, 
			0x0000,0x0000,0x0000, (short)0xa7b2,0x0000,0x0000, 0x0000, (short)0x9525,
			0x0000,0x0000,0x0000, (short)0xc760,0x0000,0x0000, 0x0000, (short)0x692c,
			0x0000,0x0000,0x0000, (short)0xdc5c,0x0000,0x0000, 0x0000, (short)0xfdd6,
			0x0000,0x0000,0x0000, (short)0xe231,0x0000,0x0000, 0x0000, (short)0xc0a4, 
			0x0000,0x0000,0x0000, (short)0x53fe,0x0000,0x0000, 0x0000, (short)0xcd6e,
			0x0000,0x0000,0x0000, (short)0x36d3,0x0000,0x0000, 0x0000, (short)0x2169
	};
	
	private static final short []  Yv =  {
			0x0000,0x0000,0x0000, 0x6658,0x0000,0x0000, 0x0000, 0x6666,
			0x0000,0x0000, 0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666, 
			0x0000,0x0000,0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666,
			0x0000,0x0000, 0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666,
			0x0000,0x0000,0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666,
			0x0000,0x0000, 0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666,
			0x0000,0x0000,0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666,
			0x0000,0x0000, 0x0000, 0x6666,0x0000,0x0000, 0x0000, 0x6666
	};
	
	// X と Y を hi/lo に分けて Matrix にセット
	private static Matrix X_MATRIX= new Matrix((short)16,(short)4);
	private static Matrix Y_MATRIX= new Matrix((short)16,(short)4);
	
	// X と Y を hi/lo に分けて Matrix にセット
	private static Matrix gf0_MATRIX= new Matrix((short)16,(short)4);
	private static Matrix gf1_MATRIX= new Matrix((short)16,(short)4);
	
	private static short[] Kv = {
			0x428a,0x2f98, (short) 0xd728,(short) 0xae22, 0x7137,0x4491, 0x23ef,0x65cd,
			(short) 0xb5c0,(short)0xfbcf, (short)0xec4d,0x3b2f, (short) 0xe9b5,(short)0xdba5, (short) 0x8189,(short) 0xdbbc,
			0x3956,(short)0xc25b, (short)0xf348,(short)0xb538, 0x59f1,0x11f1, (short)0xb605,(short)0xd019,
			(short)0x923f,(short)0x82a4, (short)0xaf19,0x4f9b, (short)0xab1c,0x5ed5, (short)0xda6d,(short)0x8118,
			(short)0xd807,(short)0xaa98, (short)0xa303,0x0242, 0x1283,0x5b01, 0x4570,0x6fbe,
			0x2431,(short)0x85be, 0x4ee4,(short)0xb28c, 0x550c,0x7dc3, (short)0xd5ff,(short)0xb4e2,
			0x72be,0x5d74, (short)0xf27b,(short)0x896f, (short)0x80de,(short)0xb1fe, 0x3b16,(short)0x96b1,
			(short)0x9bdc,0x06a7, 0x25c7,0x1235, (short)0xc19b,(short)0xf174, (short)0xcf69,0x2694,
			(short)0xe49b,0x69c1, (short)0x9ef1,0x4ad2, (short)0xefbe,0x4786, 0x384f,0x25e3,
			0x0fc1,(short)0x9dc6, (short)0x8b8c,(short)0xd5b5, 0x240c,(short)0xa1cc, 0x77ac,(short)0x9c65,
			0x2de9,0x2c6f, 0x592b,0x0275, 0x4a74,(short)0x84aa, 0x6ea6,(short)0xe483,
			0x5cb0,(short)0xa9dc, (short)0xbd41,(short)0xfbd4, 0x76f9,(short)0x88da, (short)0x8311,0x53b5,
			(short)0x983e,0x5152, (short)0xee66,(short)0xdfab, (short)0xa831,(short)0xc66d, 0x2db4,0x3210,
			(short)0xb003,0x27c8, (short)0x98fb,0x213f, (short)0xbf59,0x7fc7, (short)0xbeef,0x0ee4,
			(short)0xc6e0,0x0bf3, 0x3da8,(short)0x8fc2, (short)0xd5a7,(short)0x9147, (short)0x930a,(short)0xa725,
			0x06ca,0x6351, (short)0xe003,(short)0x826f, 0x1429,0x2967, 0x0a0e,0x6e70,
			0x27b7,0x0a85, 0x46d2,0x2ffc, 0x2e1b,0x2138, 0x5c26,(short)0xc926,
			0x4d2c,0x6dfc, 0x5ac4,0x2aed, 0x5338,0x0d13, (short)0x9d95,(short)0xb3df,
			0x650a,0x7354, (short)0x8baf,0x63de, 0x766a,0x0abb, 0x3c77,(short)0xb2a8,
			(short)0x81c2,(short)0xc92e, 0x47ed,(short)0xaee6, (short)0x9272,0x2c85, 0x1482,0x353b,
			(short)0xa2bf,(short)0xe8a1, 0x4cf1,0x0364, (short)0xa81a,0x664b, (short)0xbc42,0x3001,
			(short)0xc24b,(short)0x8b70, (short)0xd0f8,(short)0x9791, (short)0xc76c,0x51a3, 0x0654,(short)0xbe30,
			(short)0xd192,(short)0xe819, (short)0xd6ef,0x5218, (short)0xd699,0x0624, 0x5565,(short)0xa910,
			(short)0xf40e,0x3585, 0x5771,0x202a, 0x106a,(short)0xa070, 0x32bb,(short)0xd1b8,
			0x19a4,(short)0xc116, (short)0xb8d2,(short)0xd0c8, 0x1e37,0x6c08, 0x5141,(short)0xab53,
			0x2748,0x774c, (short)0xdf8e,(short)0xeb99, 0x34b0,(short)0xbcb5, (short)0xe19b,0x48a8,
			0x391c,0x0cb3, (short)0xc5c9,0x5a63, 0x4ed8,(short)0xaa4a, (short)0xe341,(short)0x8acb,
			0x5b9c,(short)0xca4f, 0x7763,(short)0xe373, 0x682e,0x6ff3, (short)0xd6b2,(short)0xb8a3,
			0x748f,(short)0x82ee, 0x5def,(short)0xb2fc, 0x78a5,0x636f, 0x4317,0x2f60,
			(short)0x84c8,0x7814, (short)0xa1f0,(short)0xab72, (short)0x8cc7,0x0208, 0x1a64,0x39ec,
			(short)0x90be,(short)0xfffa, 0x2363,0x1e28, (short)0xa450,0x6ceb, (short)0xde82,(short)0xbde9,
			(short)0xbef9,(short)0xa3f7, (short)0xb2c6,0x7915, (short)0xc671,0x78f2, (short)0xe372,0x532b,
			(short)0xca27,0x3ece, (short)0xea26,0x619c, (short)0xd186,(short)0xb8c7, 0x21c0,(short)0xc207,
			(short)0xeada,0x7dd6, (short)0xcde0,(short)0xeb1e, (short)0xf57d,0x4f7f, (short)0xee6e,(short)0xd178,
		    0x06f0,0x67aa, 0x7217,0x6fba, 0x0a63,0x7dc5, (short)0xa2c8,(short)0x98a6,
		    0x113f,(short)0x9804, (short)0xbef9,0x0dae, 0x1b71,0x0b35, 0x131c,0x471b,
		    0x28db,0x77f5, 0x2304,0x7d84, 0x32ca,(short)0xab7b, 0x40c7,0x2493,
		    0x3c9e,(short)0xbe0a, 0x15c9,(short)0xbebc, 0x431d,0x67c4, (short)0x9c10,0x0d4c,
		    0x4cc5,(short)0xd4be, (short)0xcb3e,0x42b6, 0x597f,0x299c, (short)0xfc65,0x7e2a,
		    0x5fcb,0x6fab, 0x3ad6,(short)0xfaec, 0x6c44,0x198c, 0x4a47,0x5817
	};
	
	// Matrixにセット
	private static Matrix K= new Matrix((short)80,(short)4);

	static void dl64(byte[] x, int off, short[] out) {
		// 上位32bit
	    int hi = 0;
	    for (int i = 0; i < 4; i++) {
	        hi = (hi << 8) | (x[off + i] & 0xFF);
	    }
	    // 下位32bit
	    int lo = 0;
	    for (int i = 4; i < 8; i++) {
	        lo = (lo << 8) | (x[off + i] & 0xFF);
	    }
	    // 16bitごとに分割（ビッグエンディアン順）
	    out[0] = (short)((hi >>> 16) & 0xFFFF); // hi-hi
	    out[1] = (short)(hi & 0xFFFF);          // hi-lo
	    out[2] = (short)((lo >>> 16) & 0xFFFF); // lo-hi
	    out[3] = (short)(lo & 0xFFFF);          // lo-lo
	}

	static void ts64(byte[] x, int off, Matrix uMatrix) { 
	    short[] uRow = new short[4];
	    uMatrix.getRow((short)0, uRow);

	    short hi1 = uRow[0];
	    short hi2 = uRow[1];
	    short lo1 = uRow[2];
	    short lo2 = uRow[3];

	    // リトルエンディアンで書き込み（オリジナルと同じ順）
	    x[(short)(off+7)] = (byte)(lo2 & 0xff);
	    x[(short)(off+6)] = (byte)((lo2 >>> 8) & 0xff);
	    x[(short)(off+5)] = (byte)(lo1 & 0xff);
	    x[(short)(off+4)] = (byte)((lo1 >>> 8) & 0xff);
	    x[(short)(off+3)] = (byte)(hi2 & 0xff);
	    x[(short)(off+2)] = (byte)((hi2 >>> 8) & 0xff);
	    x[(short)(off+1)] = (byte)(hi1 & 0xff);
	    x[(short)(off+0)] = (byte)((hi1 >>> 8) & 0xff);
	}
	
	// x, y, z は hi/lo 2要素の int[]
	static void Ch(short[] x, short[] y, short[] z, short[] out) {
	    short[] nx = new short[4];
	    not64(x, nx); // ~x (hi/lo同時に反転)

	    out[0] = (short)((x[0] & y[0]) ^ (nx[0] & z[0]));
	    out[1] = (short)((x[1] & y[1]) ^ (nx[1] & z[1]));
	    out[2] = (short)((x[2] & y[2]) ^ (nx[2] & z[2]));
	    out[3] = (short)((x[3] & y[3]) ^ (nx[3] & z[3]));
	}

	static void not64(short[] x, short[] out) {
	    out[0] = (short)~x[0];
	    out[1] = (short)~x[1];
	    out[2] = (short)~x[2];
	    out[3] = (short)~x[3];
	}
	static void Maj(short[] x, short[] y, short[] z, short[] out) {
	    // hi1
	    out[0] = (short)((x[0] & y[0]) ^ (x[0] & z[0]) ^ (y[0] & z[0]));
	    // hi2
	    out[1] = (short)((x[1] & y[1]) ^ (x[1] & z[1]) ^ (y[1] & z[1]));
	    //lo1
	    out[2] = (short)((x[2] & y[2]) ^ (x[2] & z[2]) ^ (y[2] & z[2]));
	    //lo2
	    out[3] = (short)((x[3] & y[3]) ^ (x[3] & z[3]) ^ (y[3] & z[3]));
	}
	
	static void Sigma0(short[] x, short[] out) {
	    short[] r28 = new short[4];
	    short[] r34 = new short[4];
	    short[] r39 = new short[4];

	    UInt64.rotr16(x, 28, r28);
	    UInt64.rotr16(x, 34, r34);
	    UInt64.rotr16(x, 39, r39);

	    out[0] = (short)(r28[0] ^ r34[0] ^ r39[0]);
	    out[1] = (short)(r28[1] ^ r34[1] ^ r39[1]);
	    out[2] = (short)(r28[2] ^ r34[2] ^ r39[2]);
	    out[3] = (short)(r28[3] ^ r34[3] ^ r39[3]);
	}
	static void Sigma1(short[] x, short[] out) {
	    short[] r14 = new short[4];
	    short[] r18 = new short[4];
	    short[] r41 = new short[4];

	    UInt64.rotr16(x, 14, r14);
	    UInt64.rotr16(x, 18, r18);
	    UInt64.rotr16(x, 41, r41);

	    out[0] = (short)(r14[0] ^ r18[0] ^ r41[0]);
	    out[1] = (short)(r14[1] ^ r18[1] ^ r41[1]);
	    out[2] = (short)(r14[2] ^ r18[2] ^ r41[2]);
	    out[3] = (short)(r14[3] ^ r18[3] ^ r41[3]);
	}
	// σ0(x) = ROTR(x,1) ^ ROTR(x,8) ^ SHR(x,7)
	static void sigma0(short[] x, short[] out) {
	    short[] r1  = new short[4];
	    short[] r8  = new short[4];
	    short[] s7  = new short[4];

	    UInt64.rotr16(x, 1, r1);
	    UInt64.rotr16(x, 8, r8);
	    UInt64.shr16(x, 7, s7);  // x >>> 7

	    out[0] = (short)(r1[0] ^ r8[0] ^ s7[0]);
	    out[1] = (short)(r1[1] ^ r8[1] ^ s7[1]);
	    out[2] = (short)(r1[2] ^ r8[2] ^ s7[2]);
	    out[3] = (short)(r1[3] ^ r8[3] ^ s7[3]);
	}

	// σ1(x) = ROTR(x,19) ^ ROTR(x,61) ^ SHR(x,6)
	static void sigma1(short[] x, short[] out) {
	    short[] r19 = new short[4];
	    short[] r61 = new short[4];
	    short[] s6  = new short[4];

	    UInt64.rotr16(x, 19, r19);
	    UInt64.rotr16(x, 61, r61);
	    UInt64.shr16(x, 6, s6);  // x >>> 6

	    out[0] = (short)(r19[0] ^ r61[0] ^ s6[0]);
	    out[1] = (short)(r19[1] ^ r61[1] ^ s6[1]);
	    out[2] = (short)(r19[2] ^ r61[2] ^ s6[2]);
	    out[3] = (short)(r19[3] ^ r61[3] ^ s6[3]);
	}
	
	void clear(short[] in){
		for(short i=0; i<(short)(in.length);i++) {
			in[i]=0;
		}
	}
	void clear(int[] in){
		for(short i=0; i<(short)(in.length);i++) {
			in[i]=0;
		}
	}
	
	public TweetNacl() {
		
		for(short i =0; i<16*4; i++) {
			D2_MATRIX.data[i] = D2v[i];
			X_MATRIX.data[i] = Xv[i];
			Y_MATRIX.data[i] = Yv[i];
			gf0_MATRIX.data[i] = 0;
			gf1_MATRIX.data[i] = 0;
			if(i==1) {
				gf1_MATRIX.data[i] = 1;
			}
		}
		for(short i=0; i<80*4 ; i++) {
			K.data[i]=Kv[i];
		}
	}
	
	
	
	//keyPair
	
	short rows = 16;   // limb 数
	short cols = 4;    // hi/lo
	Matrix p0 = new Matrix(rows, cols);
	Matrix p1 = new Matrix(rows, cols);
	Matrix p2 = new Matrix(rows, cols);
	Matrix p3 = new Matrix(rows, cols);
	public int  crypto_sign_keypair(byte [] pk, byte [] sk, boolean seeded) {
		byte [] d = new byte[64];
		p0.clear();
		p1.clear();
		p2.clear();
		p3.clear();
		
		Matrix[] pMatrices = new Matrix[4];
		pMatrices[0] = p0;
		pMatrices[1] = p1;
		pMatrices[2] = p2;
		pMatrices[3] = p3;


		if (!seeded) RandomCompat(sk, (short)32);
		crypto_hash(d, sk,0, 32, 32);
		
		
		d[0] &= 248;
		d[31] &= 127;
		d[31] |= 64;
		scalarbase(pMatrices, d,0,d.length);
		
		pack(pk, pMatrices);

		for (short i = 0; i < 32; i++) sk[(short)(i+32)] = pk[i];
		
		return 0;
		
	}
	
	public static void RandomCompat(byte[] x, short len) {
	    final SecureRandom sr = new SecureRandom();


	    byte[] tmp = new byte[len];
        sr.nextBytes(tmp);
        System.arraycopy(tmp, 0, x, (short)0, len);
	}
	
	
	//sign
	Matrix x = new Matrix((short)64,(short)4);
	public int crypto_sign(byte [] sm, short dummy, byte [] m, int n, byte [] sk)
	{
		byte[] d = new byte[64], h = new byte[64], r = new byte[64];

		short i, j;
		p0.clear();
		p1.clear();
		p2.clear();
		p3.clear();
		
		Matrix[] pMatrices = new Matrix[4];
		pMatrices[0] = p0;
		pMatrices[1] = p1;
		pMatrices[2] = p2;
		pMatrices[3] = p3;


		crypto_hash(d, sk,0,sk.length, 32);
		d[0] &= 248;
		d[31] &= 127;
		d[31] |= 64;

		///*smlen = n+64;

		for (i = 0; i < n; i ++) sm[(short)(64 + i)] = m[i];
		
		for (i = 0; i < 32; i ++) sm[(short)(32 + i)] = d[(short)(32 + i)];

		crypto_hash(r, sm,32,sm.length-32, n+32);
		
		reduce(r);
		System.out.println("r:"+util.HexUtil.byteArrayToHexString(r));
		
		scalarbase(pMatrices, r,0,r.length);
		pack(sm,pMatrices);

		for (i = 0; i < 32; i ++) sm[(short)(i+32)] = sk[(short)(i+32)];
		crypto_hash(h, sm,0,sm.length, n + 64);
		reduce(h);
		short[]xi  = new short[4];
		for (i = 0; i < 64; i ++) {
			
			x.getRow(i, xi);
			xi[0] = 0;
			xi[1] = 0;
			xi[2] = 0;
			xi[3] = 0;
			x.setRow(i, xi);
		}
		
		for (i = 0; i < 32; i ++) {
			x.getRow(i, xi);
			xi[3] = (short)(r[i] & 0xff);  // 下位8bitをxi[0]に
			xi[2] = 0;
			xi[1] = 0;
			xi[0] = 0;             // 上位16bitは0に
			x.setRow((short)i, xi); // 必要ならMatrixに戻す
		}
		short[] xij = new short[4];
		for (i = 0; i < 32; i ++) {
			for (j = 0; j < 32; j ++) {
				int hi = 0;
		        int lo = (h[i] & 0xff) * (d[j] & 0xff); // 0..65025なので32bit内に収まる
		        int[] tmp = new int[] {hi, lo};

		        
		        x.getRow((short)(i + j), xij);

		        // hi/lo に分けて加算
		        addSignedToRow(xij, tmp[1]); // lo の加算、hi に桁上がり反映
		        // 必要なら hi も加算
		        xij[0] += tmp[0];

		        x.setRow((short)(i + j), xij);
			}
		}
		
		
		modL(sm,32,sm.length-32, x);
		
		return 0;

	}
	
	Matrix u = new Matrix((short)1,(short)4);
	short[] uRow = new short[4];
	private int crypto_hash(byte [] out, byte [] m,final int moff,final int mlen, int n)
	{
		byte[] h = new byte[64], x = new byte [256];
		short i;
		int b = n;
		
		for (i = 0; i < 64; i ++) h[i] = iv[i];
		
		crypto_hashblocks(h, m,moff,mlen, n);
		///m += n;
		n &= 127;
		///m -= n;

		for (i = 0; i < 256; i ++) x[i] = 0;
		
		for (i = 0; i < n; i ++) x[i] = m[(short)(i+moff)];
		x[(short)n] = (byte) 128;

		// パディング終端
		n = 256 - 128 * (n < 112 ? 1 : 0);

		// b >>> 61 は上位3bitだが n は小さいので hi = 0
		x[(short)(n-9)] = 0;  // Java Cardではlongが使えないため0でOK

		// b: バイト数
		int bitlen = b << 3;  // ここは32bitなので一旦intで計算

		short[] bitlen_parts = new short[4]; 
		// [hi1, hi2, lo1, lo2] の順に格納

		bitlen_parts[0] = 0;                     // 上位48-63bit
		bitlen_parts[1] = 0;                     // 上位32-47bit
		bitlen_parts[2] = (short)((bitlen >>> 16) & 0xFFFF); // 中位16bit
		bitlen_parts[3] = (short)(bitlen & 0xFFFF);          // 下位16bit
				
		u.setRow((short)0, bitlen_parts);

		// ts64 でリトルエンディアン書き込み
		ts64(x, n-8, u);
		crypto_hashblocks(h, x,0,x.length, n);
  		
		
		for (i = 0; i < 64; i ++) out[i] = h[i];

		return 0;
	}

	//suspicious
	// TBD... long length n
	///int crypto_hashblocks(byte [] x, byte [] m, long n)
	Matrix z_8 = new Matrix((short)8, cols);
	Matrix a_8 = new Matrix((short)8, cols);
	Matrix b_8 = new Matrix((short)8, cols);
	Matrix w = new Matrix((short)16, cols);
	
	short[] a0 = new short[cols];
	short[] a1 = new short[cols];
	short[] a2 = new short[cols];
	short[] b3 = new short[cols];
	short[] a4 = new short[cols];
	short[] a5 = new short[cols];
	short[] a6 = new short[cols];
	short[] a7 = new short[cols];
	
	short[] b7 = new short[cols];
	
	short[] tmp1 = new short[cols];
	short[] tmp2 = new short[cols];
	short[] tmp3 = new short[cols];

	
	short[] K_i = new short[cols];
	short[] w_i = new short[cols];
	short[] t64   = new short[cols];
	
	short[] w_jm15 = new short[cols];  // w[j-15]
    short[] w_jm2  = new short[cols];  // w[j-2]
    short[] w_jm16 = new short[cols];  // w[j-16]
    short[] w_jm7  = new short[cols];  // w[j-7]

    short[] s0 = new short[cols];
    short[] s1 = new short[cols];
	
	short[] wrow = new short[cols];
	short[] a_i = new short[cols];
	short[] z_i = new short[cols];
	private int crypto_hashblocks(byte [] x, byte [] m,final int moff,final int mlen, int n)
	{

			
			clear(tmp1);
			clear(tmp2);
			clear(tmp3);
			z_8.clear();
			a_8.clear();
			b_8.clear();
			w.clear();
			
			
			short i,j;
			
			for ( i = 0; i < 8; i++) {
			    dl64(x, (short)(8 * i), tmp1);
			    z_8.setRow(i, tmp1);
				a_8.setRow(i, tmp1);
			}
			int moffset = moff;
			
			
			
			while (n >= 128) {
				for (i = 0; i < 16; i++) {
				    dl64(m, (short)(8 * i + moffset), wrow);
				    w.setRow(i, wrow);
				}
				for (i = 0; i < 80; i ++) {
					for (j = 0; j < 8; j ++) {
						b_8.copyRowFrom(j,j,a_8);  // b[j] = a[j]
					}
					
					K.getRow(i, K_i);
					
					if (i >= 16) {

					    w.getRow((short)((i-15) % 16), w_jm15);
					    w.getRow((short)((i-2)  % 16), w_jm2);
					    w.getRow((short)((i-16) % 16), w_jm16);
					    w.getRow((short)((i-7)  % 16), w_jm7);

					    sigma0(w_jm15, s0);
					    sigma1(w_jm2, s1);

					    add64(w_jm16, s0, tmp1);
					    add64(tmp1, w_jm7, tmp1);
					    add64(tmp1, s1, tmp1);
					    w.setRow((short)(i % 16), tmp1);
					}
					// t = a[7] + Sigma1(a[4]) + Ch(a[4],a[5],a[6]) + K[i] + w[i%16];
					a_8.getRow((short)4, a4);
					a_8.getRow((short)5, a5);
					a_8.getRow((short)6, a6);
					a_8.getRow((short)7, a7);
					w.getRow((short)(i % 16), w_i);
				    Sigma1(a4, tmp1);        
				    Ch(a4, a5, a6, tmp2); 
				    
				    
				    add64(a7,tmp1,tmp1);      
				    add64(tmp2,K_i, tmp2);
				    add64(tmp1,tmp2,tmp3);
				    add64(tmp3,w_i,t64);
					
					a_8.getRow((short)0, a0);
					a_8.getRow((short)1, a1);
					a_8.getRow((short)2, a2);					

					// b[7] = t + Sigma0(a[0]) + Maj(a[0],a[1],a[2]);
					
					// Sigma0(a[0])
					Sigma0(a0, tmp1);

					// Maj(a[0], a[1], a[2])
					Maj(a0, a1, a2, tmp2);
					
					b_8.getRow((short)7, b7);
					
					add64(t64, tmp1, tmp3);
					add64(tmp3,tmp2,b7);
					b_8.setRow((short)7, b7);
					
					//b[3] += t;
					
					b_8.getRow((short)3, b3);	
					add64(b3,t64,b3);
					b_8.setRow((short)3,b3);
					
					
					for (j = 0; j < 8; j ++) {
						a_8.copyRowFrom((short)((j+1)%8),j, b_8);
						//a[i+j] = b[i]; => a.copyRowFrom(i+j,i,b);
					}
					
				}
				
				for (i = 0; i < 8; i ++) {
					a_8.getRow(i, a_i);
					z_8.getRow(i, z_i);
					add64(a_i,z_i,tmp1);//a[i] +=z[i];
					a_8.setRow(i, tmp1);
					//z[i] = a[i]
					z_8.setRow(i,tmp1);
					
				}

				moffset += 128;
				n -= 128;
			}

			
			for (i = 0; i < 8; i ++) {
				u.copyRowFrom((short)0,i, z_8);
				ts64(x,8*i, u);
			}
			return n;
	}
	
	// x + y -> out (64bit)
		static void add64(short[] x, short[] y, short[] out) {
			short carry = 0;

		    for (short i = (short)(x.length - 1); i >= 0; i--) {
		        int sum = (x[i] & 0xFFFF) + (y[i] & 0xFFFF) + carry;
		        out[i] = (short)(sum & 0xFFFF);
		        carry = (short)(sum >>> 16);
		    }
		}
	
	Matrix q0 = new Matrix(rows, cols);
	Matrix q1 = new Matrix(rows, cols);
	Matrix q2 = new Matrix(rows, cols);
	Matrix q3 = new Matrix(rows, cols);
	
	private void scalarbase(Matrix[] p, byte[] s,final int soff,final int slen)
	{
		q0.clear();
		q1.clear();
		q2.clear();
		q3.clear();		
		Matrix[] qMatrixes = new Matrix[4];
		qMatrixes[0] = q0;
		qMatrixes[1] = q1;
		qMatrixes[2] = q2;
		qMatrixes[3] = q3;
		

		set25519_M(qMatrixes[0],X_MATRIX);
		set25519_M(qMatrixes[1],Y_MATRIX);
		set25519_M(qMatrixes[2],gf1_MATRIX);
		M_Matrix_M(qMatrixes[3], X_MATRIX, Y_MATRIX);
		
		scalarmult(p,qMatrixes, s,soff,slen);
		
	}
	
	private static void set25519_M(Matrix r, Matrix a)
	{
	    for (short i = 0; i < 16; i++) {
	        r.copyRowFrom(i,i,a);        // hi/lo をそのまま r の i 行にセット
	    }
	}
		
	private void scalarmult(Matrix[] p, Matrix[] q, byte[] s,final int soff,final int slen)
	{
		
		set25519_M(p[0],gf0_MATRIX);
		set25519_M(p[1],gf1_MATRIX);
		set25519_M(p[2],gf1_MATRIX);
		set25519_M(p[3],gf0_MATRIX);
		
		//printMatrix("gf1",gf1_MATRIX);

		for (int i = 255;i >= 0;--i) {
			byte b = (byte) ((s[(short)(i/8+soff)] >> (i&7))&1);
			cswap(p,q,b);
			
			add(q,p);
			add(p,p);

			cswap(p,q,b);

		}
		///String dbgt = "";
		///for (int dbg = 0; dbg < p.length; dbg ++) for (int dd = 0; dd < p[dbg].length; dd ++) dbgt += " "+p[dbg][dd];
		///L/og.d(TAG, "scalarmult -> "+dbgt);
	}
	
	private void cswap(Matrix[] p, Matrix[] q, byte b)
	{

		for (short i = 0; i < 4; i ++) {

			sel25519Matrix(p[i],q[i],b);  
		}
			
	}
	Matrix a = new Matrix(rows,cols);
	Matrix b = new Matrix(rows,cols);
	Matrix c = new Matrix(rows,cols);
	Matrix d = new Matrix(rows,cols);
	Matrix t = new Matrix(rows,cols);
	Matrix e = new Matrix(rows,cols);
	Matrix f = new Matrix(rows,cols);
	Matrix g = new Matrix(rows,cols);
	Matrix h = new Matrix(rows,cols);

	private void add(Matrix[] p, Matrix[] q)
	{
		
		a.clear();
		b.clear();
		c.clear();
		d.clear();
		t.clear();
		e.clear();
		f.clear();
		g.clear();
		h.clear();
		
		
		Matrix p0 = p[0];
		Matrix p1 = p[1];
		Matrix p2 = p[2];
		Matrix p3 = p[3];
		
		Matrix q0 = q[0];
		Matrix q1 = q[1];
		Matrix q2 = q[2];
		Matrix q3 = q[3];

		
		Z_Matrix(a, p1, p0);
		Z_Matrix(t, q1, q0);
		
		M_Matrix_M(a,a,t);

		A_Matrix(b, p0, p1);

		A_Matrix(t, q0, q1);
		M_Matrix_M(b,b,t);

		M_Matrix_M(c, p3, q3);
		M_Matrix_M(c, c,  D2_MATRIX);
		M_Matrix_M(d, p2, q2);
		
		
		
		A_Matrix(d, d, d);
		Z_Matrix(e, b, a);
		Z_Matrix(f, d, c);
		A_Matrix(g, d, c);
		A_Matrix(h, b, a);

		M_Matrix_M(p0, e, f);
		
		M_Matrix_M(p1, h, g);
		M_Matrix_M(p2, g, f);
		
		M_Matrix_M(p3, e, h);
		
		
	
	
	}
	short[]ai = new short[4];
	
	short[]bi = new short[4];
	
	short[]oi = new short[4];
	// o = a - b
		private void Z_Matrix(Matrix o, Matrix a, Matrix b) {
			clear(ai);
			clear(bi);
			clear(oi);
			
			for (short i = 0; i < 16; i++) {
			    a.getRow(i, ai);
			    b.getRow(i, bi);

			    diffWithBorrow16(ai,bi,oi);
			    o.setRow(i, oi);
			}
			
		}
		
		public void diffWithBorrow16(short[] a, short[] b, short[] out) {
			int borrow = 0;

		    for (short i = 3; i >= 0; i--) { // lo→hi の順
		        int ai = a[i] & 0xFFFF;
		        int bi = (b[i] & 0xFFFF) + borrow;
		        int diff = ai - bi;

		        if (diff < 0) {
		            diff += 0x10000;
		            borrow = 1;
		        } else {
		            borrow = 0;
		        }
		        out[i] = (short)diff;
		    }
		}
		
		// o = a + b
		private void A_Matrix(Matrix o, Matrix a, Matrix b) {
		    clear(ai);
			clear(bi);
			clear(oi);
		    for (short i = 0; i < 16; i++) {
		    	a.getRow(i, ai);
			    b.getRow(i, bi);

			    sumWithCarry16(ai,bi,oi);
			    o.setRow(i, oi);
		    }    
		}
	
		public void sumWithCarry16(short[] a, short[] b, short[] out) {
			int carry = 0;

		    for (short i = 3; i >= 0; i--) { // lo → hi
		        int sum = (a[i] & 0xFFFF) + (b[i] & 0xFFFF) + carry;
		        if (sum >= 0x10000) {
		            sum -= 0x10000;
		            carry = 1;
		        } else {
		            carry = 0;
		        }
		        out[i] = (short)sum;
		    }

		}
	
	//sign
		
		Matrix x_64 = new Matrix((short)64,(short)4);	
	private void reduce(byte [] r)
	{
		clear(tmp1);
		short i;
		
		for (i = 0; i < 64; i ++) {
			tmp1[3] = (short)(r[i] & 0xff);  // 下位8bitをxi[0]に
			tmp1[0] = 0;             // 上位16bitは0に
			x_64.setRow((short)i, tmp1); // 必要ならMatrixに戻す
		}
		
		for (i = 0; i < 64; i ++) r[i] = 0;
		
		modL(r,0,r.length, x_64);
	}
	
	short[] xi = new short[4];
	short[] xj = new short[4];
	int[] carry = new int[2];
	short[] tmp = new short[4];
	private void modL(byte[] r,final int roff,final int rlen, Matrix x)
	{
		clear(xi);
		clear(xj);
		clear(carry);
		clear(tmp);
		short i, j;
		
		int tmp1,tmp2,tmp3,tmp4;
		
		int newHi,newLo,lo_1,hi,lo;

		for (i = 63;i >= 32;--i) {
			carry[0] = 0;
			carry[1] = 0;
			x.getRow((short)i, xi);
			for (j = (short)(i - 32);j < i - 12;++j) {
				x.getRow((short)j, xj);
				lo_1 = ((xi[2] & 0xFFFF) << 16) | (xi[3] & 0xFFFF);
				tmp1 = carry[1];
				tmp2 = -16;
				tmp3 = lo_1 * L[(short)(j - (i - 32))];
				tmp4 = tmp2*tmp3;
				addSignedToRow(xj, tmp1);
				addSignedToRow(xj, tmp4);
				
				// (xj[1] + 128) >> 8 を hi/lo で表現する

				// まず 128 を加える
				tmp[0] = xj[0];
				tmp[1] = xj[1];
				tmp[2] = xj[2];
				tmp[3] = xj[3];
				addSignedToRow(tmp, 128);

				// 上位32bit = x[0] << 16 | x[1]
			    hi = ((tmp[0] & 0xFFFF) << 16) | (tmp[1] & 0xFFFF);
			    // 下位32bit = x[2] << 16 | x[3]
			    lo = ((tmp[2] & 0xFFFF) << 16) | (tmp[3] & 0xFFFF);

			    // 算術右シフト 8bit
			    newHi = hi >> 8;                   // hi は符号拡張
			    newLo = (lo >>> 8) | ((hi & 0xFF) << 24); // lo は hi 下位8bitを上位に詰める
				
				// carry に代入
				carry[0] = newHi;
				carry[1] = newLo;
				
				int[] neg = new int[2];
				neg64(carry,neg);
			
				addSignedToRow(xj,(neg[1]<<8));
				
				x.setRow((short)j, xj);
				
			}
			x.getRow((short)j, xj);
			addSignedToRow(xj,carry[1]);
			
			x.setRow((short)j, xj);
			xi[3] = 0;
			xi[2] = 0;
			xi[1] = 0;
			xi[0] = 0;
			x.setRow((short)i, xi);
			
			
		}
		
		carry[0] = 0;
		carry[1] = 0;
		short[] x31 = new short[4];
		for (j = 0; j < 32; j ++) {
			x.getRow((short)j, xj);
			x.getRow((short)31, x31);
			lo_1 = ((x31[2] & 0xFFFF) << 16) | (x31[3] & 0xFFFF);
			tmp1 = carry[1];
			tmp2 = -(lo_1 >> 4) * L[(short)j];
			addSignedToRow(xj,tmp1);
			addSignedToRow(xj,tmp2);
			
			
			// 上位32bit = x[0] << 16 | x[1]
		    hi = ((xj[0] & 0xFFFF) << 16) | (xj[1] & 0xFFFF);
		    // 下位32bit = x[2] << 16 | x[3]
		    lo = ((xj[2] & 0xFFFF) << 16) | (xj[3] & 0xFFFF);

		    // 算術右シフト 8bit
		    newHi = hi >> 8;                   // hi は符号拡張
		    newLo = (lo >>> 8) | ((hi & 0xFF) << 24); // lo は hi 下位8bitを上位に詰める
			
			// carry に代入
			carry[0] = newHi;
			carry[1] = newLo;

			
			xj[0] = 0;
			xj[1] = 0;
			xj[2] = 0;
			xj[3] &=0x00FF;
			
			x.setRow((short)j, xj);
		}
		
		for (j = 0; j < 32; j ++) {
			x.getRow((short)j, xj);
			tmp1 = -carry[1] * L[(short)j];
			addSignedToRow(xj,tmp1);
			x.setRow((short)j, xj);
		}
		
		short[] xi1 = new short[4];
		for (i = 0; i < 32; i ++) {
			x.getRow((short)i, xi);
			x.getRow((short)(i+1), xi1);
			
			// 上位32bit = x[0] << 16 | x[1]
		    hi = ((xi[0] & 0xFFFF) << 16) | (xi[1] & 0xFFFF);
		    // 下位32bit = x[2] << 16 | x[3]
		    lo = ((xi[2] & 0xFFFF) << 16) | (xi[3] & 0xFFFF);

		    // 算術右シフト 8bit
		    newHi = hi >> 8;                   // hi は符号拡張
		    newLo = (lo >>> 8) | ((hi & 0xFF) << 24); // lo は hi 下位8bitを上位に詰める

		    
		    r[(short)(i+roff)] = (byte) (xi[3]& 255);
		    
		    
		}
	}
	
	private static final int L[] = {
			0xed, 0xd3, 0xf5, 0x5c, 0x1a, 0x63, 0x12, 0x58,
			0xd6, 0x9c, 0xf7, 0xa2, 0xde, 0xf9, 0xde, 0x14,
			0,    0,    0,    0,    0,    0,    0,    0, 
			0,    0,    0,    0,    0,    0,    0,    0x10
		};
	
	//utility
	Matrix tx = new Matrix((short)16,(short)4);
	Matrix ty = new Matrix((short)16,(short)4);
	Matrix zi = new Matrix((short)16,(short)4);
	
	private void pack(byte [] r, Matrix[] p)
	{
		tx.clear();
		ty.clear();
		zi.clear();
		
		inv25519_M(zi, p[2]); 

		M_Matrix_M(tx, p[0], zi);
		M_Matrix_M(ty, p[1], zi);

		pack25519_M(r, ty);

		r[31] ^= par25519(tx) << 7;
	}
	
	
	//Matrix c = new Matrix((short)16,(short)2,zero_Matrix_16);
	private void inv25519_M(
			Matrix o,
			Matrix i)
	{
		c.clear();
		
		short a;
		
		for (a = 0; a < 16; a ++) {
			c.copyRowFrom(a,a,i);
		}
		
		for(a=253;a>=0;a--) {
			S(c, c);
			if(a!=2&&a!=4) M_Matrix_M(c, c, i);
		}
		
		for (a = 0; a < 16; a ++) { 
			o.copyRowFrom(a,a,c);
		}

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o.get(dbg);
		///L/og.d(TAG, "inv25519 -> "+dbgt);
	}
	
	private void S(Matrix o,Matrix a)
	{
		M_Matrix_M(o, a, a);
	}

    Matrix t_31 = new Matrix((short)31,cols); // 31 limb × 4
	private void M_Matrix_M(Matrix o, Matrix a, Matrix b) {
	    short i, j;
	    t_31.clear();
	    
	    short[] tmpMul = new short[4];
	    short[]ti = new short[4];   
	    
	    // 多倍長積算: t[i+j] += a[i]*b[j]
	    short[]ai = new short[4];
	    short[]bj = new short[4];
	    short[]tij = new short[4];
	    for (i = 0; i < 16; i++) {
	        a.getRow(i, ai);
	        for (j = 0; j < 16; j++) {
	        		b.getRow(j,bj);
	        		t_31.getRow((short)(i+j), tij);
	        		//System.out.printf("i=%02d,j=%02d\n", i,j);
	            mul64(ai, bj, tmpMul);            // tmpMul = a[i]*b[j] (hi/lo) 
	            add64(tij, tmpMul, tij);     // t[i+j] += tmpMul
	            t_31.setRow((short)(i+j), tij);
	        }
	        
	    }
	    //printMatrix("t31",t_31);
	    		short[] tiplus16 = new short[4];
	    		// 上位 limb を下位 limb に還元: t[i] += 38*t[i+16]
	    		
	    for (i = 0; i < 15; i++) {
	        t_31.getRow(i, ti);
	        t_31.getRow((short)(i+16), tiplus16);

	        mul64Const(tiplus16, 38, tmpMul);
	        //System.out.printf("tmpMul:%04x %04x %04x %04x\n", tmpMul[0],tmpMul[1],tmpMul[2],tmpMul[3]);
	        add64(ti, tmpMul, ti);
	        t_31.setRow(i, ti);
	    }
	    //printMatrix("t31",t_31);
	    // 結果を o にコピー（16 limb）
	    for (i = 0; i < 16; i++) {
	        o.copyRowFrom(i, i, t_31);
	    }
	    //printMatrix("o",o);
	    // hi/lo 繰り上がり処理
	    car25519Matrix(o);
	    //printMatrix("o",o);
	    car25519Matrix(o);
	}
	
	
	
	// a, b は 32bit int
	
	int[] a8 = new int[4];
    int[] b8 = new int[4];
	private void mul32to64(int a, int b, int[] out) {
		
	    
	    // 32bit を 8bit に分割
	    for (short i = 0; i < 4; i++) {
	        a8[i] = (a >>> (8*i)) & 0xFF;
	        b8[i] = (b >>> (8*i)) & 0xFF;
	    }

	    int[] tmp = new int[8]; // 64bit を 8バイトに展開
	    for (short i = 0; i < 4; i++) {
	        for (short j = 0; j < 4; j++) {
	            tmp[(short)(i+j)] += a8[i] * b8[j];
	        }
	    }

	 // キャリー処理
	    for (short i = 0; i < 7; i++) {
	        int loPart = tmp[i] & 0xFF;
	        int carry  = tmp[i] >>> 8;
	        tmp[i] = loPart;
	        tmp[(short)(i+1)] += carry;
	    }
	 // 下位 32bit を tmp[0..3] から作る
	    int lo = tmp[0] + (tmp[1]<<8) + (tmp[2]<<16) + (tmp[3]<<24);

	    // 上位 32bit を tmp[4..7] から作る
	    int hi = (tmp[4]) | (tmp[5]<<8) | (tmp[6]<<16) | (tmp[7]<<24);

	    // 直接代入する（ここで addSignedToRow を使わない）
	    out[0] = lo; // lo
	    out[1] = hi; // hi
	}

	
		
	public static void sub16WithBorrow(int[] a, int[] b, int[] o) {
	    // 下位16bit差
	    int loDiff = (a[0] & 0xFFFF) - (b[0] & 0xFFFF);
	    int borrow = (loDiff < 0) ? 1 : 0;
	    o[0] = loDiff & 0xFFFF;

	    // 上位16bit差に borrow を反映
	    int hiDiff = (a[0] >>> 16) - (b[0] >>> 16) - borrow;
	    borrow = (hiDiff < 0) ? 1 : 0;
	    hiDiff &= 0xFFFF;

	    // o[1] に上位32bitとして格納
	    o[1] = (hiDiff << 16) | (a[1] - b[1] - borrow);
	}
    // 部分積
    int[] tmp1_in = new int[2];
    int[] tmp2_in = new int[2];
    int[] tmp3_in = new int[2];
    int[] tmp4_in = new int[2];
	private void mul64(short[] ai, short[] bj, short[] out) {
		// 符号判定
		boolean negA = ai[0] < 0; 
		boolean negB = bj[0] < 0; 
	    boolean negResult = negA ^ negB;
	    
	 // intで一時的に64bit値に組み立て
        int[] aAbs = { ((ai[0] & 0xFFFF) << 16) | (ai[1] & 0xFFFF),((ai[2] & 0xFFFF) << 16) | (ai[3] & 0xFFFF)};
        int[] bAbs = { ((bj[0] & 0xFFFF) << 16) | (bj[1] & 0xFFFF),((bj[2] & 0xFFFF) << 16) | (bj[3] & 0xFFFF)};
	    //ここでint
	    
	    // 絶対値化 (aiAbs, bjAbs を int[2] で作る)
	    abs64(aAbs, aAbs, negA);
	    abs64(bAbs, bAbs, negB);

	    mul32to64(aAbs[1], bAbs[1], tmp1_in); // lo*lo
	    mul32to64(aAbs[1], bAbs[0], tmp2_in); // lo*hi
	    mul32to64(aAbs[0], bAbs[1], tmp3_in); // hi*lo
	    mul32to64(aAbs[0], bAbs[0], tmp4_in); // hi*hi
	    
	    // 足し合わせて 128bit 中の下位 64bit を out にまとめる
	    int[] row ={tmp1_in[1], tmp1_in[0]}; // hi,lo
	    addSignedToRow_int(row, tmp2_in[0]);
	    addSignedToRow_int(row, tmp3_in[0]);
	    addSignedToRow_int(row, tmp2_in[1] + tmp3_in[1] + tmp4_in[0]);

	    // 結果の符号調整
	    if (negResult) {
	   
	        neg64(row, row);
	    }

	    out[0] = (short)(row[0] >>> 16);
        out[1] = (short)(row[0] & 0xFFFF);
        out[2] = (short)(row[1] >>> 16);
        out[3] = (short)(row[1] & 0xFFFF);
	    
	}
	
	private static void abs64(int[] in, int[] out, boolean bo) {
	    if (bo == true) {
	        neg64(in, out);
	    } else {
	        out[0] = in[0];
	        out[1] = in[1];
	        
	    }
	}
	

	// 64bit を two's complement で反転（x の符号を反転）
	// in {hi,lo} -> out {hi,lo}
	private static void neg64(int[] in, int[] out) {
	    // 64bit の two's complement
		int notLo = ~in[1];
		
		int newLo;
		int newHi;
		if(in[0] == 0 && in[1] ==0) {
			out[0] = 0;
			out[1] = 0;
		}
		else if(in[0] == 0 && in[1] !=0) {
			out[0] = -1;
			out[1] = notLo +1;
		}else {
		    int notHi = ~in[0];
		    newLo = notLo + 1;
		    int carry = (newLo == 0) ? 1 : 0; // lo がオーバーフローしたら上位へキャリー
		    newHi = notHi + carry;

		    out[0] = newHi;
		    out[1] = newLo;
		}
		
		
	}
	
	// t[2] : 64bit 値 (t[0]=lo, t[1]=hi)
	// c : 16bit 定数
	// out[2] : 結果 (64bit)
	//int[] tmp1 = new int[2];
	//int[] tmp2 = new int[2];
	private void mul64Const(short[] t, int c, short[] out) {
		// 符号判定
				boolean negT = t[0] < 0; 
			    
			    int[] tAbs = { ((t[0] & 0xFFFF) << 16) | (t[1] & 0xFFFF),((t[2] & 0xFFFF) << 16) | (t[3] & 0xFFFF)};
			    // 絶対値化 (aiAbs, bjAbs を int[2] で作る)
			    abs64(tAbs, tAbs, negT);
			    //System.out.printf("t: %08x,%08x\n", tAbs[0],tAbs[1]);
			    
		clear(tmp1_in);
		clear(tmp2_in);
		
		
		
	    // 下位32bit * c
	    mul32to64(c,tAbs[1], tmp1_in); // tmp1[0]=lo, tmp1[1]=hi
	    // 上位32bit * c
	    mul32to64(c,tAbs[0], tmp2_in); // tmp2[0]=lo, tmp2[1]=hi
	    
	    
	    
	    
	 // 足し合わせて 128bit 中の下位 64bit を out にまとめる
	    int[] row = {tmp1_in[1], tmp1_in[0]}; // hi,lo
	    row[0] +=tmp2_in[0];
	    
	 // 結果の符号調整
	    if (negT) {
	   
	        neg64(row, row);
	    }
	    //System.out.printf("row : %08x %08x\n", row[0], row[1]);
	    out[0] = (short)(row[0] >>> 16);
        out[1] = (short)(row[0] & 0xFFFF);
        out[2] = (short)(row[1] >>> 16);
        out[3] = (short)(row[1] & 0xFFFF);
	}
	
	/**
	 * row = {hi, lo} に signed int add を行う（lo += add、発生したキャリー/借りを hi に反映）
	 *  long 不使用。add は符号付きでよい（正／負どちらでも可）。
	 */
	private void addSignedToRow_int(int[] row, int add) {
		int oldLo = row[1];
	    int newLo = oldLo + add;

	    int carry = 0;

	 // 下位32bit overflow / underflow 判定（符号なし比較）
	    if (add >= 0 && lessUnsigned(newLo, oldLo)) {
	        carry = 1;    // オーバーフロー
	    } else if (add < 0 && lessUnsigned(oldLo, newLo)) {
	        carry = -1;   // アンダーフロー
	    }

	    row[1] = newLo;
	    row[0] += carry;
	}
	
	// o: int[32] (16 limb × 2)
	short[] limb = new short[4];       // hi-lo
    short[] ol = new short[4];
	private void car25519Matrix(Matrix o) {
	    int c = 1;  

	    for (short i = 0; i < 16; i++) {
	        o.getRow(i, limb);

	        // 下位16bitから順に足す
	        addSignedToRow(limb,1<<16);
	        
	        
	        // 次の limb にキャリーを伝播
	         c= ((limb[1]& 0xFFFF) <<16)|(limb[2]& 0xFFFF); 
	        
	        // System.out.printf("c: %08x\n",c);
	        short lc = (short) ((i+1) * ((i<15) ? 1 : 0));
	        
	        o.getRow(lc, ol);
	        
	        int r = c-1+37*(c-1)*((i==15) ? 1 : 0);
	        // next limb に加算
	        addSignedToRow(ol, r);
	        //System.out.printf("limb:%04x %04x %04x %04x\n", ol[0],ol[1],ol[2],ol[3]);
	        o.setRow(i, limb);
	        o.setRow(lc, ol);

	        o.getRow(i, limb);

	        int sub = c << 16;
	        addSignedToRow(limb, -sub);
	        
	     // 上位32bitは固定
	        limb[0] = limb[2]<0 ? (short)-1:0;
	        limb[1] = limb[0];

	        o.setRow(i, limb);
	    }
	}
	// 無符号比較（long 使えない環境用トリック）
	private static boolean lessUnsigned(int x, int y) {
	    return (x ^ 0x80000000) < (y ^ 0x80000000);
	}

	private void addSignedToRow(short[] row, int add) {
	    int carry = 0;

	    // 下位32bit（row[2], row[3]）を int にまとめる
	    int lo = ((row[2] & 0xFFFF) << 16) | (row[3] & 0xFFFF);
	    int newLo = lo + add;

	    // 32bit内のキャリー判定（符号なし比較）
	    if (add >= 0 && lessUnsigned(newLo, lo)) {
	        carry = 1;    // オーバーフロー
	    } else if (add < 0 && lessUnsigned(lo, newLo)) {
	        carry = -1;   // アンダーフロー
	    }

	    // 更新：下位32bitを short[2] に戻す
	    row[2] = (short) (newLo >>> 16);
	    row[3] = (short) newLo;

	    // 上位32bit（row[0], row[1]）に carry 反映
	    int hi = ((row[0] & 0xFFFF) << 16) | (row[1] & 0xFFFF);
	    hi += carry;
	    row[0] = (short) (hi >>> 16);
	    row[1] = (short) hi;
	}
	
	
	short[]t0 = new short[4]; 
	short[]m14 = new short[4]; 
	short[]t15 = new short[4];
    short[] mi = new short[4];      // m の i 行
    short[] mi_1 = new short[4];    // m の i-1 行
    short[] ti = new short[4];      // t の i 行
    //Matrix m = new Matrix((short)16,(short)2,zero_Matrix_16);
    //Matrix t = new Matrix((short)16,(short)2,zero_Matrix_16);
	private void pack25519_M(byte [] o, Matrix n)
	{
		short i,b_s;
		a.clear(); //m
		t.clear(); //t
		for (i = 0; i < 16; i ++) {
			t.copyRowFrom(i,i,n);
		}

		car25519Matrix(t);
		car25519Matrix(t);
		car25519Matrix(t);

		
		for(short j = 0; j<2; j++) {
			
			t.getRow((short)0,t0);
			
			a.getRow((short)14,m14);
			
			t.getRow((short)15,t15);
			//System.out.printf("t0: %08x %08x\n", t0[0],t0[1]);
			addSignedToRow(t0, -0xffed);
			//System.out.printf("m0: %08x %08x\n", t0[0],t0[1]);
			a.setRow((short)0, t0);
			
			for(i = 1; i < 15; i++) {
	

			    a.getRow(i, mi);
			    a.getRow((short)(i-1), mi_1);
			    t.getRow(i, ti);

			    // ti - 0xffff - ((mi_1 >> 16)&1) を diffWithBorrow で計算
			    addSignedToRow(ti, -0xffff);
			    addSignedToRow(ti, -((mi_1[1] >>> 16) & 1));

			    // 計算結果を m の i 行に格納
			    a.setRow((short)i, ti);

			    // mi_1 の下位 limb をマスク
			    mi_1[1] &= 0xFFFF;
			    mi_1[0] = 0;
			    a.setRow((short)(i-1), mi_1);
			    
			}
			a.getRow((short)14,m14);
			addSignedToRow(t15, -0x7fff);
			addSignedToRow(t15, -((m14[1] >>> 16) & 1));

			a.setRow((short)15, t15);
			m14[1] &= 0xFFFF;  // 下位 limb をマスク
			m14[0] = 0;  // 下位 limb をマスク
			a.setRow((short)14, m14);
			
			// 最終 borrow フラグ
			b_s = (short)((t15[0] >>> 16) & 1);
			
			//dumpMatrix("t",t);
			//dumpMatrix("m",m);
				sel25519Matrix(t, a, (short)(1-b_s));
				//dumpMatrix("m",m);
				//dumpMatrix("t",t);
		}
		//System.out.println("-----");
		for (i = 0; i < 16; i ++) {
			
			t.getRow(i, ti);
			o[(short)(2*i)]=(byte) (ti[3]&0xff);
			o[(short)(2*i+1)]=(byte) (ti[3] >> 8);
		}
		
		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o[dbg];
		///L/og.d(TAG, "pack25519 -> "+dbgt);
	}
	// pRow, qRow: int[32] (16 limb × hi-lo)
		// b: 0 または 1

		private void sel25519Matrix(Matrix pRow, Matrix qRow, short b) {
			
			int mask = -b;  //b=0;do nothing, b=1;swap;
			
			

		    for (short i = 0; i < 16; i++) {
		        
		        // 行を取得
		        pRow.getRow(i, tmp1);
		        qRow.getRow(i, tmp2);

		        for(short j=0; j<4;j++) {
		        		short t = (short)(mask & ((tmp1[j] ^ tmp2[j]) & 0xFFFF));
		        		tmp1[j] ^= t;
				    tmp2[j] ^= t;
		        	
		        }
		        
		        // 計算結果を戻す
		        pRow.setRow(i, tmp1);
		        qRow.setRow(i, tmp2);
		    }
		}
		
		private byte par25519(Matrix a)
		{
			byte[] d = new byte[32];
			
			pack25519_M(d, a);
			
			return (byte) (d[0]&1);
		}
		
		static void printMatrix(String st,Matrix x) {
			short[] xi = new short[4];
			System.out.printf("===== %s =====\n",st);
			for(short i= 0; i<(short)(x.defrow) ; i++) {
				x.getRow(i,xi);
				long combine = ((long)(xi[0] & 0xFFFF) << 48) |
			               ((long)(xi[1] & 0xFFFF) << 32) |
			               ((long)(xi[2] & 0xFFFF) << 16) |
			               ((long)(xi[3] & 0xFFFF));
				System.out.printf("Row %02d: %016x \n", i, combine);
			}
		}
}
