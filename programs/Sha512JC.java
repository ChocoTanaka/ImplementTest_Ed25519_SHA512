package com.example.sighner;

/**
 * SHA-512 implementation for JavaCard (int-based, no long).
 * Provides MessageDigest-like interface: reset, update, doFinal.
 *
 * This implementation stores 64-bit words as (hi:int, lo:int) pairs and avoids long usage.
 * It aims for correctness rather than maximum speed or minimal memory.
 */
public class Sha512JC {
    // SHA-512 block size and digest size
    private static final short BLOCK_SIZE = 128;
    private static final short DIGEST_SIZE = 64;

    // Internal state H (8 x 64-bit, stored as hi/lo ints)
    private int[] H = new int[16]; // [hi0, lo0, hi1, lo1, ...]

    // Message length in bits (128-bit counter simulated via two 64-bit halves -> but we store only 64-bit counter here)
    private int bitCountHi, bitCountLo;

    // Buffer for partial blocks
    private byte[] buffer = new byte[BLOCK_SIZE];
    private short bufferOffset;

    // Working W (80 words * 2 (hi,lo))
    private int[] W = new int[160];

    // SHA-512 initial hash value (hi,lo pairs)
    private static final int[] H0 = {
        0x6a09e667, 0xf3bcc908,
        0xbb67ae85, 0x84caa73b,
        0x3c6ef372, 0xfe94f82b,
        0xa54ff53a, 0x5f1d36f1,
        0x510e527f, 0xade682d1,
        0x9b05688c, 0x2b3e6c1f,
        0x1f83d9ab, 0xfb41bd6b,
        0x5be0cd19, 0x137e2179
    };

    // SHA-512 constants K (hi,lo pairs)
    private static final int[] K = {
        0x428a2f98, 0xd728ae22, 0x71374491, 0x23ef65cd,
        0xb5c0fbcf, 0xec4d3b2f, 0xe9b5dba5, 0x8189dbbc,
        0x3956c25b, 0xf348b538, 0x59f111f1, 0xb605d019,
        0x923f82a4, 0xaf194f9b, 0xab1c5ed5, 0xda6d8118,
        0xd807aa98, 0xa3030242, 0x12835b01, 0x45706fbe,
        0x243185be, 0x4ee4b28c, 0x550c7dc3, 0xd5ffb4e2,
        0x72be5d74, 0xf27b896f, 0x80deb1fe, 0x3b1696b1,
        0x9bdc06a7, 0x25c71235, 0xc19bf174, 0xcf692694,
        0xe49b69c1, 0x9ef14ad2, 0xefbe4786, 0x384f25e3,
        0x0fc19dc6, 0x8b8cd5b5, 0x240ca1cc, 0x77ac9c65,
        0x2de92c6f, 0x592b0275, 0x4a7484aa, 0x6ea6e483,
        0x5cb0a9dc, 0xbd41fbd4, 0x76f988da, 0x831153b5,
        0x983e5152, 0xee66dfab, 0xa831c66d, 0x2db43210,
        0xb00327c8, 0x98fb213f, 0xbf597fc7, 0xbeef0ee4,
        0xc6e00bf3, 0x3da88fc2, 0xd5a79147, 0x930aa725,
        0x06ca6351, 0xe003826f, 0x14292967, 0x0a0e6e70,
        0x27b70a85, 0x46d22ffc, 0x2e1b2138, 0x5c26c926,
        0x4d2c6dfc, 0x5ac42aed, 0x53380d13, 0x9d95b3df,
        0x650a7354, 0x8baf63de, 0x766a0abb, 0x3c77b2a8,
        0x81c2c92e, 0x47edaee6, 0x92722c85, 0x1482353b,
        0xa2bfe8a1, 0x4cf10364, 0xa81a664b, 0xbc423001,
        0xc24b8b70, 0xd0f89791, 0xc76c51a3, 0x0654be30,
        0xd192e819, 0xd6ef5218, 0xd6990624, 0x5565a910,
        0xf40e3585, 0x5771202a, 0x106aa070, 0x32bbd1b8,
        0x19a4c116, 0xb8d2d0c8, 0x1e376c08, 0x5141ab53,
        0x2748774c, 0xdf8eeb99, 0x34b0bcb5, 0xe19b48a8,
        0x391c0cb3, 0xc5c95a63, 0x4ed8aa4a, 0xe3418acb,
        0x5b9cca4f, 0x7763e373, 0x682e6ff3, 0xd6b2b8a3,
        0x748f82ee, 0x5defb2fc, 0x78a5636f, 0x43172f60,
        0x84c87814, 0xa1f0ab72, 0x8cc70208, 0x1a6439ec,
        0x90befffa, 0x23631e28, 0xa4506ceb, 0xde82bde9,
        0xbef9a3f7, 0xb2c67915, 0xc67178f2, 0xe372532b,
        0xca273ece, 0xea26619c, 0xd186b8c7, 0x21c0c207,
        0xeada7dd6, 0xcde0eb1e, 0xf57d4f7f, 0xee6ed178,
        0x06f067aa, 0x72176fba, 0x0a637dc5, 0xa2c898a6,
        0x113f9804, 0xbef90dae, 0x1b710b35, 0x131c471b,
        0x28db77f5, 0x23047d84, 0x32caab7b, 0x40c72493,
        0x3c9ebe0a, 0x15c9bebc, 0x431d67c4, 0x9c100d4c,
        0x4cc5d4be, 0xcb3e42b6, 0xcb0ef593, 0xfc19dc6f,
        0x240ca1cc, 0x2de92c6f, 0x2e1b2138, 0x4d2c6dfc
    };

    public Sha512JC() {
        reset();
    }

    public void reset() {
        for (short i = 0; i < 16; i++) H[i] = H0[i];
        bitCountHi = 0;
        bitCountLo = 0;
        bufferOffset = 0;
    }

    public void update(byte[] inBuff, short inOffset, short inLength) {
        
    	for (short i = 0; i < inLength; i++) {
            buffer[bufferOffset++] = inBuff[(short)(inOffset + i)];
            addBitCount(8);
            if (bufferOffset == BLOCK_SIZE) {
                processBlock(buffer, (short)0);
                bufferOffset = 0;
            }
        }
    }

    public short doFinal(byte[] inBuff, short inOffset, short inLength,
                         byte[] outBuff, short outOffset) {
        // process remaining input
        update(inBuff, inOffset, inLength);

        // append 0x80
        buffer[bufferOffset++] = (byte)0x80;

        if (bufferOffset > BLOCK_SIZE - 16) {
            while (bufferOffset < BLOCK_SIZE) buffer[bufferOffset++] = 0;
            processBlock(buffer, (short)0);
            bufferOffset = 0;
        }

        while (bufferOffset < BLOCK_SIZE - 16) buffer[bufferOffset++] = 0;

        // append 128-bit length (we keep high 64 bits zero)
        int hi = bitCountHi;
        int lo = bitCountLo;
        for (short i = 0; i < 8; i++) {
            buffer[(short)(BLOCK_SIZE - 1 - i)] = (byte)(lo & 0xFF);
            lo = lo >>> 8;
        }
        for (short i = 0; i < 8; i++) {
            buffer[(short)(BLOCK_SIZE - 9 - i)] = (byte)(hi & 0xFF);
            hi = hi >>> 8;
        }
        processBlock(buffer, (short)0);

        // output H
        for (short i = 0; i < 8; i++) {
            int hi32 = H[(short)(i*2)];
            int lo32 = H[(short)(i*2+1)];
            int base = outOffset + i*8;
            outBuff[(short)(base + 0)] = (byte)(hi32 >>> 24);
            outBuff[(short)(base + 1)] = (byte)(hi32 >>> 16);
            outBuff[(short)(base + 2)] = (byte)(hi32 >>> 8);
            outBuff[(short)(base + 3)] = (byte)(hi32);
            outBuff[(short)(base + 4)] = (byte)(lo32 >>> 24);
            outBuff[(short)(base + 5)] = (byte)(lo32 >>> 16);
            outBuff[(short)(base + 6)] = (byte)(lo32 >>> 8);
            outBuff[(short)(base + 7)] = (byte)(lo32);
        }

        reset();
        return DIGEST_SIZE;
    }

    // increment 64-bit bit count (stored in two 32-bit ints as low/high parts)
    private void addBitCount(int bits) {
        int oldLo = bitCountLo;
        bitCountLo += bits;
        if (bitCountLo < oldLo) bitCountHi++;
    }

    // process 128-byte block at buffer[off..off+127]
    private void processBlock(byte[] data, short off) {
        // load W[0..15]
        int idx = off;
        for (int t = 0; t < 16; t++) {
            short base = (short)(idx + t*8);
            int hi = ((data[base] & 0xFF) << 24) | ((data[(short)(base+1)] & 0xFF) << 16) | ((data[(short)(base+2)] & 0xFF) << 8) | (data[(short)(base+3)] & 0xFF);
            int lo = ((data[(short)(base+4)] & 0xFF) << 24) | ((data[(short)(base+5)] & 0xFF) << 16) | ((data[(short)(base+6)] & 0xFF) << 8) | (data[(short)(base+7)] & 0xFF);
            W[(short)(t*2)] = hi; W[(short)(t*2+1)] = lo;
        }

        // expand W[16..79]
        for (int t = 16; t < 80; t++) {
            int[] s1 = smallSigma1(W[(short)((t-2)*2)], W[(short)((t-2)*2+1)]);
            int[] s0 = smallSigma0(W[(short)((t-15)*2)], W[(short)((t-15)*2+1)]);
            int[] tmp = add64pairs(s1[(short)0], s1[(short)1], W[(short)((t-7)*2)], W[(short)((t-7)*2+1)]);
            tmp = add64pairs(tmp[(short)0], tmp[(short)1], s0[(short)0], s0[(short)1]);
            tmp = add64pairs(tmp[(short)0], tmp[(short)1], W[(short)((t-16)*2)], W[(short)((t-16)*2+1)]);
            W[(short)(t*2)] = tmp[(short)0]; W[(short)(t*2+1)] = tmp[(short)1];
        }

        // initialize working variables a..h from H
        int aHi = H[(short)0], aLo = H[(short)1];
        int bHi = H[(short)2], bLo = H[(short)3];
        int cHi = H[(short)4], cLo = H[(short)5];
        int dHi = H[(short)6], dLo = H[(short)7];
        int eHi = H[(short)8], eLo = H[(short)9];
        int fHi = H[(short)10], fLo = H[(short)11];
        int gHi = H[(short)12], gLo = H[(short)13];
        int hHi = H[(short)14], hLo = H[(short)15];

        for (short t = 0; t < 80; t++) {
            int kHi = K[(short)(t*2)], kLo = K[(short)(t*2+1)];
            int[] S1 = bigSigma1(eHi, eLo);
            int[] ch = ch64(eHi, eLo, fHi, fLo, gHi, gLo);
            int[] Wt = new int[] { W[(short)(t*2)], W[(short)(t*2+1)] };

            int[] T1 = add64pairs(hHi, hLo, S1[(short)0], S1[(short)1]);
            T1 = add64pairs(T1[(short)0], T1[(short)1], ch[(short)0], ch[(short)1]);
            T1 = add64pairs(T1[(short)0], T1[(short)1], kHi, kLo);
            T1 = add64pairs(T1[(short)0], T1[(short)1], Wt[(short)0], Wt[(short)1]);

            int[] S0 = bigSigma0(aHi, aLo);
            int[] maj = maj64(aHi, aLo, bHi, bLo, cHi, cLo);
            int[] T2 = add64pairs(S0[(short)0], S0[(short)1], maj[(short)0], maj[(short)1]);

            // h = g
            hHi = gHi; hLo = gLo;
            // g = f
            gHi = fHi; gLo = fLo;
            // f = e
            fHi = eHi; fLo = eLo;
            // e = d + T1
            int[] dPlusT1 = add64pairs(dHi, dLo, T1[(short)0], T1[(short)1]);
            eHi = dPlusT1[(short)0]; eLo = dPlusT1[(short)1];
            // d = c
            dHi = cHi; dLo = cLo;
            // c = b
            cHi = bHi; cLo = bLo;
            // b = a
            bHi = aHi; bLo = aLo;
            // a = T1 + T2
            int[] aNew = add64pairs(T1[(short)0], T1[(short)1], T2[(short)0], T2[(short)1]);
            aHi = aNew[(short)0]; aLo = aNew[(short)1];
        }

        // add working vars back into H
        int[] sum;

        sum = add64pairs(H[(short)0], H[(short)1], aHi, aLo); H[(short)0] = sum[(short)0]; H[(short)1] = sum[(short)1];
        sum = add64pairs(H[(short)2], H[(short)3], bHi, bLo); H[(short)2] = sum[(short)0]; H[(short)3] = sum[(short)1];
        sum = add64pairs(H[(short)4], H[(short)5], cHi, cLo); H[(short)4] = sum[(short)0]; H[(short)5] = sum[(short)1];
        sum = add64pairs(H[(short)6], H[(short)7], dHi, dLo); H[(short)6] = sum[(short)0]; H[(short)7] = sum[(short)1];
        sum = add64pairs(H[(short)8], H[(short)9], eHi, eLo); H[(short)8] = sum[(short)0]; H[(short)9] = sum[(short)1];
        sum = add64pairs(H[(short)10], H[(short)11], fHi, fLo); H[(short)10] = sum[(short)0]; H[(short)11] = sum[(short)1];
        sum = add64pairs(H[(short)12], H[(short)13], gHi, gLo); H[(short)12] = sum[(short)0]; H[(short)13] = sum[(short)1];
        sum = add64pairs(H[(short)14], H[(short)15], hHi, hLo); H[(short)14] = sum[(short)0]; H[(short)15] = sum[(short)1];
    }

    // Add two 64-bit words represented as (hi1,lo1)+(hi2,lo2) without using long.
    // Returns array [resHi, resLo]
    private int[] add64pairs(int hi1, int lo1, int hi2, int lo2) {
        int sumLo = lo1 + lo2;
        int carry = (((lo1 & lo2) | ((lo1 | lo2) & ~sumLo)) >>> 31); // carry 0 or 1
        int resHi = hi1 + hi2 + carry;
        return new int[] { resHi, sumLo };
    }

    // rotate right 64-bit (hi,lo) by n (0<=n<64)
    private int[] rotr64(int hi, int lo, int n) {
        n &= 63;
        if (n == 0) return new int[] { hi, lo };
        if (n < 32) {
            int newHi = (hi >>> n) | (lo << (32 - n));
            int newLo = (lo >>> n) | (hi << (32 - n));
            return new int[] { newHi, newLo };
        } else {
            int m = n - 32;
            int newHi = (lo >>> m) | (hi << (32 - m));
            int newLo = (hi >>> m) | (lo << (32 - m));
            return new int[] { newHi, newLo };
        }
    }

    private int[] shr64(int hi, int lo, int n) {
        if (n == 0) return new int[] { hi, lo };
        if (n < 32) {
            int newHi = hi >>> n;
            int newLo = (lo >>> n) | (hi << (32 - n));
            return new int[] { newHi, newLo };
        } else {
            int m = n - 32;
            int newHi = 0;
            int newLo = hi >>> m;
            return new int[] { newHi, newLo };
        }
    }

    private int[] smallSigma0(int hi, int lo) {
        int[] r1 = rotr64(hi, lo, 1);
        int[] r8 = rotr64(hi, lo, 8);
        int[] s = shr64(hi, lo, 7);
        return new int[] { r1[(short)0] ^ r8[(short)0] ^ s[(short)0], r1[(short)1] ^ r8[(short)1] ^ s[(short)1] };
    }

    private int[] smallSigma1(int hi, int lo) {
        int[] r19 = rotr64(hi, lo, 19);
        int[] r61 = rotr64(hi, lo, 61);
        int[] s = shr64(hi, lo, 6);
        return new int[] { r19[(short)0] ^ r61[(short)0] ^ s[(short)0], r19[(short)1] ^ r61[(short)1] ^ s[(short)1] };
    }

    private int[] bigSigma0(int hi, int lo) {
        int[] r28 = rotr64(hi, lo, 28);
        int[] r34 = rotr64(hi, lo, 34);
        int[] r39 = rotr64(hi, lo, 39);
        return new int[] { r28[(short)0] ^ r34[(short)0] ^ r39[(short)0], r28[(short)1] ^ r34[(short)1] ^ r39[(short)1] };
    }

    private int[] bigSigma1(int hi, int lo) {
        int[] r14 = rotr64(hi, lo, 14);
        int[] r18 = rotr64(hi, lo, 18);
        int[] r41 = rotr64(hi, lo, 41);
        return new int[] { r14[(short)0] ^ r18[(short)0] ^ r41[(short)0], r14[(short)1] ^ r18[(short)1] ^ r41[(short)1] };
    }

    private int[] ch64(int xHi, int xLo, int yHi, int yLo, int zHi, int zLo) {
        return new int[] { (xHi & yHi) ^ (~xHi & zHi), (xLo & yLo) ^ (~xLo & zLo) };
    }

    private int[] maj64(int xHi, int xLo, int yHi, int yLo, int zHi, int zLo) {
        return new int[] { (xHi & yHi) ^ (xHi & zHi) ^ (yHi & zHi), (xLo & yLo) ^ (xLo & zLo) ^ (yLo & zLo) };
    }
}
