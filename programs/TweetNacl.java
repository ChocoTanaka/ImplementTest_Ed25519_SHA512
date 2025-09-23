package com.example.sighner;

import java.security.SecureRandom;






final class UInt64 {
    private UInt64() {} // static only

    static void copy(int[] src, int[] dst) {
        dst[0] = src[0]; dst[1] = src[1];
    }

    static void xor(int[] a, int[] b, int[] out) {
        out[0] = a[0] ^ b[0];
        out[1] = a[1] ^ b[1];
    }

    static void and(int[] a, int[] b, int[] out) {
        out[0] = a[0] & b[0];
        out[1] = a[1] & b[1];
    }

    static void not(int[] a, int[] out) {
        out[0] = ~a[0]; out[1] = ~a[1];
    }


    // out = a + b (in-place allowed: out may be a or b)
    // shr: logical right shift by n (0<=n<64)
    static void shr(int[] x, int n, int[] out) {
        if (n == 0) { copy(x,out); return; }
        if (n < 32) {
            out[1] = (x[1] >>> n) | (x[0] << (32 - n));
            out[0] = x[0] >>> n;
        } else {
            out[1] = x[0] >>> (n - 32);
            out[0] = 0;
        }
    }
    static void shl(int[] x, int n, int[] out) {
        if (n == 0) { copy(x,out); return; }
        if (n < 32) {
            out[0] = (x[0] << n) | (x[1] >>> (32 - n));
            out[1] = x[1] << n;
        } else {
            out[0] = x[1] << (n - 32);
            out[1] = 0;
        }
    }

    static void rotr(int[] x, int n, int[] out) {
        int[] t1 = new int[2];
        int[] t2 = new int[2];
        shr(x, n, t1);
        shl(x, 64 - n, t2);
        out[0] = t1[0] | t2[0];
        out[1] = t1[1] | t2[1];
    }
}


final class Matrix {
    short defrow;
	short defcol;
	int[] data = new int[defrow * defcol];
	
	Matrix(short row, short col) {
		this.defrow = row;
		this.defcol = col;
		this.data = new int[row * col]; // ←ここで正しく確保
	}

    int get(short row, short col) {
        return data[row * defcol + col];
    }

    void set(short row, short col, int value) {
        data[row * defcol + col] = value;
    }
    
 // get row into int[2] (hi/lo)
    public void getRow(short row, int[] out) {
        out[0] = data[row * defcol + 0];
        out[1] = data[row * defcol + 1];
    }
    public void setRow(short row, int[] in) {
        data[row * defcol + 0] = in[0];
        data[row * defcol + 1] = in[1];
    }
    
 // 1行（hi/lo）をコピーする
    
    //a[i+j] = b[i]; => a.copyRowFrom(i+j,i,b);
    
    void copyRowFrom(short dstRow, short srcRow, Matrix src) {
    		int[]set = new int[2];    
    		src.getRow(srcRow, set);
        this.setRow(dstRow, set);
    }
}

public final class TweetNacl {
		
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
	private static final int [] D2v = new int [] {
			0x0000, 0xf159, 0x0000, 0x26b2, 0x0000, 0x9b94, 0x0000, 0xebd6, 
			0x0000, 0xb156, 0x0000, 0x8283, 0x0000, 0x149a, 0x0000, 0x00e0,
			0x0000, 0xd130, 0x0000, 0xeef3, 0x0000, 0x80f2, 0x0000, 0x198e,
			0x0000, 0xfce7, 0x0000, 0x56df, 0x0000, 0xd9dc, 0x0000, 0x2406
	};
	// X と Y を hi/lo に分けて Matrix にセット
		private static Matrix D2_MATRIX = new Matrix((short)16, (short)2);
		static {
			for(short i=0;i<16;i++){
				D2_MATRIX.set(i, (short)0, D2v[i*2]);     // hi
				D2_MATRIX.set(i, (short)1, D2v[i*2 + 1]); // lo
			}
		}
	
	private static final int []  Xv = new int [] {
			0x0000, 0xd51a, 0x0000, 0x8f25, 0x0000, 0x2d60, 0x0000, 0xc956, 
			0x0000, 0xa7b2, 0x0000, 0x9525, 0x0000, 0xc760, 0x0000, 0x692c,
			0x0000, 0xdc5c, 0x0000, 0xfdd6, 0x0000, 0xe231, 0x0000, 0xc0a4, 
			0x0000, 0x53fe, 0x0000, 0xcd6e, 0x0000, 0x36d3, 0x0000, 0x2169
	};
	
	private static final int []  Yv = new int [] {
			0x0000, 0x6658, 0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666, 
			0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666,
			0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666,
			0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666, 0x0000, 0x6666
	};
	
	// X と Y を hi/lo に分けて Matrix にセット
	private static final Matrix X_MATRIX = new Matrix((short)16, (short)2);
	private static final Matrix Y_MATRIX = new Matrix((short)16, (short)2);

	static {
		for(short i=0; i<16; i++){
	    X_MATRIX.set(i, (short)0, Xv[i*2]);     // hi
	    X_MATRIX.set(i, (short)1, Xv[i*2 + 1]); // lo
	    Y_MATRIX.set(i, (short)0, Yv[i*2]);     // hi
	    Y_MATRIX.set(i, (short)1, Yv[i*2 + 1]); // lo
		};
	}
	
	
	// X と Y を hi/lo に分けて Matrix にセット
	private static final Matrix gf0_MATRIX = new Matrix((short)16, (short)2);
	private static final Matrix gf1_MATRIX = new Matrix((short)16, (short)2);

	static {
		for(short i=0; i<16; i++){
	    gf0_MATRIX.set(i, (short)0, 0x0000);     // hi
	    gf0_MATRIX.set(i, (short)1, 0x0000); // lo
	    if(i == 0) {
		    gf1_MATRIX.set(i, (short)0, 0x0000);     // hi
		    gf1_MATRIX.set(i, (short)1, 0x0001); // lo
	    }else {
		    gf1_MATRIX.set(i, (short)0, 0x0000);     // hi
		    gf1_MATRIX.set(i, (short)1, 0x0000); // lo
	    }

		};
	}
	
	private static int[] Kv = {
			0x428a2f98, 0xd728ae22, 0x71374491, 0x23ef65cd, 0xb5c0fbcf, 0xec4d3b2f, 0xe9b5dba5, 0x8189dbbc,
			0x3956c25b, 0xf348b538, 0x59f111f1, 0xb605d019, 0x923f82a4, 0xaf194f9b, 0xab1c5ed5, 0xda6d8118,
			0xd807aa98, 0xa3030242, 0x12835b01, 0x45706fbe, 0x243185be, 0x4ee4b28c, 0x550c7dc3, 0xd5ffb4e2,
			0x72be5d74, 0xf27b896f, 0x80deb1fe, 0x3b1696b1, 0x9bdc06a7, 0x25c71235, 0xc19bf174, 0xcf692694,
			0xe49b69c1, 0x9ef14ad2, 0xefbe4786, 0x384f25e3, 0x0fc19dc6, 0x8b8cd5b5, 0x240ca1cc, 0x77ac9c65,
			0x2de92c6f, 0x592b0275, 0x4a7484aa, 0x6ea6e483, 0x5cb0a9dc, 0xbd41fbd4, 0x76f988da, 0x831153b5,
			0x983e5152, 0xee66dfab, 0xa831c66d, 0x2db43210, 0xb00327c8, 0x98fb213f, 0xbf597fc7, 0xbeef0ee4,
			0xc6e00bf3, 0x3da88fc2, 0xd5a79147, 0x930aa725, 0x06ca6351, 0xe003826f, 0x14292967, 0x0a0e6e70,
			0x27b70a85, 0x46d22ffc, 0x2e1b2138, 0x5c26c926, 0x4d2c6dfc, 0x5ac42aed, 0x53380d13, 0x9d95b3df,
			0x650a7354, 0x8baf63de, 0x766a0abb, 0x3c77b2a8, 0x81c2c92e, 0x47edaee6, 0x92722c85, 0x1482353b,
			0xa2bfe8a1, 0x4cf10364, 0xa81a664b, 0xbc423001, 0xc24b8b70, 0xd0f89791, 0xc76c51a3, 0x0654be30,
			0xd192e819, 0xd6ef5218, 0xd6990624, 0x5565a910, 0xf40e3585, 0x5771202a, 0x106aa070, 0x32bbd1b8,
			0x19a4c116, 0xb8d2d0c8, 0x1e376c08, 0x5141ab53, 0x2748774c, 0xdf8eeb99, 0x34b0bcb5, 0xe19b48a8,
			0x391c0cb3, 0xc5c95a63, 0x4ed8aa4a, 0xe3418acb, 0x5b9cca4f, 0x7763e373, 0x682e6ff3, 0xd6b2b8a3,
			0x748f82ee, 0x5defb2fc, 0x78a5636f, 0x43172f60, 0x84c87814, 0xa1f0ab72, 0x8cc70208, 0x1a6439ec,
			0x90befffa, 0x23631e28, 0xa4506ceb, 0xde82bde9, 0xbef9a3f7, 0xb2c67915, 0xc67178f2, 0xe372532b,
			0xca273ece, 0xea26619c, 0xd186b8c7, 0x21c0c207, 0xeada7dd6, 0xcde0eb1e, 0xf57d4f7f, 0xee6ed178,
		    0x06f067aa, 0x72176fba, 0x0a637dc5, 0xa2c898a6, 0x113f9804, 0xbef90dae, 0x1b710b35, 0x131c471b,
		    0x28db77f5, 0x23047d84, 0x32caab7b, 0x40c72493, 0x3c9ebe0a, 0x15c9bebc, 0x431d67c4, 0x9c100d4c,
		    0x4cc5d4be, 0xcb3e42b6, 0x597f299c, 0xfc657e2a, 0x5fcb6fab, 0x3ad6faec, 0x6c44198c, 0x4a475817
	};
	
	// Matrixにセット
	private static final Matrix K = new Matrix((short)80, (short)2);
	static {
		for(short i=0; i<80; i++){
	    K.set(i, (short)0, Kv[i*2]);     // hi
	    K.set(i, (short)1, Kv[i*2 + 1]); // lo
		};
	}
	
	static void dl64(byte[] x, int off, int[] out) {
	    int hi = 0, lo = 0;
	    for (int i=0;i<4;i++) hi = (hi << 8) | (x[off+i] & 0xff);
	    for (int i=4;i<8;i++) lo = (lo << 8) | (x[off+i] & 0xff);
	    out[0]=hi; out[1]=lo;
	}

	static void ts64(byte[] x, int off, Matrix uMatrix) { 
	    int[] uRow = new int[2];
	    uMatrix.getRow((short)0, uRow);

	    int hi = uRow[0];
	    int lo = uRow[1];

	    // リトルエンディアンで書き込み（オリジナルと同じ順）
	    x[off+7] = (byte)(lo & 0xff);
	    x[off+6] = (byte)((lo >>> 8) & 0xff);
	    x[off+5] = (byte)((lo >>> 16) & 0xff);
	    x[off+4] = (byte)((lo >>> 24) & 0xff);
	    x[off+3] = (byte)(hi & 0xff);
	    x[off+2] = (byte)((hi >>> 8) & 0xff);
	    x[off+1] = (byte)((hi >>> 16) & 0xff);
	    x[off+0] = (byte)((hi >>> 24) & 0xff);
	}

	//private static long R(long x,int c){ return (x >>> c) | (x << (64 - c)); }
	
	// x, y, z は hi/lo 2要素の int[]
	static void Ch(int[] x, int[] y, int[] z, int[] out) {
	    int[] nx = new int[2];
	    not64(x, nx); // ~x (hi/lo同時に反転)

	    out[0] = (x[0] & y[0]) ^ (nx[0] & z[0]);
	    out[1] = (x[1] & y[1]) ^ (nx[1] & z[1]);
	}

	static void not64(int[] x, int[] out) {
	    out[0] = ~x[0];
	    out[1] = ~x[1];
	}
	static void Maj(int[] x, int[] y, int[] z, int[] out) {
	    // hi
	    out[0] = (x[0] & y[0]) ^ (x[0] & z[0]) ^ (y[0] & z[0]);
	    // lo
	    out[1] = (x[1] & y[1]) ^ (x[1] & z[1]) ^ (y[1] & z[1]);
	}
	
	static void Sigma0(int[] x, int[] out) {
	    int[] r28 = new int[2];
	    int[] r34 = new int[2];
	    int[] r39 = new int[2];

	    UInt64.rotr(x, 28, r28);
	    UInt64.rotr(x, 34, r34);
	    UInt64.rotr(x, 39, r39);

	    out[0] = r28[0] ^ r34[0] ^ r39[0];
	    out[1] = r28[1] ^ r34[1] ^ r39[1];
	}
	static void Sigma1(int[] x, int[] out) {
	    int[] r14 = new int[2];
	    int[] r18 = new int[2];
	    int[] r41 = new int[2];

	    UInt64.rotr(x, 14, r14);
	    UInt64.rotr(x, 18, r18);
	    UInt64.rotr(x, 41, r41);

	    out[0] = r14[0] ^ r18[0] ^ r41[0];
	    out[1] = r14[1] ^ r18[1] ^ r41[1];
	}
	// σ0(x) = ROTR(x,1) ^ ROTR(x,8) ^ SHR(x,7)
	static void sigma0(int[] x, int[] out) {
	    int[] r1  = new int[2];
	    int[] r8  = new int[2];
	    int[] s7  = new int[2];

	    UInt64.rotr(x, 1, r1);
	    UInt64.rotr(x, 8, r8);
	    UInt64.shr(x, 7, s7);  // x >>> 7

	    out[0] = r1[0] ^ r8[0] ^ s7[0];
	    out[1] = r1[1] ^ r8[1] ^ s7[1];
	}

	// σ1(x) = ROTR(x,19) ^ ROTR(x,61) ^ SHR(x,6)
	static void sigma1(int[] x, int[] out) {
	    int[] r19 = new int[2];
	    int[] r61 = new int[2];
	    int[] s6  = new int[2];

	    UInt64.rotr(x, 19, r19);
	    UInt64.rotr(x, 61, r61);
	    UInt64.shr(x, 6, s6);  // x >>> 6

	    out[0] = r19[0] ^ r61[0] ^ s6[0];
	    out[1] = r19[1] ^ r61[1] ^ s6[1];
	}
	
	//keyPair
	public static int  crypto_sign_keypair(byte [] pk, byte [] sk, boolean seeded) {
		byte [] d = new byte[64];
		short rows = 16;   // limb 数
		short cols = 2;    // hi/lo
		Matrix p0 = new Matrix(rows, cols);
		Matrix p1 = new Matrix(rows, cols);
		Matrix p2 = new Matrix(rows, cols);
		Matrix p3 = new Matrix(rows, cols);
		
		Matrix[] pMatrices = new Matrix[4];
		pMatrices[0] = p0;
		pMatrices[1] = p1;
		pMatrices[2] = p2;
		pMatrices[3] = p3;

		// 初期化（必要なら 0 で埋める）
		for (short i = 0; i < rows; i++) {
		    int[] zero = new int[2]; // hi/lo
		    zero[0] = 0; // lo
		    zero[1] = 0; // hi
		    p0.setRow(i, zero);
		    p1.setRow(i, zero);
		    p2.setRow(i, zero);
		    p3.setRow(i, zero);
		}

		if (!seeded) RandomCompat(sk, (short)32);
		crypto_hash(d, sk,0, 32, 32);
		System.out.println(util.HexUtil.byteArrayToHexString(d));
		d[0] &= 248;
		d[31] &= 127;
		d[31] |= 64;

		
		scalarbase(pMatrices, d,0,d.length);
		pack(pk, pMatrices);

		for (short i = 0; i < 32; i++) sk[i+32] = pk[i];
		return 0;
	}
	
	public static void RandomCompat(byte[] x, short len) {
	    final SecureRandom sr = new SecureRandom();


	    byte[] tmp = new byte[len];
        sr.nextBytes(tmp);
        System.arraycopy(tmp, 0, x, (short)0, len);
	}
	
	private static int crypto_hash(byte [] out, byte [] m,final int moff,final int mlen, int n)
	{
		byte[] h = new byte[64], x = new byte [256];
		int i;
		int b = n;
		
		for (i = 0; i < 64; i ++) h[i] = iv[i];

		crypto_hashblocks(h, m,moff,mlen, n);
		//System.out.println(util.HexUtil.byteArrayToHexString(h));
		
		///m += n;
		n &= 127;
		///m -= n;

		for (i = 0; i < 256; i ++) x[i] = 0;
		
		for (i = 0; i < n; i ++) x[i] = m[i+moff];
		x[n] = (byte) 128;

		// パディング終端
		n = 256 - 128 * (n < 112 ? 1 : 0);

		// b >>> 61 は上位3bitだが n は小さいので hi = 0
		x[n-9] = 0;  // Java Cardではlongが使えないため0でOK

		// ビット長を hi/lo に分ける（メッセージ長 b をビット単位に変換）
		int bitlen_lo = b << 3;  // 下位32bit
		int bitlen_hi = 0;       // 上位32bit（b << 3 < 2^32 なら0でOK）

		int[] uRow = new int[2];
		uRow[0] = bitlen_hi;
		uRow[1] = bitlen_lo;

		Matrix u = new Matrix((short)1,(short)2);
		u.setRow((short)0, uRow);

		// ts64 でリトルエンディアン書き込み
		ts64(x, n-8, u);
		
		crypto_hashblocks(h, x,0,x.length, n);
  		
		
		for (i = 0; i < 64; i ++) out[i] = h[i];

		return 0;
	}

	//suspicious
	// TBD... long length n
	///int crypto_hashblocks(byte [] x, byte [] m, long n)
	private static int crypto_hashblocks(byte [] x, byte [] m,final int moff,final int mlen, int n)
	{
		short col = (short)2;
			Matrix z = new Matrix((short)8, col);
			Matrix a = new Matrix((short)8, col);
			Matrix b = new Matrix((short)8, col);
			Matrix w = new Matrix((short)16, col);
			
			
			short i,j;
			int[] tmp = new int[2];
			for ( i = 0; i < 8; i++) {
			    dl64(x, (short)(8 * i), tmp);
			    z.setRow(i, tmp);
				a.setRow(i, tmp);
			}
			
			

			int moffset = moff;
			
			
			int[] a0 = new int[col];
			int[] a1 = new int[col];
			int[] a2 = new int[col];
			int[] b3 = new int[col];
			int[] a4 = new int[col];
			int[] a5 = new int[col];
			int[] a6 = new int[col];
			int[] a7 = new int[col];
			
			int[] b7 = new int[col];
			
			int[] tmp1 = new int[col];
			int[] tmp2 = new int[col];
			int[] tmp3 = new int[col];
			int[] tmp4 = new int[col];
			int[] tmp5 = new int[col];
			
			int[] K_i = new int[col];
			int[] w_i = new int[col];
			int[] t64   = new int[col];
			
			int[] w_j116 = new int[col];
			int[] w_j1416 = new int[col];
			int[] w_j916 = new int[col];
			int[] w_j = new int[col];
			
			int[] wrow = new int[col];
			while (n >= 128) {
				for (i = 0; i < 16; i++) {
				    dl64(m, (short)(8 * i + moffset), wrow);
				    w.setRow(i, wrow);
				}
				
				for (i = 0; i < 80; i ++) {
					for (j = 0; j < 8; j ++) {
						b.copyRowFrom(j,j,a);  // b[j] = a[j]
					}
					
					K.getRow(i, K_i);

					
					
					// t = a[7] + Sigma1(a[4]) + Ch(a[4],a[5],a[6]) + K[i] + w[i%16];
					a.getRow((short)4, a4);
					a.getRow((short)5, a5);
					a.getRow((short)6, a6);
					a.getRow((short)7, a7);
					w.getRow((short)(i % 16), w_i);

				    
				    Sigma1(a4, tmp1);        
				    Ch(a4, a5, a6, tmp2); 
				    add64(a7,tmp1,tmp3);      
				    add64(tmp2,K_i, tmp4);
				    add64(tmp3,tmp4,tmp5);
				    add64(tmp5,w_i,t64);
					
					
					a.getRow((short)0, a0);
					a.getRow((short)1, a1);
					a.getRow((short)2, a2);					

					// b[7] = t + Sigma0(a[0]) + Maj(a[0],a[1],a[2]);
					
					// Sigma0(a[0])
					Sigma0(a0, tmp1);

					// Maj(a[0], a[1], a[2])
					Maj(a0, a1, a2, tmp2);
					
					b.getRow((short)7, b7);
					
					add64(t64, tmp1, tmp3);
					add64(tmp3,tmp2,b7);
					b.setRow((short)7, b7);
					
					//b[3] += t;
					
					b.getRow((short)3, b3);	
					add64(b3,t64,b3);
					b.setRow((short)3,b3);
					
					
					for (j = 0; j < 8; j ++) {
						a.copyRowFrom((short)((j+1)%8),j, b);
						//a[i+j] = b[i]; => a.copyRowFrom(i+j,i,b);
					}
					
					
					//----problem---
					if (i%16 == 15) {
						for (j = 0; j < 16; j ++) {
							short j116 = (short)((j+1)%16);
							short j1416 = (short)((j+14)%16);
							short j916 = (short)((j+9)%16);
							
							w.getRow(j116, w_j116);
							w.getRow(j1416, w_j1416);

							w.getRow(j916, w_j916);
							w.getRow((short)j, w_j);
							
							sigma0(w_j116,tmp1);
							sigma1(w_j1416,tmp2);
							
							//wj = sigma0+sigma1+w_j916
							add64(tmp1,tmp2,tmp3);
							add64(tmp3,w_j916,w_j);
							
							// 結果を Matrix に書き戻す
						    w.setRow((short)j,w_j);							
						}
					}
					//-----
				}
				
				dumpMatrix("ai", a);
				dumpMatrix("Zi", z);
				
				int[] a_i = new int[2];
				int[] z_i = new int[2];
				for (i = 0; i < 8; i ++) {
					a.getRow(i, a_i);
					z.getRow(i, z_i);
					add64(a_i,z_i,tmp);//a[i] +=z[i];
					a.setRow(i, tmp);
					//z[i] = a[i]
					z.setRow(i,tmp);
					
				}

				moffset += 128;
				n -= 128;
			}

			int[] zi = new int[col];
			for (i = 0; i < 8; i ++) {
				z.getRow(i, zi);
				Matrix zr = new Matrix((short)1,(short)2);
				zr.setRow((short)0, zi);
				ts64(x,8*i, zr);
			}
			return n;
	}
	
	// x + y -> out (64bit)
	static void add64(int[] x, int[] y, int[] out) {
		int[] vals = new int[]{x[1], y[1]};      // 下位32bit
	    int[] carry = new int[2];
	    sumWithCarry(vals, carry);
	    int lo = carry[0];
	    int carryHi = carry[1];

	    vals[0] = x[0]; 
	    vals[1] = y[0]; 
	    vals[0] += carryHi;                     // 下位のキャリーを上位に足す
	    sumWithCarry(vals, carry);
	    out[0] = carry[0];                      // hi
	    out[1] = lo;                            // lo は元の下位32bit
	}


	
	private static void scalarbase(Matrix[] p, byte[] s,final int soff,final int slen)
	{
		
		short rows = 16;   // limb 数
		short cols = 2;    // hi/lo
		Matrix q0 = new Matrix(rows, cols);
		Matrix q1 = new Matrix(rows, cols);
		Matrix q2 = new Matrix(rows, cols);
		Matrix q3 = new Matrix(rows, cols);
		
		// 初期化（必要なら 0 で埋める）
				for (short i = 0; i < rows; i++) {
				    int[] zero = new int[2]; // hi/lo
				    zero[0] = 0; // lo
				    zero[1] = 0; // hi
				    q0.setRow(i, zero);
				    q1.setRow(i, zero);
				    q2.setRow(i, zero);
				    q3.setRow(i, zero);
				}
		
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
		int[] tmp = new int[2];
	    for (short i = 0; i < 16; i++) {
	    		a.getRow(i, tmp);        // a の i 行を tmp に取得
	        r.setRow(i, tmp);        // hi/lo をそのまま r の i 行にセット
	    }
	}
		
	private static void scalarmult(Matrix[] p, Matrix[] q, byte[] s,final int soff,final int slen)
	{
		
		set25519_M(p[0],gf0_MATRIX);
		set25519_M(p[1],gf1_MATRIX);
		set25519_M(p[2],gf1_MATRIX);
		set25519_M(p[3],gf0_MATRIX);

		
		for (int i = 255;i >= 0;--i) {
			byte b = (byte) ((s[i/8+soff] >> (i&7))&1);

			cswap(p,q,b);
			add(q,p);
			add(p,p);
			cswap(p,q,b);
		}

		///String dbgt = "";
		///for (int dbg = 0; dbg < p.length; dbg ++) for (int dd = 0; dd < p[dbg].length; dd ++) dbgt += " "+p[dbg][dd];
		///L/og.d(TAG, "scalarmult -> "+dbgt);
	}
	
	private static void cswap(Matrix[] p, Matrix[] q, byte b)
	{

		for (short i = 0; i < 4; i ++) {

			sel25519Matrix(p[i],q[i],b);  
		}
			
	}


	private static void add(Matrix[] p, Matrix[] q)
	{
		short col = (short)2;
		Matrix a = new Matrix((short)16,col);
		Matrix b = new Matrix((short)16,col);
		Matrix c = new Matrix((short)16,col);
		Matrix d = new Matrix((short)16,col);
		Matrix t = new Matrix((short)16,col);
		Matrix e = new Matrix((short)16,col);
		Matrix f = new Matrix((short)16,col);
		Matrix g = new Matrix((short)16,col);
		Matrix h = new Matrix((short)16,col);
		
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
	
	
		
	// o = a - b
	private static void Z_Matrix(Matrix o, Matrix a, Matrix b) {
		int[]ai = new int[2];
		
		int[]bi = new int[2];
		
		int[]oi = new int[2];
		
		int[] borrow = new int[2];
		int[] borrow_hi = new int[2];
	
		for (short i = 0; i < 16; i++) {
			a.getRow(i, ai);
			b.getRow(i, bi);
			o.getRow(i, oi);
			int[] checkdef = new int[] {ai[0],bi[0]};
			diffWithBorrow(checkdef,borrow);
			
			// 下位32bitの借り
	        oi[0] = borrow[0];
	        
	        int[] checkdef_hi = new int[] {ai[1],bi[1],borrow[1]};
	        diffWithBorrow(checkdef_hi,borrow_hi);
	        oi[1] = borrow_hi[0];
	        o.setRow((short)i, oi);
		
		}
		
	}
	
	public static void diffWithBorrow(int[] values, int[] borrow) {
	    int borrow_in = (values.length > 2) ? values[2] : 0;
	    int diff_lo = 0;
	    int diff_hi = 0;
	    
	    for (int i = 0; i < 2; i++) { // 0: 下位16bit, 1: 上位16bit
	        int a_part = (i == 0) ? (values[0] & 0xFFFF) : (values[0] >>> 16);
	        int b_part = (i == 0) ? (values[1] & 0xFFFF) : (values[1] >>> 16);

	        int temp = a_part - b_part - borrow_in;
	        if (temp < 0) {
	            temp += 0x10000; // 2^16を足して繰り上げ
	            borrow_in = 1;
	        } else {
	            borrow_in = 0;
	        }

	        if (i == 0) diff_lo = temp;
	        else diff_hi = temp;
	    }

	    borrow[0] = (diff_hi << 16) | (diff_lo & 0xFFFF);
	    borrow[1] = borrow_in; // 上位への borrow
	}
		
	// o = a + b
	private static void A_Matrix(Matrix o, Matrix a, Matrix b) {
		int[]ai = new int[2];
		
		int[]bi = new int[2];
		int[]oi = new int[2];
		
		int[] carry = new int[2];
		int[] carry_hi = new int[2];
		for (short i = 0; i < 16; i++) {
			a.getRow(i, ai);
			b.getRow(i, bi);
			o.getRow(i, oi);
			int[] checksum = new int[] {ai[0],bi[0]};

			sumWithCarry(checksum,carry);

	        
	        oi[0] = carry[0];
	        
	        int[] checksum_hi = new int[]{ai[1],bi[1],carry[1]};
	        sumWithCarry(checksum_hi,carry_hi);// 下位32bitを保存
	        oi[1] = carry_hi[0] + carry_hi[1];                  // 上位32bitを保存
	    
	        o.setRow(i, oi);
		}
		
	}
	
	
	//sign
	public static int crypto_sign(byte [] sm, short dummy /* *smlen not used*/, byte [] m, int/*long*/ n, byte [] sk)
	{
		byte[] d = new byte[64], h = new byte[64], r = new byte[64];

		short i, j;
		Matrix x = new Matrix((short)64,(short)2);

		short rows = 16;   // limb 数
		short cols = 2;    // hi/lo
		Matrix p0 = new Matrix(rows, cols);
		Matrix p1 = new Matrix(rows, cols);
		Matrix p2 = new Matrix(rows, cols);
		Matrix p3 = new Matrix(rows, cols);
		
		Matrix[] pMatrices = new Matrix[4];
		pMatrices[0] = p0;
		pMatrices[1] = p1;
		pMatrices[2] = p2;
		pMatrices[3] = p3;

		// 初期化（必要なら 0 で埋める）
		for (i = 0; i < rows; i++) {
		    int[] zero = new int[2]; // hi/lo
		    zero[0] = 0; // lo
		    zero[1] = 0; // hi
		    p0.setRow(i, zero);
		    p1.setRow(i, zero);
		    p2.setRow(i, zero);
		    p3.setRow(i, zero);
		}

		crypto_hash(d, sk,0,sk.length, 32);
		d[0] &= 248;
		d[31] &= 127;
		d[31] |= 64;

		///*smlen = n+64;

		for (i = 0; i < n; i ++) sm[64 + i] = m[i];
		
		for (i = 0; i < 32; i ++) sm[32 + i] = d[32 + i];

		crypto_hash(r, sm,32,sm.length-32, n+32);
		reduce(r);
		scalarbase(pMatrices, r,0,r.length);
		pack(sm,pMatrices);

		for (i = 0; i < 32; i ++) sm[i+32] = sk[i+32];
		crypto_hash(h, sm,0,sm.length, n + 64);
		reduce(h);

		for (i = 0; i < 64; i ++) {
			int[]xi  = new int[2];
			x.getRow(i, xi);
			xi[0] = 0;
			xi[1] = 0;
			x.setRow(i, xi);
		}
		
		for (i = 0; i < 32; i ++) {
			int[]xi  = new int[2];
			x.getRow(i, xi);
			xi[0] = r[i] & 0xff;  // 下位8bitをxi[0]に
			xi[1] = 0;             // 上位16bitは0に
			x.setRow((short)i, xi); // 必要ならMatrixに戻す
		}
		
		for (i = 0; i < 32; i ++) for (j = 0; j < 32; j ++) {
			int[]xij  = new int[2];
			x.getRow((short)(i+j), xij);
			// 下位16bitに加算
			int temp = (xij[0] & 0xFFFF) + ((h[i] & 0xFF) * (d[j] & 0xFF));
			int carry = temp >>> 16;
			xij[0] = temp & 0xFFFF;

			// 上位16bitにキャリーを足す
			xij[1] = (xij[1] & 0xFFFF) + carry;

			// 必要ならMatrixに戻す
			x.setRow((short)(i+j), xij);
		}
		
		modL(sm,32,sm.length-32, x);

		return 0;
	}
	
	private static void reduce(byte [] r)
	{
		Matrix x = new Matrix((short)64,(short)2);
		short i;
		int[]xi = new int[2];
		
		for (i = 0; i < 64; i ++) {
			x.getRow(i, xi);
			xi[0] = r[i] & 0xff;  // 下位8bitをxi[0]に
			xi[1] = 0;             // 上位16bitは0に
			x.setRow((short)i, xi); // 必要ならMatrixに戻す
		}
		
		for (i = 0; i < 64; i ++) r[i] = 0;
		
		modL(r,0,r.length, x);
	}
	
	private static void modL(byte[] r,final int roff,final int rlen, Matrix x)
	{
	    int[] xi = new int[2];
	    int[] xj = new int[2];
	    int[] x31 = new int[2];
	    int carry;

	    // 上位32バイトから処理
	    for (int i = 63; i >= 32; --i) {
	        carry = 0;
	        x.getRow((short)i, xi);
	        
	        for (int j = i - 32; j < i - 12; ++j) {
	            x.getRow((short)j, xj);

	            // 下位16bit計算
	            int temp = (xj[0] & 0xFFFF) - (16 * (xi[0] & 0xFFFF) * (L[j - (i - 32)] & 0xFF)) + carry;
	            carry = temp >> 16;
	            xj[0] = temp & 0xFFFF;

	            // 上位16bitにキャリーを足す
	            xj[1] = (xj[1] & 0xFFFF) + carry;
	            x.setRow((short)j, xj);
	        }

	        // xi は使い終わったらクリア
	        xi[0] = xi[1] = 0;
	    }

	    // 下位32バイト処理
	    carry = 0;
	    for (int j = 0; j < 32; j++) {
	        x.getRow((short)j, x31);
	        int temp = (x31[0] & 0xFFFF) - ((x31[1] >>> 4) & 0xFFFF) * (L[j] & 0xFF) + carry;
	        carry = temp >> 8;
	        x31[0] = temp & 0xFF;
	        x.setRow((short)j, x31);
	    }

	    // 最後に r に書き込む
	    for (int i = 0; i < 32; i++) {
	        x.getRow((short)i, xi);
	        r[i + roff] = (byte)(xi[0] & 0xFF);
	    }
	}
	
	private static final int L[] = {
			0xed, 0xd3, 0xf5, 0x5c, 0x1a, 0x63, 0x12, 0x58,
			0xd6, 0x9c, 0xf7, 0xa2, 0xde, 0xf9, 0xde, 0x14,
			0,    0,    0,    0,    0,    0,    0,    0, 
			0,    0,    0,    0,    0,    0,    0,    0x10
		};
	
	//utility
	private static void pack(byte [] r, Matrix[] p)
	{
		Matrix tx = new Matrix((short)16,(short)2);
		Matrix ty = new Matrix((short)16,(short)2);
		Matrix zi = new Matrix((short)16,(short)2);
		
		inv25519_M(zi, p[2]); 

		M_Matrix_M(tx, p[0], zi);
		M_Matrix_M(ty, p[1], zi);

		pack25519_M(r, ty);

		r[31] ^= par25519(tx) << 7;
	}
	
	private static void inv25519_M(
			Matrix o,
			Matrix i)
	{
		Matrix c = new Matrix((short)16,(short)2);
		short a;
		
		for (a = 0; a < 16; a ++) {
			int[] ia = new int[2];
			i.getRow(a, ia);
			c.setRow(a,ia);
		}
		
		for(a=253;a>=0;a--) {
			S(c, c);
			if(a!=2&&a!=4) M_Matrix_M(c, c, i);
		}
		
		for (a = 0; a < 16; a ++) {
			int[] ca =new int[2]; 
			c.getRow(a, ca);
			o.setRow(a, ca);
		}

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o.get(dbg);
		///L/og.d(TAG, "inv25519 -> "+dbgt);
	}
	private static void S(Matrix o,Matrix a)
	{
		M_Matrix_M(o, a, a);
	}

	private static void M_Matrix_M(Matrix o, Matrix a, Matrix b) {
	    short i, j;
	    int[] t = new int[31*2]; // 31 limb × hi/lo
	    int[] tmpMul = new int[2];
	    int[] tmpAdd = new int[2];

	    // t を初期化
	    for (i = 0; i < 31; i++) {
	        t[2*i] = 0;
	        t[2*i+1] = 0;
	    }

	    // 多倍長積算: t[i+j] += a[i]*b[j]
	    for (i = 0; i < 16; i++) {
	        int[] ai = { a.get(i, (short)0), a.get(i, (short)1) };
	        for (j = 0; j < 16; j++) {
	            int[] bj = { b.get(j, (short)0), b.get(j, (short)1) };
	            mul64(ai, bj, tmpMul);            // tmpMul = a[i]*b[j] (hi/lo)
	            add64ToArray(t, i+j, tmpMul);     // t[i+j] += tmpMul
	        }
	    }

	    // 上位 limb を下位 limb に還元: t[i] += 38*t[i+16]
	    for (i = 0; i < 15; i++) {
	        int[] ti = { t[2*i], t[2*i+1] };
	        int[] tiplus16 = { t[2*(i+16)], t[2*(i+16)+1] };
	        mul64Const(tiplus16, (short)38, tmpMul);
	        add64(ti, tmpMul, tmpAdd);
	        t[2*i] = tmpAdd[0];
	        t[2*i+1] = tmpAdd[1];
	    }

	    // 結果を o にコピー（16 limb）
	    for (i = 0; i < 16; i++) {
	    	int[] row = new int[2];   // hi-lo 2要素
	        row[0] = t[2*i];          // 下位32bit
	        row[1] = t[2*i+1];        // 上位32bit
	        o.setRow((short)i, row);
	    }

	    // hi/lo 繰り上がり処理
	    car25519Matrix(o);
	}
	
	
	// a, b は 32bit int
	private static void mul32to64(int a, int b, int[] out) {
	    int a_lo = a & 0xFFFF;
	    int a_hi = (a >>> 16) & 0xFFFF;
	    int b_lo = b & 0xFFFF;
	    int b_hi = (b >>> 16) & 0xFFFF;

	    int p0 = a_lo * b_lo;         // 下位 16bit × 下位 16bit
	    int p1 = a_lo * b_hi;         // 下位 × 上位
	    int p2 = a_hi * b_lo;         // 上位 × 下位
	    int p3 = a_hi * b_hi;         // 上位 × 上位

	    int carry = (p0 >>> 16) + (p1 & 0xFFFF) + (p2 & 0xFFFF);
	    out[0] = (p0 & 0xFFFF) | ((carry & 0xFFFF) << 16);       // 下位32bit
	    out[1] = p3 + (p1 >>> 16) + (p2 >>> 16) + (carry >>> 16); // 上位32bit
	}
	
	public static void sumWithCarry(int[] values, int[] carry) {
	    int lo = 0;
	    int hi = 0;

	    for (int v : values) {
	        int vlo = v & 0xFFFF;       // 下位16bit
	        int vhi = (v >>> 16) & 0xFFFF; // 上位16bit

	        lo += vlo;
	        hi += vhi;

	        // lo のオーバーフローを hi に伝搬
	        if ((lo & 0xFFFF0000) != 0) {
	            hi += (lo >>> 16);
	            lo &= 0xFFFF;
	        }
	    }

	    carry[0] = (hi << 16) | (lo & 0xFFFF); // 下位32bit
	    carry[1] = hi >>> 16;                  // さらに桁上がりした分
        
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
	
	private static void mul64(int[] ai, int[] bj, int[] out) {
		int[] tmp1 = new int[2];
	    int[] tmp2 = new int[2];
	    int[] tmp3 = new int[2];
	    int[] tmp4 = new int[2];

	    // a_lo * b_lo
	    mul32to64(ai[0], bj[0], tmp1);  // tmp1 = a_lo * b_lo

	    // a_lo * b_hi
	    mul32to64(ai[0], bj[1], tmp2);  // tmp2 = a_lo * b_hi

	    // a_hi * b_lo
	    mul32to64(ai[1], bj[0], tmp3);  // tmp3 = a_hi * b_lo

	    // a_hi * b_hi
	    mul32to64(ai[1], bj[1], tmp4);  // tmp4 = a_hi * b_hi
	    
	    int[] carry = new int[2]; // carry[0]=lo, carry[1]=hi
	    int[] checksom = new int[]{tmp1[1], tmp2[0], tmp3[0]};
	    sumWithCarry(checksom,carry);           // 上位部分

	    // 最終的な 64bit 結果
	    
	    int[] outoverflow = new int[2];
	    int[] check_out0 = new int[] {tmp1[0], carry[0]};
	    sumWithCarry(check_out0, outoverflow);
	    out[0] = outoverflow[0];     // 下位 32bit
	    out[1] = tmp4[0] + tmp2[1] + tmp3[1] + carry[1] + outoverflow[1];     // 上位 32bit
	    
	 // a_hi*b_hi の上位部分も忘れずに
	    out[1] += tmp4[1];
	}
	
	// t に tmpMul を足す、位置 ij に hi/lo 形式で加算
	private static void add64ToArray(int[] t, int ij, int[] tmpMul) {
	    // ij 番目の位置に hi/lo を加算
	    int tIndex = ij * 2; // hi: tIndex, lo: tIndex+1
	    int[] sum = new int[2];
	    
	    int[] tIn = new int[] {
	    		t[tIndex], t[tIndex + 1]
	    };
	    
	    add64(tmpMul, tIn, sum);
	    t[tIndex] = sum[0];
	    t[tIndex + 1] = sum[1];
	}
	
	// t[2] : 64bit 値 (t[0]=lo, t[1]=hi)
	// c : 16bit 定数
	// out[2] : 結果 (64bit)
	private static void mul64Const(int[] t, short c, int[] out) {
	    int t_lo = t[0];
	    int t_hi = t[1];

	    // 32bit x 16bit の分割積
	    int[] tmp1 = new int[2];
	    int[] tmp2 = new int[2];

	    // 下位32bit * c
	    mul32to64(t_lo, c, tmp1);

	    // 上位32bit * c
	    mul32to64(t_hi, c, tmp2);

	    // 64bit 結果を合算
	    int[] sumCarry = new int[2];
	    int[] sumTmp = new int[]{ tmp1[1], tmp2[0] };
	    sumWithCarry(sumTmp, sumCarry); // tmp1[1] + tmp2[0] をキャリー付きで足す

	    out[0] = tmp1[0];               // 下位32bit
	    out[1] = tmp2[1] + sumCarry[1]; // 上位32bit
	}
	
	// o: int[32] (16 limb × 2)
	// 各 limb: o[2*i] = lo, o[2*i+1] = hi
	private static void car25519Matrix(Matrix o) {
	    int[] limb = new int[2];   // 一時的に limb を格納
	    int carry;                 // キャリー計算用

	    for (short i = 0; i < 16; i++) {
	        o.getRow(i, limb);     // limb = [lo, hi]

	        // 下位 16bit に 2^16 を加算
	        limb[0] += 1 << 16;
	        carry = limb[0] >>> 16; // 下位からのキャリー
	        limb[0] &= 0xFFFF;      // 下位16bitだけ残す

	        limb[1] += carry;        // 上位にキャリーを加算

	        // 最上位 limb は mod 2^255-19 に従って調整
	        if (i == 15) {
	            limb[1] += 37 * carry;
	        }

	        o.setRow(i, limb);       // Matrix に戻す
	    }
	}
	
	public static void pack25519(byte[]o, int[]n) {
		short i,b;
		Matrix m = new Matrix((short)16,(short)2), t = new Matrix((short)16,(short)2);
		
		for (i = 0; i < 16; i ++) {
			t.set(i,(short)0, 0x0000);
			t.set(i,(short)1, n[i]);
		}

		car25519Matrix(t);
		car25519Matrix(t);
		car25519Matrix(t);

		
		for(short j = 0; j<2; j++) {
			
			int[]t0 = new int[2]; 
			t.getRow((short)0,t0);
			int[]m14 = new int[2]; 
			m.getRow((short)14,m14);
			int[]t15 = new int[2];
			t.getRow((short)15,t15);
			int[]m15 = new int[2]; 
			m.getRow((short)15,m15);
			
			// borrow 計算用
			int lo = t0[0]; // 下位 limb
			int hi = t0[1]; // 上位 limb

			// 下位 limb で 0xffed を引く
			int new_lo = lo - 0xffed;

			// borrow が発生したら上位 limb に伝播
			int borrow_1 = (new_lo >> 16) & 1;  // new_lo は int なので上位16bitを取り出す
			new_lo &= 0xFFFF;                 // 下位16bitだけにマスク

			// 上位 limb に borrow を加算
			int new_hi = hi + borrow_1;

			// 計算結果を m の 0 行に格納
			int[] borrow1 = new int[2];
			borrow1[0] = new_lo;
			borrow1[1] = new_hi;
			m.setRow((short)0, borrow1);
			
			for(i = 1; i < 15; i++) {
			    int[] mi = new int[2];      // m の i 行
			    int[] mi_1 = new int[2];    // m の i-1 行
			    int[] ti = new int[2];      // t の i 行

			    m.getRow(i, mi);
			    m.getRow((short)(i-1), mi_1);
			    t.getRow(i, ti);

			    // 下位 limb の借り計算
			    int[] borrow = new int[2];
			    // ti - 0xffff - ((mi_1 >> 16)&1) を diffWithBorrow で計算
			    int borrow_in = (mi_1[1] >>> 16) & 1;
			    int[] temp_values2 = new int[]{ti[0], 0xffff, borrow_in}; // 上位は0, 借りは3番目に入れる
			    diffWithBorrow(temp_values2, borrow);

			    // 計算結果を m の i 行に格納
			    m.setRow((short)i, borrow);

			    // mi_1 の下位 limb をマスク
			    mi_1[0] &= 0xFFFF;
			    m.setRow((short)(i-1), mi_1);
			    
			}

			int[] borrow_last = new int[2];
			int borrow_in = (m14[1] >>> 16) & 1;
			int[] temp_values = new int[]{t15[0], 0x7fff, borrow_in};
			diffWithBorrow(temp_values, borrow_last);
			m.setRow((short)15, borrow_last);
			m14[0] &= 0xFFFF;  // 下位 limb をマスク
			m.setRow((short)14, m14);
			
			// 最終 borrow フラグ
			b = (short)((borrow_last[0] >>> 16) & 1);
				
			
				sel25519Matrix(t, m, 1-b);
				
				//dumpMatrix("after sel25519", t);
		
		}
		
		for (i = 0; i < 8; i++) {
		    int[] ti = new int[2]; // getRowが返す形式に合わせる
		    t.getRow((short) i, ti);
		    int base = i * 4;  // 1行あたり4バイト

		    // lo (下位16bitだけ使う)
		    o[base + 0] = (byte) (ti[0] & 0xFF);
		    o[base + 1] = (byte) ((ti[0] >>> 8) & 0xFF);

		    // hi (下位16bitだけ使う)
		    o[base + 2] = (byte) (ti[1] & 0xFF);
		    o[base + 3] = (byte) ((ti[1] >>> 8) & 0xFF);
		}

	///String dbgt = "";
	///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o[dbg];
	///L/og.d(TAG, "pack25519 -> "+dbgt);
}
	
	private static void pack25519_M(byte [] o, Matrix n)
	{
		short i,b;
		Matrix m = new Matrix((short)16,(short)2), t = new Matrix((short)16,(short)2);
		
		for (i = 0; i < 16; i ++) {
			int[]ni = new int[2];
			n.getRow(i, ni);
			t.setRow(i, ni);
		}

		car25519Matrix(t);
		car25519Matrix(t);
		car25519Matrix(t);

		
		for(short j = 0; j<2; j++) {
			
			int[]t0 = new int[2]; 
			t.getRow((short)0,t0);
			int[]m14 = new int[2]; 
			m.getRow((short)14,m14);
			int[]t15 = new int[2];
			t.getRow((short)15,t15);
			int[]m15 = new int[2]; 
			m.getRow((short)15,m15);
			
			// borrow 計算用
			int lo = t0[0]; // 下位 limb
			int hi = t0[1]; // 上位 limb

			// 下位 limb で 0xffed を引く
			int new_lo = lo - 0xffed;

			// borrow が発生したら上位 limb に伝播
			int borrow_1 = (new_lo >> 16) & 1;  // new_lo は int なので上位16bitを取り出す
			new_lo &= 0xFFFF;                 // 下位16bitだけにマスク

			// 上位 limb に borrow を加算
			int new_hi = hi + borrow_1;

			// 計算結果を m の 0 行に格納
			int[] borrow1 = new int[2];
			borrow1[0] = new_lo;
			borrow1[1] = new_hi;
			m.setRow((short)0, borrow1);
			
			for(i = 1; i < 15; i++) {
			    int[] mi = new int[2];      // m の i 行
			    int[] mi_1 = new int[2];    // m の i-1 行
			    int[] ti = new int[2];      // t の i 行

			    m.getRow(i, mi);
			    m.getRow((short)(i-1), mi_1);
			    t.getRow(i, ti);

			    // 下位 limb の借り計算
			    int[] borrow = new int[2];
			    // ti - 0xffff - ((mi_1 >> 16)&1) を diffWithBorrow で計算
			    int borrow_in = (mi_1[1] >>> 16) & 1;
			    int[] temp_values2 = new int[]{ti[0], 0xffff, borrow_in}; // 上位は0, 借りは3番目に入れる
			    diffWithBorrow(temp_values2, borrow);

			    // 計算結果を m の i 行に格納
			    m.setRow((short)i, borrow);

			    // mi_1 の下位 limb をマスク
			    mi_1[0] &= 0xFFFF;
			    m.setRow((short)(i-1), mi_1);
			    
			}

			int[] borrow_last = new int[2];
			int borrow_in = (m14[1] >>> 16) & 1;
			int[] temp_values = new int[]{t15[0], 0x7fff, borrow_in};
			diffWithBorrow(temp_values, borrow_last);
			m.setRow((short)15, borrow_last);
			m14[0] &= 0xFFFF;  // 下位 limb をマスク
			m.setRow((short)14, m14);
			
			// 最終 borrow フラグ
			b = (short)((borrow_last[0] >>> 16) & 1);
				
			
				sel25519Matrix(t, m, 1-b);
				
				//dumpMatrix("after sel25519", t);
				
		}
		
			for (i = 0; i < 8; i++) {
			    int[] ti = new int[2]; // getRowが返す形式に合わせる
			    t.getRow((short) i, ti);
			    int base = i * 4;  // 1行あたり4バイト

			    // lo (下位16bitだけ使う)
			    o[base + 0] = (byte) (ti[0] & 0xFF);
			    o[base + 1] = (byte) ((ti[0] >>> 8) & 0xFF);

			    // hi (下位16bitだけ使う)
			    o[base + 2] = (byte) (ti[1] & 0xFF);
			    o[base + 3] = (byte) ((ti[1] >>> 8) & 0xFF);
			}

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o[dbg];
		///L/og.d(TAG, "pack25519 -> "+dbgt);
	}
	
	
	// suspicious point
	// pRow, qRow: int[32] (16 limb × hi-lo)
	// b: 0 または 1
	private static void sel25519Matrix(Matrix pRow, Matrix qRow, int b) {
		
		int mask = -b;  //b=0;do nothing, b=1;swap;
		
		

	    for (short i = 0; i < 16; i++) {
	        int[] pVals = new int[2];
	        int[] qVals = new int[2];

	        // 行を取得
	        pRow.getRow(i, pVals);
	        qRow.getRow(i, qVals);

	        // 下位32bit (lo) の XOR
	        int t_lo = mask & (pVals[0] ^ qVals[0]);
	        pVals[0] ^= t_lo;
	        qVals[0] ^= t_lo;

	        // 上位32bit (hi) の XOR
	        int t_hi = mask & (pVals[1] ^ qVals[1]);
	        pVals[1] ^= t_hi;
	        qVals[1] ^= t_hi;
	        
	        // 計算結果を戻す
	        pRow.setRow(i, pVals);
	        qRow.setRow(i, qVals);
	    }
	}
	
	private static byte par25519(Matrix a)
	{
		byte[] d = new byte[32];
		
		pack25519_M(d, a);
		
		return (byte) (d[0]&1);
	}
	
	// t の内容を全部ダンプする関数
	private static void dumpMatrix(String label, Matrix m) {
	    
		System.out.println("=== " + label + " ===");
	    for (int i = 0; i < m.defrow; i++) {
	    		int[] Mi = new int[2];
	    		m.getRow((short)i,Mi);
	        long hi = ((long) Mi[0]) & 0xFFFFFFFFL;
	        long lo = ((long) Mi[1]) & 0xFFFFFFFFL;
	        long combined = (hi << 32) | lo;
	        System.out.printf("row %2d : %08x\n", i, combined);
	    }
	}
	
	
}
