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
			System.out.println(util.HexUtil.byteArrayToHexString(r));
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
				xi[1] = r[i] & 0xff;  // 下位8bitをxi[0]に
				xi[0] = 0;             // 上位16bitは0に
				x.setRow((short)i, xi); // 必要ならMatrixに戻す
			}
			
			for (i = 0; i < 32; i ++) {
				for (j = 0; j < 32; j ++) {
					int hi = 0;
			        int lo = (h[i] & 0xff) * (d[j] & 0xff); // 0..65025なので32bit内に収まる
			        int[] tmp = new int[] {hi, lo};

			        int[] xij = new int[2];
			        x.getRow((short)(i + j), xij);

			        // hi/lo に分けて加算
			        addSignedToRow(xij, tmp[1]); // lo の加算、hi に桁上がり反映
			        // 必要なら hi も加算
			        xij[0] += tmp[0];

			        x.setRow((short)(i + j), xij);
				}
			}
			dumpMatrix("x",x);
			
			modL(sm,32,sm.length-32, x);
			
			return 0;

		}
		
	
	private static int crypto_hash(byte [] out, byte [] m,final int moff,final int mlen, int n)
	{
		byte[] h = new byte[64], x = new byte [256];
		int i;
		int b = n;
		
		for (i = 0; i < 64; i ++) h[i] = iv[i];

		crypto_hashblocks(h, m,moff,mlen, n);
		
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
			
			int[] w_jm15 = new int[2];  // w[j-15]
		    int[] w_jm2  = new int[2];  // w[j-2]
		    int[] w_jm16 = new int[2];  // w[j-16]
		    int[] w_jm7  = new int[2];  // w[j-7]

		    int[] s0 = new int[2];
		    int[] s1 = new int[2];
			
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
					
					if (i >= 16) {

					    w.getRow((short)((i-15) % 16), w_jm15);
					    w.getRow((short)((i-2)  % 16), w_jm2);
					    w.getRow((short)((i-16) % 16), w_jm16);
					    w.getRow((short)((i-7)  % 16), w_jm7);

					    sigma0(w_jm15, s0);
					    sigma1(w_jm2, s1);

					    add64(w_jm16, s0, tmp);
					    add64(tmp, w_jm7, tmp);
					    add64(tmp, s1, tmp);

					    w.setRow((short)(i % 16), tmp);
					}

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
					
				}
				
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
	    int lo = x[1] + y[1];  // 下位32bitの加算（オーバーフローしてもOK）
	    int carry = 0;

	    // キャリー判定（下位のオーバーフローを判定）
	    if (lessUnsigned(lo, x[1])) {
	        carry = 1;
	    }

	    // 上位32bitにキャリーを加えて加算
	    int hi = x[0] + y[0] + carry;

	    out[0] = hi;  // hi
	    out[1] = lo;  // lo
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
		//dumpMatrix("t",t);
		
		M_Matrix_M(a,a,t);
		//System.out.println("----------");
		//dumpMatrix("a",a);

		A_Matrix(b, p0, p1);
		A_Matrix(t, q0, q1);
		M_Matrix_M(b,b,t);
		M_Matrix_M(c, p3, q3);
		M_Matrix_M(c, c,  D2_MATRIX);
		M_Matrix_M(d, p2, q2);
		
		
		//dumpMatrix("b",b);
		
		A_Matrix(d, d, d);
		Z_Matrix(e, b, a);
		Z_Matrix(f, d, c);
		A_Matrix(g, d, c);
		A_Matrix(h, b, a);

		//dumpMatrix("e",e);
		//dumpMatrix("h",h);
		
		M_Matrix_M(p0, e, f);
		//dumpMatrix("p0",p0);
		
		M_Matrix_M(p1, h, g);
		M_Matrix_M(p2, g, f);
		
		M_Matrix_M(p3, e, h);
		
		
		//dumpMatrix("p3",p3);
	
	}
	
	
		
	// o = a - b
	private static void Z_Matrix(Matrix o, Matrix a, Matrix b) {
		int[]ai = new int[2];
		
		int[]bi = new int[2];
		
		int[]oi = new int[2];
		
		int[] borrow = new int[2]; // 下位の borrow を上位に渡す用
		for (short i = 0; i < 16; i++) {
		    a.getRow(i, ai);
		    b.getRow(i, bi);

		    // 下位 limb の減算
		    diffWithBorrow32_8bit(ai[1], bi[1],0, borrow); // borrow[0] = 下位の結果、borrow[1] = 上位への借り
		    oi[1] = borrow[0];

		    // 上位 limb の減算
		    diffWithBorrow32_8bit(ai[0], bi[0], borrow[1], borrow); // borrow[1] を受けて上位計算
		    oi[0] = borrow[0];
		o.setRow(i, oi);
		}
		
	}
	
	public static void diffWithBorrow32_8bit(int a, int b, int borrow_in, int[] result) {
	    int borrow = borrow_in;
	    int res = 0;

	    for (int k = 0; k < 4; k++) { // 下位から上位へ 8bit 単位で処理
	        int shift = k * 8;
	        int a_part = (a >>> shift) & 0xFF;
	        int b_part = (b >>> shift) & 0xFF;

	        int temp = a_part - b_part - borrow;
	        if (temp < 0) {
	            temp += 0x100; // 2^8
	            borrow = 1;
	        } else {
	            borrow = 0;
	        }

	        res |= (temp & 0xFF) << shift;
	    }

	    result[0] = res;
	    result[1] = borrow; // 上位への borrow
	}
	
	
	
		
	// o = a + b
	private static void A_Matrix(Matrix o, Matrix a, Matrix b) {
		int[] ai = new int[2];
	    int[] bi = new int[2];
	    int[] oi = new int[2];

	    for (short i = 0; i < 16; i++) {
	        a.getRow(i, ai);
	        b.getRow(i, bi);
	        // 下位 32bit を加算（符号付きキャリー対応）
	        oi[1] = ai[1];                 // lo 初期値
	        addSignedToRow(oi, bi[1]);     // lo に bi[1] を加算、必要なら hi に carry

	        // 上位 32bit を加算（符号付きキャリー対応）
	        oi[0] = ai[0];                 // hi 初期値
	        addSignedToRow(oi, bi[0]);     // hi に bi[0] を加算、必要なら hi に carry

	        o.setRow(i, oi);
	    }
	        
	        
	}
		
	
	
	
	private static void reduce(byte [] r)
	{
		Matrix x = new Matrix((short)64,(short)2);
		short i;
		int[]xi = new int[2];
		
		for (i = 0; i < 64; i ++) {
			x.getRow(i, xi);
			xi[1] = r[i] & 0xff;  // 下位8bitをxi[0]に
			xi[0] = 0;             // 上位16bitは0に
			x.setRow((short)i, xi); // 必要ならMatrixに戻す
		}
		
		for (i = 0; i < 64; i ++) r[i] = 0;
		
		modL(r,0,r.length, x);
	}
	
	private static void modL(byte[] r,final int roff,final int rlen, Matrix x)
	{
		int[] carry = new int[2];
		int i, j;
		int[] xi = new int[2];
		int[] xj = new int[2];
		int tmp1,tmp2,tmp3,tmp4;
		int[] tmp = new int[2];
		int newHi,newLo;
		
		for (i = 63;i >= 32;--i) {
			carry[0] = 0;
			carry[1] = 0;
			x.getRow((short)i, xi);
			for (j = i - 32;j < i - 12;++j) {
				x.getRow((short)j, xj);
				tmp1 = carry[1];
				tmp2 = -16;
				tmp3 = xi[1] * L[j - (i - 32)];
				tmp4 = tmp2*tmp3;
				addSignedToRow(xj, tmp1);
				addSignedToRow(xj, tmp4);
				// (xj[1] + 128) >> 8 を hi/lo で表現する

				// まず 128 を加える
				tmp[0] = xj[0];
				tmp[1] = xj[1];
				addSignedToRow(tmp, 128);

				// 2. 全体を 8bit 算術シフト
				newHi = tmp[0] >> 8;  // 算術シフトで符号維持
				newLo = ((tmp[0] << 24) | (tmp[1] >>> 8)) & 0xffffffff;

				
				// carry に代入
				carry[0] = newHi;
				carry[1] = newLo;
				
				int[] neg = new int[2];
				neg64(carry,neg);
			
				addSignedToRow(xj,(neg[1]<<8));
				//System.out.printf("xj %08x %08x\n", xj[0],xj[1]);
				x.setRow((short)j, xj);
			}
			x.getRow((short)j, xj);
			addSignedToRow(xj,carry[1]);
			
			x.setRow((short)j, xj);
			xi[1] = 0;
			xi[0] = 0;
			x.setRow((short)i, xi);
			
			
		}
		
		carry[0] = 0;
		carry[1] = 0;
		int[] x31 = new int[2];
		for (j = 0; j < 32; j ++) {
			x.getRow((short)j, xj);
			x.getRow((short)31, x31);
			tmp1 = carry[1];
			tmp2 = -(x31[1] >> 4) * L[j];
			addSignedToRow(xj,tmp1);
			addSignedToRow(xj,tmp2);
			
			//System.out.printf("xj %08x %08x\n", xj[0],xj[1]);
			// 2. 全体を 8bit 算術シフト
			newHi = xj[0] >> 8;  // 算術シフトで符号維持
			newLo = ((xj[0] << 24) | (xj[1] >>> 8)) & 0xffffffff;
			
			// carry に代入
			carry[0] = newHi;
			carry[1] = newLo;

			xj[1] &= 255;
			xj[0] = 0;
			x.setRow((short)j, xj);
		}
		
		for (j = 0; j < 32; j ++) {
			x.getRow((short)j, xj);
			tmp1 = -carry[1] * L[j];
			addSignedToRow(xj,tmp1);
			x.setRow((short)j, xj);
		}
		//dumpMatrix("x",x);
		int[] xi1 = new int[2];
		for (i = 0; i < 32; i ++) {
			x.getRow((short)i, xi);
			x.getRow((short)(i+1), xi1);
			//System.out.printf("xj %08x %08x\n", xj[0],xj[1]);
			// 2. 全体を 8bit 算術シフト
			newHi = xj[0] >> 8;  // 算術シフトで符号維持
			newLo = ((xj[0] << 24) | (xj[1] >>> 8)) & 0xffffffff;

			// carry に代入
			xi1[0] = newHi;
			xi1[1] = newLo;
		    r[i+roff] = (byte) (xi[1] & 255);
		    //x.setRow((short)(i+1), xi1);
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
	    Matrix t = new Matrix((short)31,(short)2); // 31 limb × hi/lo
	    int[] tmpMul = new int[2];

	    // t を初期化
	    for (i = 0; i < 31; i++) {
	    		int[]ti = new int[2];    
	    		t.getRow(i, ti);
	    		t.setRow(i, new int[]{0,0});
	    }

	    // 多倍長積算: t[i+j] += a[i]*b[j]
	    int[]ai = new int[2];
	    int[]bj = new int[2];
	    int[]tij = new int[2];
	    for (i = 0; i < 16; i++) {
	        a.getRow(i, ai);
	        for (j = 0; j < 16; j++) {
	        		b.getRow(j,bj);
	        		t.getRow((short)(i+j), tij);
	            mul64(ai, bj, tmpMul);            // tmpMul = a[i]*b[j] (hi/lo)  	            
	            add64(tij, tmpMul, tij);     // t[i+j] += tmpMul
	            t.setRow((short)(i+j), tij);
	        }
	        
	    }
	    		//dumpMatrix("t",t);
	    		int[] ti = new int[2];
	    		int[] tiplus16 = new int[2];
	    		// 上位 limb を下位 limb に還元: t[i] += 38*t[i+16]
	    		
	    for (i = 0; i < 15; i++) {
	        t.getRow(i, ti);
	        t.getRow((short)(i+16), tiplus16);

	        mul64Const(tiplus16, 38, tmpMul);
	        //System.out.printf("ti : %02d %08x %08x\n",(i), ti[0],ti[1]);
	        //System.out.printf("tmpMul : %02d %08x %08x\n",(i), tmpMul[0],tmpMul[1]);
	        add64(ti, tmpMul, ti);
	        t.setRow(i, ti);
            //System.out.printf("row : %02d %08x %08x\n",(i), ti[0],ti[1]);
	    }
	    //dumpMatrix("t",t);
	    // 結果を o にコピー（16 limb）
	    for (i = 0; i < 16; i++) {
	        o.copyRowFrom(i, i, t);
	    }
	    //dumpMatrix("o",o);
	    // hi/lo 繰り上がり処理
	    car25519Matrix(o);
	    //dumpMatrix("o",o);
	    car25519Matrix(o);
	}
	
	
	// a, b は 32bit int
	private static void mul32to64(int a, int b, int[] out) {
		int[] a8 = new int[4];
	    int[] b8 = new int[4];
	    
	    // 32bit を 8bit に分割
	    for (int i = 0; i < 4; i++) {
	        a8[i] = (a >>> (8*i)) & 0xFF;
	        b8[i] = (b >>> (8*i)) & 0xFF;
	    }

	    int[] tmp = new int[8]; // 64bit を 8バイトに展開
	    for (int i = 0; i < 4; i++) {
	        for (int j = 0; j < 4; j++) {
	            tmp[i+j] += a8[i] * b8[j];
	        }
	    }

	 // キャリー処理
	    for (int i = 0; i < 7; i++) {
	        int loPart = tmp[i] & 0xFF;
	        int carry  = tmp[i] >>> 8;
	        tmp[i] = loPart;
	        tmp[i+1] += carry;
	    }
	 // 下位 32bit を tmp[0..3] から作る
	    int lo = tmp[0] + (tmp[1]<<8) + (tmp[2]<<16) + (tmp[3]<<24);

	    // 上位 32bit を tmp[4..7] から作る
	    int hi = (tmp[4]) | (tmp[5]<<8) | (tmp[6]<<16) | (tmp[7]<<24);

	    // 直接代入する（ここで addSignedToRow を使わない）
	    out[0] = lo; // lo
	    out[1] = hi; // hi
	}

	public static void sumWithCarry(int[] values, int[] carry) {
		// バイト別に蓄積（lo0=最下位バイト, lo3=最上位バイト）
	    int lo0 = 0, lo1 = 0, lo2 = 0, lo3 = 0;
	    int hiCarry = 0; // 32bit より上のキャリーを蓄える（加算回数次第で大きくなる）

	    for (int v : values) {
	        // treat v as unsigned 32bit
	        int b0 = v & 0xFF;
	        int b1 = (v >>> 8) & 0xFF;
	        int b2 = (v >>> 16) & 0xFF;
	        int b3 = (v >>> 24) & 0xFF;

	        // lowest byte
	        int s0 = lo0 + b0;
	        lo0 = s0 & 0xFF;
	        int c0 = s0 >>> 8; // carry to next byte

	        // next byte
	        int s1 = lo1 + b1 + c0;
	        lo1 = s1 & 0xFF;
	        int c1 = s1 >>> 8;

	        // next
	        int s2 = lo2 + b2 + c1;
	        lo2 = s2 & 0xFF;
	        int c2 = s2 >>> 8;

	        // top byte of 32-bit word
	        int s3 = lo3 + b3 + c2;
	        lo3 = s3 & 0xFF;
	        int c3 = s3 >>> 8; // this is carry beyond 32 bits for this summand

	        hiCarry += c3;
	    }

	    // 合成して返す
	    carry[0] = (lo0) | (lo1 << 8) | (lo2 << 16) | (lo3 << 24);
	    carry[1] = hiCarry; // 上位 32bit（ただし加算回数が多いと更に桁上がりする点に注意）
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
		// 符号判定
		boolean negA = ai[0] < 0; 
		boolean negB = bj[0] < 0; 
	    boolean negResult = negA ^ negB;
	    
	    int[] aAbs = new int[2];
	    int[] bAbs = new int[2];
	    // 絶対値化 (aiAbs, bjAbs を int[2] で作る)
	    abs64(ai, aAbs, negA);
	    abs64(bj, bAbs, negB);
	    
	    // 部分積
	    int[] tmp1 = new int[2];
	    int[] tmp2 = new int[2];
	    int[] tmp3 = new int[2];
	    int[] tmp4 = new int[2];

	    mul32to64(aAbs[1], bAbs[1], tmp1); // lo*lo
	    mul32to64(aAbs[1], bAbs[0], tmp2); // lo*hi
	    mul32to64(aAbs[0], bAbs[1], tmp3); // hi*lo
	    mul32to64(aAbs[0], bAbs[0], tmp4); // hi*hi

	    
	    
	    // 足し合わせて 128bit 中の下位 64bit を out にまとめる
	    int[] row = new int[]{tmp1[1], tmp1[0]}; // hi,lo
	    addSignedToRow(row, tmp2[0]);
	    addSignedToRow(row, tmp3[0]);
	    addSignedToRow(row, tmp2[1] + tmp3[1] + tmp4[0]);

	    // 結果の符号調整
	    if (negResult) {
	   
	        neg64(row, row);
	    }

	    out[0] = row[0]; // hi
	    out[1] = row[1]; // lo
	    
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
	private static void mul64Const(int[] t, int c, int[] out) {
		// 符号判定
				boolean negT = t[0] < 0; 
			    
			    int[] tAbs = new int[2];
			    // 絶対値化 (aiAbs, bjAbs を int[2] で作る)
			    abs64(t, tAbs, negT);
			    //System.out.printf("t : %08x %08x\n", t[0],t[1]);
			    //System.out.printf("tAbs : %08x %08x\n", tAbs[0],tAbs[1]);
			    
		int[] tmp1 = new int[2];
		int[] tmp2 = new int[2];
		
		
	    // 下位32bit * c
	    mul32to64(c,tAbs[1], tmp1); // tmp1[0]=lo, tmp1[1]=hi
	    // 上位32bit * c
	    mul32to64(c,tAbs[0], tmp2); // tmp2[0]=lo, tmp2[1]=hi
	    
	    
	    
	    
	 // 足し合わせて 128bit 中の下位 64bit を out にまとめる
	    int[] row = new int[]{tmp1[1], tmp1[0]}; // hi,lo
	    row[0] +=tmp2[0];
	    
	 // 結果の符号調整
	    if (negT) {
	   
	        neg64(row, row);
	    }
	    //System.out.printf("row : %08x %08x\n", row[0], row[1]);
	    out[0] = row[0]; // hi
	    out[1] = row[1]; // lo
	}
	
	
	// o: int[32] (16 limb × 2)
	private static void car25519Matrix(Matrix o) {
		int[] limb = new int[2];       // hi-lo
	    int[] ol = new int[2];
	    int[] out = new int[2];
	    int c = 1;                     // キャリー初期値
	    
	    for (short i = 0; i < 16; i++) {
	        o.getRow(i, limb);
	        //System.out.println("i="+i);
	        //System.out.printf("limb: %08x %08x\n",limb[0],limb[1]);
	        addSignedToRow(limb, 1 << 16);
	        //if (limb[1] >= 0 && limb[0]>0) {
	        //    limb[0] = 0;
	        //}
	        
	        //System.out.printf("limb: %08x %08x\n",limb[0],limb[1]);
	        

	        if (limb[0] == 0 || limb[0] == -1) {
	            c = shiftRight16Small(limb[0], limb[1]);
	        } else {
	            c = shiftRight16Large(limb[0], limb[1]);
	        }
	        
	        //System.out.printf("c: %08d\n",c);
	        
	        short lc = (short) ((i+1) * ((i<15) ? 1 : 0));
	        
	        
	        
	        int r = c-1+37*(c-1)*((i==15) ? 1 : 0);
	        
	        o.setRow(i, limb);
	        o.getRow(lc, ol);
	        int[]tmp = new int[2];
	        o.getRow(lc, tmp);
	        addSignedToRow(ol, r);
	        //System.out.println(r<0);
	        //System.out.printf("r:%08x %08x %08x %08x %08x\n", r,tmp[0],tmp[1], ol[0],ol[1]);
	        o.setRow(lc, ol);

	        o.getRow(i, out);

	        int sub = c << 16;

	        // 下位32bitで減算
	        out[1] = out[1] - sub;
	        
	        
	        // 上位32bitは固定
	        out[0] = out[1]<0 ? -1:0;
	        
	        //System.out.printf("%08x %08x\n", out[0],out[1]);

	        o.setRow(i, out);
	    }
	}
	// 無符号比較（long 使えない環境用トリック）
	private static boolean lessUnsigned(int x, int y) {
	    return (x ^ 0x80000000) < (y ^ 0x80000000);
	}
	/**
	 * row = {hi, lo} に signed int add を行う（lo += add、発生したキャリー/借りを hi に反映）
	 *  long 不使用。add は符号付きでよい（正／負どちらでも可）。
	 */
	private static void addSignedToRow(int[] row, int add) {
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
	
	
	
	private static int shiftRight16Large(int hi, int lo) {
	    int hiPart = hi & 0xFFFF;
	    int loPart = (lo >>> 16) & 0xFFFF;
	    return (hiPart << 16) | loPart;
	}
	
	private static int shiftRight16Small(int hi, int lo) {
	    int result = (lo >>> 16) & 0xFFFF;
	    if (hi == -1) {
	        result |= 0xFFFF0000; // 負数なら符号拡張
	    }
	    return result;
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
			//System.out.printf("t0: %08x %08x\n", t0[0],t0[1]);
			addSignedToRow(t0, -0xffed);
			//System.out.printf("m0: %08x %08x\n", t0[0],t0[1]);
			m.setRow((short)0, t0);
			
			for(i = 1; i < 15; i++) {
			    int[] mi = new int[2];      // m の i 行
			    int[] mi_1 = new int[2];    // m の i-1 行
			    int[] ti = new int[2];      // t の i 行

			    m.getRow(i, mi);
			    m.getRow((short)(i-1), mi_1);
			    t.getRow(i, ti);

			    // ti - 0xffff - ((mi_1 >> 16)&1) を diffWithBorrow で計算
			    addSignedToRow(ti, -0xffff);
			    addSignedToRow(ti, -((mi_1[1] >>> 16) & 1));

			    // 計算結果を m の i 行に格納
			    m.setRow((short)i, ti);

			    // mi_1 の下位 limb をマスク
			    mi_1[1] &= 0xFFFF;
			    mi_1[0] = 0;
			    m.setRow((short)(i-1), mi_1);
			    
			}
			m.getRow((short)14,m14);
			addSignedToRow(t15, -0x7fff);
			addSignedToRow(t15, -((m14[1] >>> 16) & 1));

			m.setRow((short)15, t15);
			m14[1] &= 0xFFFF;  // 下位 limb をマスク
			m14[0] = 0;  // 下位 limb をマスク
			m.setRow((short)14, m14);
			
			// 最終 borrow フラグ
			b = (short)((t15[0] >>> 16) & 1);
			
			//dumpMatrix("t",t);
			//dumpMatrix("m",m);
				sel25519Matrix(t, m, 1-b);
				//dumpMatrix("m",m);
				//dumpMatrix("t",t);
		}
		//System.out.println("-----");
		for (i = 0; i < 16; i ++) {
			int[] ti = new int[2];
			t.getRow(i, ti);
			o[2*i]=(byte) (ti[1]&0xff);
			o[2*i+1]=(byte) (ti[1] >> 8);
		}
			System.out.println("o:" + util.HexUtil.byteArrayToHexString(o));
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

	     // 下位32bit (lo) XOR、符号なしマスク
	        int t_lo = mask & ((pVals[1] ^ qVals[1]) & 0xFFFFFFFF);
	        pVals[1] ^= t_lo;
	        qVals[1] ^= t_lo;

	        // 上位32bit (hi) XOR、符号なしマスク
	        int t_hi = mask & ((pVals[0] ^ qVals[0]) & 0xFFFFFFFF);
	        pVals[0] ^= t_hi;
	        qVals[0] ^= t_hi;
	        
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
	        System.out.printf("row %2d : %08x %08x\n", i, Mi[0],Mi[1]);
	    }
	}
	// t の内容を全部ダンプする関数
	private static void dumpMatrix(String label, int[] in) { 
	    System.out.println("=== " + label + " ===");
	    for (int i = 0; i < in.length / 2; i++) {
	        long hi = ((long) in[2*i]) & 0xFFFFFFFFL;     // 上位32bit
	        long lo = ((long) in[2*i+1]) & 0xFFFFFFFFL;   // 下位32bit
	        long combined = (hi << 32) | lo;
	        System.out.printf("row %2d : %016x\n", i, combined);
	    }
	}
	
}
