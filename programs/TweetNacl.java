package com.example.sighner;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Int64 {
    int hi;
    int lo;
    
    Int64(int hi, int lo){
    		this.hi = hi;
    		this.lo = lo;
    }
    
    void setlo(short[] lo) {
	    lo[0] = (short)(this.lo & 0xFFFF); 
	    lo[1] = (short)((this.lo >>> 16) & 0xFFFF); 
    }
    void sethi(short[] hi) {
	    hi[0] = (short)(this.hi & 0xFFFF); 
	    hi[1] = (short)((this.hi >>> 16) & 0xFFFF); 
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
		

	private static final short [] D2v = {
			(short)0xf159,(short)0x26b2,
			(short)0x9b94,(short)0xebd6, 
			(short)0xb156,(short)0x8283,
			(short)0x149a,(short)0x00e0,
			(short)0xd130,(short)0xeef3,
			(short)0x80f2,(short)0x198e,
			(short)0xfce7,(short)0x56df,
			(short)0xd9dc,(short)0x2406
	};
	// X と Y を hi/lo に分けて Matrix にセット
		private static Matrix D2_MATRIX= new Matrix((short)16,(short)4);
		
	
	private static final short []  Xv =  {
			(short)0xd51a,(short)0x8f25,
			(short)0x2d60,(short)0xc956, 
			(short)0xa7b2,(short)0x9525,
			(short)0xc760,(short)0x692c,
			(short)0xdc5c,(short)0xfdd6,
			(short)0xe231,(short)0xc0a4, 
			(short)0x53fe, (short)0xcd6e,
			(short)0x36d3,(short)0x2169
	};
	
	private static final short []  Yv =  {
			0x6658,0x6666,
			0x6666,0x6666, 
			0x6666,0x6666,
			0x6666,0x6666,
			0x6666,0x6666,
			0x6666,0x6666,
			0x6666,0x6666,
			0x6666,0x6666
	};
	
	// X と Y を hi/lo に分けて Matrix にセット
	private static Matrix X_MATRIX= new Matrix((short)16,(short)4);
	private static Matrix Y_MATRIX= new Matrix((short)16,(short)4);
	
	// X と Y を hi/lo に分けて Matrix にセット
	private static Matrix gf0_MATRIX= new Matrix((short)16,(short)4);
	private static Matrix gf1_MATRIX= new Matrix((short)16,(short)4);
	

	
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
			D2_MATRIX.data[i] = 0;
			X_MATRIX.data[i] = 0;
			Y_MATRIX.data[i] = 0;
			gf0_MATRIX.data[i] = 0;
			gf1_MATRIX.data[i] = 0;
			if(i==1) {
				gf1_MATRIX.data[i] = 1;
			}
			if(i%4 ==3) {
				D2_MATRIX.data[i] = D2v[(short)(i/4)];
				X_MATRIX.data[i] = Xv[(short)(i/4)];
				Y_MATRIX.data[i] = Yv[(short)(i/4)];
			}
		}
	}
	
	
	
	//keyPair
	
	short rows = 16;   // limb 数
	short cols = 4;    // hi/lo
	Matrix p0 = new Matrix(rows, cols);
	Matrix p1 = new Matrix(rows, cols);
	Matrix p2 = new Matrix(rows, cols);
	Matrix p3 = new Matrix(rows, cols);
	short[] calclo = new short[2];	
	
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


		crypto_hash_SHA512_kp(d, sk,0,sk.length, 32);
		d[0] &= 248;
		d[31] &= 127;
		d[31] |= 64;

		///*smlen = n+64;

		for (i = 0; i < n; i ++) sm[(short)(64 + i)] = m[i];
		
		for (i = 0; i < 32; i ++) sm[(short)(32 + i)] = d[(short)(32 + i)];

		crypto_hash_SHA512(r, sm,32,sm.length-32, n+32);
		
		reduce(r);
		
		scalarbase(pMatrices, r,0,r.length);
		pack(sm,pMatrices);
		
		for (i = 0; i < 32; i ++) sm[(short)(i+32)] = sk[(short)(i+32)];
		crypto_hash_SHA512(h, sm,0,sm.length, n + 64);
		reduce(h);
		
		x.clear();
		
		for (i = 0; i < 32; i ++) {
			x.getRow(i, xi);
			xi[3] = (short)(r[i] & 0xff);  // 下位8bitをxi[0]に

			x.setRow((short)i, xi); // 必要ならMatrixに戻す
		}
		//printMatrix("x",x);
		
		short[] xij = new short[4];
		
		for (i = 0; i < 32; i ++) {
			for (j = 0; j < 32; j ++) {
		        int lo = (h[i] & 0xff) * (d[j] & 0xff); // 0..65025なので32bit内に収まる
		        	//System.out.printf("lo: %08x\n", lo);
		        
		        x.getRow((short)(i + j), xij);

		        calclo[0] = (short)(lo & 0xFFFF); 
			    calclo[1] = (short)((lo >>> 16) & 0xFFFF); 
		        
		        // hi/lo に分けて加算
		        addSignedToRow(xij, calclo); // lo の加算、hi に桁上がり反映
		        // 必要なら hi も加算

		        x.setRow((short)(i + j), xij);
			}
		}
		
		
		modL(sm,32,sm.length-32, x);
		
		return 0;

	}
	
	short[] uRow = new short[4];
	
	
	private void crypto_hash_SHA512(byte [] out, byte [] m,final int moff,final int mlen, int n) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(m, moff, mlen);
	        byte[] hash = md.digest();
	        System.arraycopy(hash, 0, out, 0, 64);
		} catch (Exception e) {
			 throw new RuntimeException(e);
	    }
        
	}
	
	private void crypto_hash_SHA512_kp(byte [] out, byte [] m,final int moff,final int mlen, int n) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(m, 0, 32); // seed部分
			byte[] d = md.digest(); // 秘密スカラー生成用
			System.arraycopy(d, 0, out, 0, 64);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
        
	}

	//suspicious
	// TBD... long length n
	///int crypto_hashblocks(byte [] x, byte [] m, long n)

	
	short[] tmp1 = new short[cols];
	short[] tmp2 = new short[cols];
	short[] tmp3 = new short[cols];

	

	
	
	// x + y -> out (64bit)
		static void add64(short[] x, short[] y, short[] out) {
			short carry = 0;
			short a8, b8, sum, incarry;
		    short result;

		    for (short i = (short)(x.length - 1); i >= 0; i--) {
		    		result = 0;
		        incarry = 0;

		        // ---- 下位8bit ----
		        a8 = (short)(x[i] & 0xFF);
		        b8 = (short)(y[i] & 0xFF);

		        sum = (short)(a8 + b8 + carry);

		        incarry = (short)((sum >> 8) & 1);  // ループ内での一時キャリー
		        sum &= 0xFF;                         // 下位8bitだけ残す

		        result |= sum;                        // 下位8bitをセット

		        // ---- 上位8bit ----
		        a8 = (short)((x[i] >> 8) & 0xFF);
		        b8 = (short)((y[i] >> 8) & 0xFF);

		        sum = (short)(a8 + b8 + incarry);

		        carry = (short)((sum >> 8) & 1);
		        sum &= 0xFF;

		        result |= (short)(sum << 8);  // 上位8bitをセット

		        out[i] = result;
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

		    short borrow = 0;    // 次の桁に持ち越すborrow
		    short a8, b8, diff, inborrow;
		    short result;

		    // 下位 short から上位 short に向かって処理（lo→hi）
		    for (short i = 3; i >= 0; i--) {
		        result = 0;
		        inborrow =0;

		        // 下位8bit
		        a8 = (short)(a[i] & 0xFF);
		        b8 = (short)(b[i] & 0xFF);
		        diff = (short)(a8 - b8 - borrow);

		        // ループ内での一時borrow
		        inborrow = (short)((diff < 0) ? 1 : 0);

		        // 下位8bitに収める
		        if (diff < 0) diff += 0x100;
		        result |= diff;

		        // 上位8bit
		        a8 = (short)((a[i] >> 8) & 0xFF);
		        b8 = (short)((b[i] >> 8) & 0xFF);
		        diff = (short)(a8 - b8 - inborrow);
		        borrow = (short)((diff < 0) ? 1 : 0);
		        if (diff < 0) diff += 0x100;
		        result |= (short)(diff << 8);


		        out[i] = result;
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
		    short carry = 0;  // 次の桁に持ち越すキャリー
		    short a8, b8, sum, incarry;
		    short result;

		    // 下位shortから上位shortに向かって計算
		    for (short i = 3; i >= 0; i--) {

		        result = 0;
		        incarry = 0;

		        // ---- 下位8bit ----
		        a8 = (short)(a[i] & 0xFF);
		        b8 = (short)(b[i] & 0xFF);

		        sum = (short)(a8 + b8 + carry);

		        incarry = (short)((sum >> 8) & 1);  // ループ内での一時キャリー
		        sum &= 0xFF;                         // 下位8bitだけ残す

		        result |= sum;                        // 下位8bitをセット

		        // ---- 上位8bit ----
		        a8 = (short)((a[i] >> 8) & 0xFF);
		        b8 = (short)((b[i] >> 8) & 0xFF);

		        sum = (short)(a8 + b8 + incarry);

		        carry = (short)((sum >> 8) & 1);
		        sum &= 0xFF;

		        result |= (short)(sum << 8);  // 上位8bitをセット

		        out[i] = result;
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
	short[] tmp = new short[4];
	private void modL(byte[] r,final int roff,final int rlen, Matrix x)
	{
		clear(xi);
		clear(xj);
		clear(tmp);
		short i, j;
		
		int tmp1,tmp2,tmp3,tmp4;
		
		int carryLo,lo_1,hi,lo;
		
		short[] lo_2 = new short[2];
		short[] tmp5 = new short[2];
		short[] tmp6 = new short[2];

		for (i = 63;i >= 32;--i) {
			
			//i = 52;

			carryLo = 0;
			x.getRow((short)i, xi);
			for (j = (short)(i - 32);j < i - 12;++j) {
				x.getRow((short)j, xj);
				lo_1 = ((xi[2] & 0xFFFF) << 16) | (xi[3] & 0xFFFF);
				tmp1 = carryLo;
				tmp2 = -16;
				tmp3 = lo_1 * L[(short)(j - (i - 32))];
				tmp4 = tmp2*tmp3;
				
				lo_2[0] = xi[2];
				lo_2[1] = xi[3];
				mulShort8bit(lo_2,L[(short)(j - (i - 32))],tmp5);
				mulShort8bit(tmp5,(short)16,tmp6);
				// tmp[0]=上位16bit, tmp[1]=下位16bit
				tmp6[0] = (short)(~tmp6[0]);
				tmp6[1] = (short)(~tmp6[1] + 1);
				if (tmp6[1] == 0) tmp6[0] += 1; // 下位が0の場合のキャリー
				
				//System.out.printf("tmp4:%08x, tmp6: %04x%04x\n",tmp4,tmp6[0],tmp6[1]);
				
				calclo[0] = (short)(tmp1 & 0xFFFF); 
			    calclo[1] = (short)(((tmp1) >>> 16) & 0xFFFF); 
				addSignedToRow(xj, calclo);
				calclo[0] = (short)(tmp4 & 0xFFFF); 
			    calclo[1] = (short)((tmp4 >>> 16) & 0xFFFF);
			    short[] xj2 = new short[] {xj[0],xj[1],xj[2],xj[3]};
			    
			    
			    //System.out.printf("calclo:%04x%04x, tmp6:%04x%04x\n",calclo[0],calclo[1],tmp6[0],tmp6[1],xj2[2],xj2[3]);
				addSignedToRow(xj, calclo);
				addSignedToRow(xj2, tmp6);
				
				//System.out.printf("xj:%04x%04x%04x%04x, xj2:%04x%04x%04x%04x\n",xj[0],xj[1],xj[2],xj[3],xj2[0],xj2[1],xj2[2],xj2[3]);
				// (xj[1] + 128) >> 8 を hi/lo で表現する

				// まず 128 を加える
				tmp[0] = xj[0];
				tmp[1] = xj[1];
				tmp[2] = xj[2];
				tmp[3] = xj[3];
				
				addSignedToRow(tmp, new short[]{128,0});

				// 上位32bit = x[0] << 16 | x[1]
			    hi = ((tmp[0] & 0xFFFF) << 16) | (tmp[1] & 0xFFFF);
			    // 下位32bit = x[2] << 16 | x[3]
			    lo = ((tmp[2] & 0xFFFF) << 16) | (tmp[3] & 0xFFFF);

			    carryLo = (lo >>> 8) | ((hi & 0xFF) << 24); // lo は hi 下位8bitを上位に詰める
			    int notLo = ~carryLo;
			    int newLo = notLo + 1;

			    calclo[0] = (short)((newLo<<8) & 0xFFFF); 
			    calclo[1] = (short)(((newLo<<8) >>> 16) & 0xFFFF); 
				addSignedToRow(xj,calclo);
				
				x.setRow((short)j, xj);
				
			}
			
			x.getRow((short)j, xj);
			calclo[0] = (short)(carryLo & 0xFFFF); 
		    calclo[1] = (short)((carryLo >>> 16) & 0xFFFF);
			addSignedToRow(xj,calclo);
			
			x.setRow((short)j, xj);
			//printMatrix("x",x);
			clear(xi);
			x.setRow((short)i, xi);
			
			
		}
		//printMatrix("x",x);
		carryLo = 0;
		short[] x31 = new short[4];
		for (j = 0; j < 32; j ++) {
			x.getRow((short)j, xj);
			x.getRow((short)31, x31);
			lo_1 = ((x31[2] & 0xFFFF) << 16) | (x31[3] & 0xFFFF);
			tmp1 = carryLo;
			tmp2 = -(lo_1 >> 4) * L[(short)j];
			calclo[0] = (short)(tmp1 & 0xFFFF); 
		    calclo[1] = (short)((tmp1 >>> 16) & 0xFFFF);
			addSignedToRow(xj,calclo);
			calclo[0] = (short)(tmp2 & 0xFFFF); 
		    calclo[1] = (short)((tmp2 >>> 16) & 0xFFFF);
			addSignedToRow(xj,calclo);
			
			
			// 上位32bit = x[0] << 16 | x[1]
		    hi = ((xj[0] & 0xFFFF) << 16) | (xj[1] & 0xFFFF);
		    // 下位32bit = x[2] << 16 | x[3]
		    lo = ((xj[2] & 0xFFFF) << 16) | (xj[3] & 0xFFFF);


		    carryLo = (lo >>> 8) | ((hi & 0xFF) << 24); // lo は hi 下位8bitを上位に詰める


			
			xj[0] = 0;
			xj[1] = 0;
			xj[2] = 0;
			xj[3] &=0x00FF;
			
			x.setRow((short)j, xj);
		}
		//printMatrix("x",x);
		for (j = 0; j < 32; j ++) {
			x.getRow((short)j, xj);
			tmp1 = -1*carryLo * L[(short)j];
			calclo[0] = (short)(tmp1 & 0xFFFF); 
		    calclo[1] = (short)((tmp1 >>> 16) & 0xFFFF);
			addSignedToRow(xj,calclo);
			x.setRow((short)j, xj);
		}
		//printMatrix("x",x);
		short[] xi1 = new short[4];
		for (i = 0; i < 32; i ++) {
			x.getRow((short)i, xi);
			x.getRow((short)(i+1), xi1);
			
			// 上位32bit = x[0] << 16 | x[1]
		    hi = ((xi[0] & 0xFFFF) << 16) | (xi[1] & 0xFFFF);
		    // 下位32bit = x[2] << 16 | x[3]
		    lo = ((xi[2] & 0xFFFF) << 16) | (xi[3] & 0xFFFF);

		    carryLo = (lo >>> 8) | ((hi & 0xFF) << 24); // lo は hi 下位8bitを上位に詰める
		    calclo[0] = (short)(carryLo & 0xFFFF); 
		    calclo[1] = (short)((carryLo >>> 16) & 0xFFFF);
		    addSignedToRow(xi1,calclo);
		    
		    r[(short)(i+roff)] = (byte) (xi[3]& 255);
		    
		    x.setRow((short)(i+1), xi1);
		}
	}
	
	private static final short L[] = {
			0xed, 0xd3, 0xf5, 0x5c, 0x1a, 0x63, 0x12, 0x58,
			0xd6, 0x9c, 0xf7, 0xa2, 0xde, 0xf9, 0xde, 0x14,
			0,    0,    0,    0,    0,    0,    0,    0, 
			0,    0,    0,    0,    0,    0,    0,    0x10
		};
	
	public void mulShort8bit(short[] a, short b, short[] out) {
		// a[0] = hi16, a[1] = lo16
	    short a0 = (short)(a[1] & 0xFF);        // lo16 下位8bit
	    short a1 = (short)((a[1] >> 8) & 0xFF); // lo16 上位8bit
	    short a2 = (short)(a[0] & 0xFF);        // hi16 下位8bit
	    short a3 = (short)((a[0] >> 8) & 0xFF); // hi16 上位8bit

	    short b0 = (short)(b & 0xFF);
	    short b1 = (short)((b >> 8) & 0xFF);

	    short sum, incarry;
	    short outLo = 0, outHi = 0;

	    // ---- 下位16bit計算 ----
	    sum = (short)(a0 * b0);
	    outLo = (short)(sum & 0xFF);       // 下位8bit
	    incarry = (short)((sum >> 8) & 0xFF);

	    sum = (short)(a1 * b0 + a0 * b1 + incarry);
	    outLo |= (short)((sum & 0xFF) << 8);  // 下位16bit 上位8bit
	    incarry = (short)((sum >> 8) & 0xFF);

	    // ---- 上位16bit計算 ----
	    sum = (short)(a2 * b0 + a1 * b1 + incarry);
	    outHi = (short)(sum & 0xFF);
	    incarry = (short)((sum >> 8) & 0xFF);

	    sum = (short)(a3 * b0 + a2 * b1 + incarry);
	    outHi |= (short)((sum & 0xFF) << 8);
	    // incarryはoutの上位キャリーとして必要なら次に持ち越す

	    out[0] = outHi; // 上位16bit
	    out[1] = outLo; // 下位16bit
	}
	
	
	
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
	

	private void mul32to64(short[] a16, short[] b16, Int64 out) {
	    // 32bit を 16bit に分割

	    // tmp 配列 16bit×4 = 64bit 表現
	    short[] tmp = new short[4]; // tmp[0..3] 下位から上位

	    // 部分積計算
	    // aLow*bLow
	    int prod = (a16[0] & 0xFFFF) * (b16[0] & 0xFFFF);
	    tmp[0] = (short)(prod & 0xFFFF);
	    tmp[1] = (short)((prod >>> 16) & 0xFFFF);

	    // aLow*bHigh
	    prod = (a16[0] & 0xFFFF) * (b16[1] & 0xFFFF);
	    int sum = (tmp[1] & 0xFFFF) + (prod & 0xFFFF);
	    tmp[1] = (short)(sum & 0xFFFF);
	    int carry = (prod >>> 16) + (sum >>> 16);

	    // aHigh*bLow
	    prod = (a16[1] & 0xFFFF) * (b16[0] & 0xFFFF);
	    sum = (tmp[1] & 0xFFFF) + (prod & 0xFFFF);
	    tmp[1] = (short)(sum & 0xFFFF);
	    carry += (prod >>> 16) + (sum >>> 16);

	    // aHigh*bHigh
	    prod = (a16[1] & 0xFFFF) * (b16[1] & 0xFFFF);
	    tmp[2] = (short)((prod & 0xFFFF) + (carry & 0xFFFF));
	    tmp[3] = (short)((prod >>> 16) + (carry >>> 16));

	    // 結果を Int64 に格納
	    out.lo = (tmp[0] & 0xFFFF) | ((tmp[1] & 0xFFFF) << 16);
	    out.hi = (tmp[2] & 0xFFFF) | ((tmp[3] & 0xFFFF) << 16);
	}

    // 部分積
    Int64 tmp1_in = new Int64(0,0);
    Int64 tmp2_in = new Int64(0,0);
    Int64 tmp3_in = new Int64(0,0);
    Int64 tmp4_in = new Int64(0,0);
    short[] alo = new short[2];
    short[] ahi = new short[2];
    short[] blo = new short[2];
    short[] bhi = new short[2];
    short[] lo2 = new short[2];
    short[] hi2 = new short[2];
    short[] lo3 = new short[2];
    short[] hi3 = new short[2];
    short[] lo4 = new short[2];

	private void mul64(short[] ai, short[] bj, short[] out) {
		// 符号判定
		boolean negA = ai[0] < 0; 
		boolean negB = bj[0] < 0; 
	    boolean negResult = negA ^ negB;
	    
	 // intで一時的に64bit値に組み立て
	    Int64 aAbs = new Int64(((ai[0] & 0xFFFF) << 16) | (ai[1] & 0xFFFF),((ai[2] & 0xFFFF) << 16) | (ai[3] & 0xFFFF));
	    Int64 bAbs = new Int64(((bj[0] & 0xFFFF) << 16) | (bj[1] & 0xFFFF),((bj[2] & 0xFFFF) << 16) | (bj[3] & 0xFFFF));
	    //ここでint
	    
	    // 絶対値化 (aiAbs, bjAbs を int[2] で作る)
	    if(negA) neg64(aAbs);
	    if(negB) neg64(bAbs);
	    aAbs.setlo(alo);
	    aAbs.sethi(ahi);
	    bAbs.setlo(blo);
	    bAbs.sethi(bhi);
	    mul32to64(alo, blo, tmp1_in); 
	    mul32to64(alo, bhi, tmp2_in); 
	    mul32to64(ahi, blo, tmp3_in); 
	    mul32to64(ahi, bhi, tmp4_in);
	    
	    tmp2_in.setlo(lo2);
	    tmp2_in.sethi(hi2);
	    tmp3_in.setlo(lo3);
	    tmp3_in.sethi(hi3);
	    tmp4_in.setlo(lo4); 
	    

	    addSignedToRow_int(tmp1_in, lo2);
	    addSignedToRow_int(tmp1_in, lo3);
	    add64(hi2,hi3,hi2);
	    add64(hi2,lo4,hi2);
	    addSignedToRow_int(tmp1_in, hi2);

	    // 結果の符号調整
	    if (negResult) {
	   
	        neg64(tmp1_in);
	    }

	    out[0] = (short)(tmp1_in.hi >>> 16);
        out[1] = (short)(tmp1_in.hi & 0xFFFF);
        out[2] = (short)(tmp1_in.lo >>> 16);
        out[3] = (short)(tmp1_in.lo & 0xFFFF);
	    
	}

	private static void neg64(Int64 a) {
	    
		int notLo = ~a.lo;

		if(a.hi == 0 && a.lo ==0) {
			a.hi = 0;
			a.lo = 0;
		}
		else if(a.hi == 0 && a.lo !=0) {
			a.hi = -1;
			a.lo= notLo +1;
		}else {
		    int notHi = ~a.hi;
		    a.lo = notLo + 1;
		    int carry = (a.lo == 0) ? 1 : 0; 
		    a.hi = notHi + carry;
		}
		
		
	}
	
	// t[2] : 64bit 値 (t[0]=lo, t[1]=hi)
	// c : 16bit 定数
	// out[2] : 結果 (64bit)
	//int[] tmp1 = new int[2];
	//int[] tmp2 = new int[2];
	short[] tlo = new short[2];
	short[] thi = new short[2];
	short[] cm = new short[2];
	private void mul64Const(short[] t, int c, short[] out) {
		// 符号判定
				boolean negT = t[0] < 0; 
			    
			    Int64 tAbs = new Int64(((t[0] & 0xFFFF) << 16) | (t[1] & 0xFFFF),((t[2] & 0xFFFF) << 16) | (t[3] & 0xFFFF));
			    // 絶対値化 (aiAbs, bjAbs を int[2] で作る)
			    if(negT) neg64(tAbs);
			    //System.out.printf("t: %08x,%08x\n", tAbs[0],tAbs[1]);
			    tAbs.setlo(tlo);
			    tAbs.sethi(thi);
			    cm[0] = (short)(c & 0xFFFF); 
			    cm[1] = (short)((c >>> 16) & 0xFFFF); 
	    // 下位32bit * c
	    mul32to64(cm,tlo, tmp1_in); // tmp1[0]=lo, tmp1[1]=hi
	    // 上位32bit * c
	    mul32to64(cm,thi, tmp2_in); // tmp2[0]=lo, tmp2[1]=hi

	    tmp1_in.hi += tmp2_in.lo;
	    
	 // 足し合わせて 128bit 中の下位 64bit を out にまとめる

	    
	 // 結果の符号調整
	    if (negT) {
	        neg64(tmp1_in);
	    }
	    //System.out.printf("row : %08x %08x\n", row[0], row[1]);
	    out[0] = (short)(tmp1_in.hi >>> 16);
        out[1] = (short)(tmp1_in.hi & 0xFFFF);
        out[2] = (short)(tmp1_in.lo >>> 16);
        out[3] = (short)(tmp1_in.lo & 0xFFFF);
	}
	
	/**
	 * row = {hi, lo} に signed int add を行う（lo += add、発生したキャリー/借りを hi に反映）
	 *  long 不使用。add は符号付きでよい（正／負どちらでも可）。
	 */
	private void addSignedToRow_int(Int64 row, short[] adds) {
		
		int add = ((adds[1] & 0xFFFF) << 16) | (adds[0] & 0xFFFF);
		int oldLo = row.lo;
	    int newLo = oldLo + add;

	    int carry = 0;

	 
	    if (add >= 0 && ((newLo ^ 0x80000000) < (oldLo ^ 0x80000000))) {
	        carry = 1;    
	    } else if (add < 0 && ((oldLo ^ 0x80000000) < (newLo ^ 0x80000000))) {
	        carry = -1;  
	    }

	    row.lo = newLo;
	    row.hi += carry;
	}
	
	// o: int[32] (16 limb × 2)
	short[] limb = new short[4];       // hi-lo
    short[] ol = new short[4];
	private void car25519Matrix(Matrix o) {
	    int c = 1;  

	    for (short i = 0; i < 16; i++) {
	        o.getRow(i, limb);
	        calclo[0] = (short)(1<<16 & 0xFFFF); 
		    calclo[1] = (short)(((1<<16) >>> 16) & 0xFFFF); 
	        // 下位16bitから順に足す
	        addSignedToRow(limb,calclo);
	        
	        
	        // 次の limb にキャリーを伝播
	         c= ((limb[1]& 0xFFFF) <<16)|(limb[2]& 0xFFFF); 
	        
	        // System.out.printf("c: %08x\n",c);
	        short lc = (short) ((i+1) * ((i<15) ? 1 : 0));
	        
	        o.getRow(lc, ol);
	        
	        int r = c-1+37*(c-1)*((i==15) ? 1 : 0);
	        // next limb に加算
	        calclo[0] = (short)(r & 0xFFFF); 
		    calclo[1] = (short)((r >>> 16) & 0xFFFF); 
	        addSignedToRow(ol, calclo);
	        //System.out.printf("limb:%04x %04x %04x %04x\n", ol[0],ol[1],ol[2],ol[3]);
	        o.setRow(i, limb);
	        o.setRow(lc, ol);

	        o.getRow(i, limb);

	        int sub = -1*(c << 16);
	        calclo[0] = (short)(sub & 0xFFFF); 
		    calclo[1] = (short)((sub >>> 16) & 0xFFFF); 
	        addSignedToRow(limb, calclo);
	        
	     // 上位32bitは固定
	        limb[0] = limb[2]<0 ? (short)-1:0;
	        limb[1] = limb[0];

	        o.setRow(i, limb);
	    }
	}

	private void addSignedToRow(short[] row, short[] adds) {
		int add = ((adds[1] & 0xFFFF) << 16) | (adds[0] & 0xFFFF);
		int carry = 0;
	    
	    int lo = ((row[2] & 0xFFFF) << 16) | (row[3] & 0xFFFF);
	    int newLo = lo + add;

	    if (add >= 0 && ((newLo ^ 0x80000000) < (lo ^ 0x80000000))) {
	        carry = 1;    
	    } else if (add < 0 && ((lo ^ 0x80000000) < (newLo ^ 0x80000000))) {
	        carry = -1;  
	    }

	    row[2] = (short) (newLo >>> 16);
	    row[3] = (short) newLo;

	    
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
			calclo[0] = (short)(-0xffed & 0xFFFF); 
		    calclo[1] = (short)((-0xffed >>> 16) & 0xFFFF); 
			addSignedToRow(t0, calclo);
			//System.out.printf("m0: %08x %08x\n", t0[0],t0[1]);
			a.setRow((short)0, t0);
			
			for(i = 1; i < 15; i++) {
	

			    a.getRow(i, mi);
			    a.getRow((short)(i-1), mi_1);
			    t.getRow(i, ti);

			    // ti - 0xffff - ((mi_1 >> 16)&1) を diffWithBorrow で計算
			    calclo[0] = (short)(-0xffff & 0xFFFF); 
			    calclo[1] = (short)((-0xffff >>> 16) & 0xFFFF); 
			    addSignedToRow(ti, calclo);
			    int m1 = -((mi_1[1] >>> 16) & 1);
			    if (m1 == 0) {
			        calclo[0] = 0;
			        calclo[1] = 0;
			    } else {
			        calclo[0] = (short)0xFFFF;
			        calclo[1] = (short)0xFFFF;
			    }
			    addSignedToRow(ti, calclo);

			    // 計算結果を m の i 行に格納
			    a.setRow((short)i, ti);

			    // mi_1 の下位 limb をマスク
			    mi_1[1] &= 0xFFFF;
			    mi_1[0] = 0;
			    a.setRow((short)(i-1), mi_1);
			    
			}
			a.getRow((short)14,m14);
			calclo[0] = (short)(-0x7fff & 0xFFFF); 
		    calclo[1] = (short)((-0x7fff >>> 16) & 0xFFFF); 
			addSignedToRow(t15, calclo);
			int m14_s = -((m14[1] >>> 16) & 1);
			if (m14_s == 0) {
			    calclo[0] = 0;
			    calclo[1] = 0;
			} else {
			    calclo[0] = (short)0xFFFF;
			    calclo[1] = (short)0xFFFF;
			}
			addSignedToRow(t15, calclo);

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
			
			short mask = (short) -b;  //b=0;do nothing, b=1;swap;
			
			

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
