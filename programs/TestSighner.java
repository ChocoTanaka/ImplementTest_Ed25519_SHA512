package com.example.sighner;

public class TestSighner {

	public static void main(String[] args) {
		
		/*
        // 例: 32バイトの秘密鍵
        int[] prikeyint = {
        		254, 165, 104, 138, 94, 135, 73, 55, 170, 104,
        		105, 97, 218, 182, 60, 80, 113, 16, 187, 100,
        		126, 230, 174, 12, 186, 100, 231, 231, 28, 29,
        		251, 244
        		};
        byte[] privKey = new byte[prikeyint.length];
        for (int i = 0; i <prikeyint.length; i++) {
            privKey[i] = (byte)prikeyint[i]; // キャストでbyteに変換
        }

        // 例: messageBuffer を外部で作成
        int[] messageint = {73, 214, 225, 206, 39, 106, 133, 183, 14, 175, 229, 35, 73, 170, 204, 163, 137, 48, 46, 122, 151, 84, 188, 241, 34, 30, 121, 73, 79, 198, 101, 164, 1, 152, 84, 65, 96, 109, 0, 0, 0, 0, 0, 0, 56, 226, 30, 253, 20, 0, 0, 0, 152, 154, 124, 153, 141, 134, 177, 90, 85, 50, 156, 158, 66, 100, 0, 116, 84, 64, 249, 169, 122, 120, 103, 94, 0, 0, 1, 0, 0, 0, 0, 0, 206, 139, 160, 103, 46, 33, 192, 114, 0, 225, 245, 5, 0, 0, 0, 0};
        
        byte[] messageBuffer = new byte[messageint.length];
        for (int i = 0; i <messageint.length; i++) {
        	messageBuffer[i] = (byte)messageint[i]; // キャストでbyteに変換
        }
*/
		String privstr = "ABF4CF55A2B3F742D7543D9CC17F50447B969E6E06F5EA9195D428AB12B7318D";
		byte[] privKey = hexStringToByteArray(privstr);
		
		//System.out.print(privKey);
		System.out.println("length:" +privKey.length);
		//true:32
		
		
		String datastr = "8CE03CD60514233B86789729102EA09E867FC6D964DEA8C2018EF7D0A2E0E24BF7E348E917116690B9";
		byte[] messageBuffer = hexStringToByteArray(datastr);
		
		System.out.println("length:" +messageBuffer.length);
		//true: written length
		
		
        Ed25519SignerJC signer = new Ed25519SignerJC(privKey,(short)0,(short)32);
        byte[] signature = new byte[64];

        signer.sign(privKey, (short)0, (short)messageBuffer.length, messageBuffer, signature, (short)0);

        System.out.print("Signature: ");
        for (byte b : signature) {
            System.out.printf("%02X", b);
        }
        System.out.print("len:"+signature.length);
    }
	
	
	public static byte[] hexStringToByteArray(String hex) {
	    if (hex == null) throw new IllegalArgumentException("hex is null");
	    int len = hex.length();
	    if ((len & 1) != 0) throw new IllegalArgumentException("Hex string must have even length");

	    byte[] out = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        int hi = Character.digit(hex.charAt(i), 16);
	        int lo = Character.digit(hex.charAt(i + 1), 16);
	        if (hi == -1 || lo == -1) {
	            throw new IllegalArgumentException("Invalid hex character at position " + i);
	        }
	        out[i / 2] = (byte) ((hi << 4) + lo);
	    }
	    return out;
	}
}
