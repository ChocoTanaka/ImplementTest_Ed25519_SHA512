package com.example.sighner;


import java.util.Arrays;



public final class Ed25519SignerJC {
	
	private static final short SEED_LEN = 32;
    private static final short PK_LEN   = 32;
    private static final short H_LEN    = 64;

    private Sha512JC sha512jc;
    //or
    //private MessageDigest sha512;
    
    
    private final byte[] hSeed    = new byte[H_LEN];    // H(seed)
    private final byte[] a        = new byte[SEED_LEN]; // clamped secret scalar
    private final byte[] prefix   = new byte[SEED_LEN]; // upper 32 bytes of H(seed)

    private final byte[] r64      = new byte[H_LEN];    // H(prefix || M)
    private final byte[] r32      = new byte[SEED_LEN]; // r mod L
    private final byte[] Renc     = new byte[PK_LEN];   // encoded R
    private final byte[] hram64   = new byte[H_LEN];    // H(R || A || M)
    private final byte[] hram32   = new byte[SEED_LEN]; // reduced h
    private final byte[] S        = new byte[SEED_LEN]; // S
    
    private static final byte[] _9 = {
    		0x09,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
    	 };
    
    private static final int [] _121665 = {
    		0xDB41,0x01,0x00,0x00,
    		0x00,0x00,0x00,0x00,
    		0x00,0x00,0x00,0x00,
    		0x00,0x00,0x00,0x00,
    };
	

 
    private final byte[] pubKey = new byte[PK_LEN];; // 32B
    
    private static final short L[] = {
			(short)0xed, (short)0xd3, (short)0xf5, (short)0x5c, (short)0x1a, (short)0x63, (short)0x12,(short)0x58,
			(short)0xd6, (short)0x9c, (short)0xf7, (short)0xa2, (short)0xde, (short)0xf9, (short)0xde, (short)0x14,
			0,    0,    0,    0,    0,    0,    0,    0, 
			0,    0,    0,    0,    0,    0,    0,    (short)0x10
		};
        
    
    public Ed25519SignerJC(byte[] seed32, short off, short len) {
    		this.sha512jc = new Sha512JC();
    		//or
    		/*
    		try {
                String instance = "SHA-512";
                sha512 = MessageDigest.getInstance(instance);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace(); // エラー処理
            }

    		*/
        // (seed -> a/prefix -> A=a*B)
        // H(seed)
    		sha512jc.doFinal(seed32, off, SEED_LEN, hSeed, (short)0);
    		//or
    		/*
    		sha512.update(seed32, off, SEED_LEN);
    		byte[] digestResult = sha512.digest();
    		System.arraycopy(digestResult, 0, hSeed, 0, digestResult.length);
    		*/
        // a, prefix
        System.arraycopy(hSeed, (short)0, a, (short)0, SEED_LEN);
        clampScalar(a);
        System.arraycopy(hSeed, SEED_LEN, prefix, (short)0, SEED_LEN);

        // A = a*B
        scalarmult_base(pubKey, (short)0, a, (short)0);

        zero(hSeed, (short)0, H_LEN);
        zero(a, (short)0, SEED_LEN);
        zero(prefix, (short)0, SEED_LEN);
    }
    
    /**
     * 署名（R||S 64B）を生成。
     * @param seed32   32B seed（カード内保管のものを渡す）
     * @param msg      署名対象（可変長）
     * @param mOff     オフセット
     * @param mLen     長さ
     * @param sigOut   出力（64B）
     * @param sOff     出力オフセット
     */
    public void sign(final byte[] seed32, short mOff, short mLen,
            final byte[] msg, final byte[] sigOut, short sOff) {

    		// 1) H(seed) -> a, prefix
    		sha512jc.doFinal(seed32, (short)0, SEED_LEN, hSeed, (short)0);
    		//or
    		/*
    		sha512.update(seed32, (short)0, SEED_LEN);
		byte[] digestResult1 = sha512.digest();
		System.arraycopy(digestResult1, 0, hSeed, 0, digestResult1.length);
    		*/
		System.arraycopy(hSeed, (short)0, a, (short)0, SEED_LEN);
    		clampScalar(a);
    		System.arraycopy(hSeed, SEED_LEN, prefix, (short)0, SEED_LEN);

    		// 2) r = H(prefix || M) mod L
    		
    		sha512jc.reset();
    		sha512jc.update(prefix, (short)0, SEED_LEN);
    		sha512jc.doFinal(msg, mOff, mLen, r64, (short)0);
    		
    		//or
    		/*
    		sha512.reset();
    		sha512.update(prefix, (short)0, SEED_LEN);
    		sha512.update(msg, mOff, mLen);
    		byte[] digestResult2 = sha512.digest();
    		System.arraycopy(digestResult2, 0, r64, 0, digestResult2.length);
    		*/
    		
    		sc_reduce(r32, (short)0, r64, (short)0);
    		
    		System.out.print("r32: ");
            for (byte b : r32) {
                System.out.printf("%02X", b);
            }
            System.out.println();

    		// 3) R = r * B
    		scalarmult_base(Renc, (short)0, r32, (short)0);
    		
    		System.out.print("Renc: ");
            for (byte b : Renc) {
                System.out.printf("%02X", b);
            }
            System.out.println();

    		// 4) h = H(R || A || M) mod L
    		
    		
    		sha512jc.reset();
    		sha512jc.update(Renc, (short)0, PK_LEN);
    		sha512jc.update(pubKey, (short)0, PK_LEN);
    		sha512jc.doFinal(msg, mOff, mLen, hram64, (short)0);
    		//or
    		/*
    		sha512.reset();
    		sha512.update(Renc, (short)0, PK_LEN);
    		sha512.update(pubKey, (short)0, PK_LEN);
    		sha512.update(msg, mOff, mLen);
    		byte[] digestResult_3 = sha512.digest();
    		System.arraycopy(digestResult_3, 0, hram64, 0, digestResult_3.length);
    		*/
    		System.out.print("hram64: ");
            for (byte b : hram64) {
                System.out.printf("%02X", b);
            }
            System.out.println();
    		
    		
    		sc_reduce(hram32, (short)0, hram64, (short)0);
    		
    		System.out.print("hram32: ");
            for (byte b : hram32) {
                System.out.printf("%02X", b);
            }
            System.out.println();

    		// 5) S = (r + h*a) mod L
    		sc_muladd(S, (short)0, hram32, (short)0, a, (short)0, r32, (short)0);
    		
    		System.out.print("S: ");
            for (byte b : S) {
                System.out.printf("%02X", b);
            }
            System.out.println();

    		// 6) 出力: sig = R || S
    		System.arraycopy(Renc, (short)0, sigOut, sOff, PK_LEN);
    		System.out.print("Sigout: ");
            for (byte b : sigOut) {
                System.out.printf("%02X", b);
            }
            System.out.println();
    		System.arraycopy(S,    (short)0, sigOut, (short)(sOff + PK_LEN), SEED_LEN);
    		
    		System.out.print("Sigout: ");
            for (byte b : sigOut) {
                System.out.printf("%02X", b);
            }
            System.out.println();

    		// 秘密情報クリア
    		zero(hSeed, (short)0, H_LEN);
    		zero(a, (short)0, SEED_LEN);
    		zero(prefix, (short)0, SEED_LEN);
    		zero(r64, (short)0, H_LEN);
    		zero(r32, (short)0, SEED_LEN);
    		zero(hram64, (short)0, H_LEN);
    		zero(hram32, (short)0, SEED_LEN);
    		zero(S, (short)0, SEED_LEN);
    		// Renc/pubKey は公開情報なのでクリア不要
    }
    
    private static void clampScalar(byte[] a32) {
        a32[0]  &= (byte)0xF8;
        a32[31] &= (byte)0x7F;
        a32[31] |= (byte)0x40;
    }

    private static void zero(byte[] b, short off, short len) {
    	Arrays.fill(b, (short) 0, (short) len, (byte) 0);
    }
    
 // ===== TweetNaCl から移植する3関数 =====

    /** R = a * B（基点倍算、出力は32B圧縮形式） */
    private static void scalarmult_base(byte[] outR, short offR, byte[] a32, short offA) {
    			crypto_scalarmult(outR,a32,_9);
   
    }

    /** out32 = in64 mod L（64Bを group order L で縮約） */
    private static void sc_reduce(byte[] out32, short offOut, byte[] in64, short offIn) {
       
    		reduce(in64, out32);
    	
    }

    /** S = (r + h*a) mod L（多倍長演算） */
    private static void sc_muladd(byte[] outS, short offS,
                                  byte[] h, short offH,
                                  byte[] a, short offA,
                                  byte[] r, short offR) {

    		byte[] x = new byte[64];
    		System.arraycopy(r, (short)0, x, (short)0, (short)32);
    		for (short i = 0; i < 32; i++) {
    		    for (short j = 0; j < 32; j++) {
    		        x[(short)(i+j)] += (h[i]) * (a[j]);
    		    }
    		}
    		
    		
    		reduce(x,outS);
    }
    
	public static int crypto_scalarmult(byte []out,byte []in,byte []p)
	{
		
		
		
		byte[] z = new byte[32];
		int[] x = new int[80];

		short r,i;
		int [] a = new int[16],b = new int[16], c = new int[16],
				d = new int[16], e = new int[16], f = new int[16];
		
		for (i = 0; i < 31; i ++) z[i]=in[i];
		
		z[31]=(byte) (((in[31]&127)|64) & 0xff);
		z[0]&=248;
		
		unpack25519(x,p);
		
		for (i = 0; i < 16; i ++) {
			b[i]=x[i];
			d[i]=a[i]=c[i]=0;
		}
		a[0]=d[0]=1;

		for(i=254;i>=0;--i) {
			short index =(short)(i>>>3);
			r=(short) ((z[index]>>>(i&7))&1);
			sel25519(a,(short)0,(short)a.length, b,(short)0,(short)b.length, r);
			sel25519(c,(short)0,(short)c.length, d,(short)0,(short)d.length, r);
			A(e,(short)0,(short)e.length, a,(short)0,(short)a.length, c,(short)0,(short)c.length);
			Z(a,(short)0,(short)a.length, a,(short)0,(short)a.length, c,(short)0,(short)c.length);
			A(c,(short)0,(short)c.length, b,(short)0,(short)b.length, d,(short)0,(short)d.length);
			Z(b,(short)0,(short)b.length, b,(short)0,(short)b.length, d,(short)0,(short)d.length);
			S(d,(short)0,(short)d.length, e,(short)0,(short)e.length);
			S(f,(short)0,(short)f.length, a,(short)0,(short)a.length);
			M(a,(short)0,(short)a.length, c,(short)0,(short)c.length, a,(short)0,(short)a.length);
			M(c,(short)0,(short)c.length, b,(short)0,(short)b.length, e,(short)0,(short)e.length);
			A(e,(short)0,(short)e.length, a,(short)0,(short)a.length, c,(short)0,(short)c.length);
			Z(a,(short)0,(short)a.length, a,(short)0,(short)a.length, c,(short)0,(short)c.length);
			S(b,(short)0,(short)b.length, a,(short)0,(short)a.length);
			Z(c,(short)0,(short)c.length, d,(short)0,(short)d.length, f,(short)0,(short)f.length);
			M(a,(short)0,(short)a.length, c,(short)0,(short)c.length, _121665,(short)0,(short)_121665.length);
			A(a,(short)0,(short)a.length, a,(short)0,(short)a.length, d,(short)0,(short)d.length);
			M(c,(short)0,(short)c.length, c,(short)0,(short)c.length, a,(short)0,(short)a.length);
			M(a,(short)0,(short)a.length, d,(short)0,(short)d.length, f,(short)0,(short)f.length);
			M(d,(short)0,(short)d.length, b,(short)0,(short)b.length, x,(short)0,(short)x.length);
			S(b,(short)0,(short)b.length, e,(short)0,(short)e.length);
			sel25519(a,(short)0,(short)a.length, b,(short)0,(short)b.length, r);
			sel25519(c,(short)0,(short)c.length, d,(short)0,(short)d.length, r);
		}
		
		for (i = 0; i < 16; i ++) {
			x[(short)(i+16)]=a[i];
			x[(short)(i+32)]=c[i];
			x[(short)(i+48)]=b[i];
			x[(short)(i+64)]=d[i];
		}
		
		inv25519(x, (short)32, (short)(x.length-32), x, (short)32, (short)(x.length-32));
		
		M(x,(short)16,(short)(x.length-16), x,(short)16,(short)(x.length-16), x,(short)32,(short)(x.length-32));
		
		pack25519(out, x,(short)16,(short)(x.length-16));

		///String dbgt = "";
		///for (int dbg = 0; dbg < q.length; dbg ++) dbgt += " "+q[dbg];
		///L/og.d(TAG, "crypto_scalarmult -> "+dbgt);

		return 0;
	}
	
	private static void pack25519(byte [] o, int [] n,final short noff,final short nlen)
	{
		short i,j,b;
		int [] m = new int[16], t = new int[16];
		
		for (i = 0; i < 16; i ++) t[i] = n[(short)(i+noff)];
		
		car25519(t,(short)0,(short)t.length);
		car25519(t,(short)0,(short)t.length);
		car25519(t,(short)0,(short)t.length);
		
		for (j = 0; j < 2; j ++) {
			m[0]=t[0]-0xffed;
			
			for(i=1;i<15;i++) {
				m[i]=t[i]-0xffff-((m[(short)(i-1)] >> 16)&1);
				m[(short)(i-1)]&=0xffff;
			}
			
			m[15]=t[15]-0x7fff-((m[14] >> 16)&1);
			b= (short)((m[15] >> 16)&1);
			m[14]&=0xffff;
			
			sel25519(t,(short)0,(short)t.length, m,(short)0,(short)m.length, (short)(1-b));
		}
		
		for (i = 0; i < 16; i ++) {
			o[(short)(2*i)]=(byte) (t[i]&0xff);
			o[(short)(2*i+1)]=(byte) (t[i] >> 8);
		}

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o[dbg];
		///L/og.d(TAG, "pack25519 -> "+dbgt);
		//
	}

	
	private static void unpack25519(int [] o, byte [] n)
	{
		short i;
		
		for (i = 0; i < 16; i ++) o[i]=((n[(short)(2*i)]&0xff)+((n[(short)(2*i+1)]<<8)&0xffff));
		
		o[15]&=0x7fff;

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o[dbg];
		///L/og.d(TAG, "unpack25519 -> "+dbgt);
	}
	
	private static void inv25519(
			int [] o,final short ooff,final short olen,
			int [] i,final short ioff,final short ilen)
	{
		int [] c = new int[16];
		short a;
		
		for (a = 0; a < 16; a ++) c[a]=i[(short)(a+ioff)];
		
		for(a=253;a>=0;a--) {
			S(c,(short)0,(short)c.length, c,(short)0,(short)c.length);
			if(a!=2&&a!=4) M(c,(short)0,(short)c.length, c,(short)0,(short)c.length, i,ioff,ilen);
		}
		
		for (a = 0; a < 16; a ++) o[(short)(a+ooff)] = c[a];

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o.get(dbg);
		///L/og.d(TAG, "inv25519 -> "+dbgt);
	}
	
	private static void sel25519(
			int[] p,final short poff,final short plen,
			int[] q,final short qoff,final short qlen,
			short b)
	{
		short i;
		int t,c=~(b-1);
		
		for (i = 0; i < 16; i ++) {
			t = c & (p[(short)(i+poff)] ^ q[(short)(i+qoff)]);
			p[(short)(i+poff)] ^= t;
			q[(short)(i+qoff)] ^= t;
		}

		///String dbgt = "";
		///for (int dbg = 0; dbg < p.length; dbg ++) dbgt += " "+p.get(dbg);
		///L/og.d(TAG, "sel25519 -> "+dbgt);

	}
	
	private static void A(
			int [] o,final short ooff,final short olen,
			int [] a,final short aoff,final short alen,
			int [] b,final short boff,final short blen)
	{
		for (short i = 0; i < 16; i ++) o[(short)(i+ooff)] = (short)(a[(short)(i+aoff)] + b[(short)(i+boff)]);
	}

	private static void Z(
			int [] o,final short ooff,final short olen,
			int [] a,final short aoff,final short alen,
			int [] b,final short boff,final short blen)
	{
		for (short i = 0; i < 16; i ++) o[(short)(i+ooff)] = (short)(a[(short)(i+aoff)] - b[(short)(i+boff)]);
	}

	private static void M(
			int [] o,final short ooff,final short olen,
			int [] a,final short aoff,final short alen,
			int [] b,final short boff,final short blen)
	{
		short i,j;
		short [] t = new short[32];
		
		for (i = 0; i < 31; i ++) t[i]=0;
		
		for (i = 0; i < 16; i ++) for (j = 0; j < 16; j ++) t[(short)(i+j)]+=a[(short)(i+aoff)]*b[(short)(j+boff)];
		
		for (i = 0; i < 15; i ++) t[i]+=38*t[(short)(i+16)];
		
		for (i = 0; i < 16; i ++) o[(short)(i+ooff)]=t[i];
		
		car25519(o,ooff,olen);
		car25519(o,ooff,olen);

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o.get(dbg);
		///L/og.d(TAG, "M -> "+dbgt);
	}
	private static void S(
			int [] o,final short ooff,final short olen,
			int [] a,final short aoff,final short alen)
	{
		M(o,ooff,olen, a,aoff,alen, a,aoff,alen);
	}


	private static void car25519(int [] o,final short ooff,final short olen)
	{

		///String dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o.get(dbg);
		///L/og.d(TAG, "car25519 pre -> "+dbgt);

		for (int i = 0; i < 16; i ++) {
			o[(short)(i+ooff)] = (short)(o[(short)(i+ooff)] + (1 << 16)); 

			int c = o[(short)(i+ooff)]>>16;

			o[(short)((i+1)*((i<15) ? 1 : 0)+ooff)] += c-1+37*(c-1)*((i==15) ? 1 : 0);

			o[(short)(i+ooff)] -= (c<<16);
		}

		///dbgt = "";
		///for (int dbg = 0; dbg < o.length; dbg ++) dbgt += " "+o.get(dbg);
		///L/og.d(TAG, "car25519 -> "+dbgt);

	}
	
	private static void reduce(byte[] in, byte[]out)
	{
		short len = (short)in.length;
	 // 64 バイト → short 配列にコピー
	    short[] x = new short[len];
	    for (short i = 0; i < len; i++) {
	        x[i] = (short)(in[i] & 0xFF);
	    }

	    // in をゼロクリア
	    Arrays.fill(in, (short)0, len, (byte)0);

	    modL(out, (short)0, len, x);

	}
	
	private static void modL(byte[] r,final short roff,final short rlen, short x[])
	{
		int carry;
		short i, j;

		for (i = 63;i >= 32;--i) {
			carry = 0;
			for (j = (short)(i - 32);j < i - 12;++j) {
				x[j] += carry - 16 * x[i] * L[(short)(j - (i - 32))];
				carry = (x[j] + 128) >> 8;
			x[j] -= carry << 8;
			}
			x[j] += carry;
			x[i] = 0;
		}
		carry = 0;

		for (j = 0; j < 32; j ++) {
			x[j] += carry - (x[31] >> 4) * L[j];
			carry = x[j] >> 8;
			x[j] &= 255;
		}

		for (j = 0; j < 32; j ++) x[j] -= carry * L[j];

		for (i = 0; i < 32; i ++) {
			x[(short)(i+1)] += x[i] >> 8;
		    r[(short)(i+roff)] = (byte) (x[i] & 255);
		}
	}


}
