package com.example.sighner;

public class TestSighner {

	public static void main(String[] args) {
		
		String privstr = "E6C1685E7AC64EFB63B77D55E05D7A1D1036B21A2831A250564653DC0A137CB7";
		byte[] privKey = util.HexUtil.hexStringToByteArray(privstr);

		
		String PubKeystr = "2E71772EA2A8216A6489235DC630E2948B1320E5BCAFE9F9D624F1B9515F5871";
		byte[] pubKey = util.HexUtil.hexStringToByteArray(PubKeystr);
		
		//System.out.println(util.HexUtil.byteArrayToHexString(pubKey));
		
		String datastr = "A50E6D1BCF638075E64C9E1B17C4DF9D21EA977878E2852628A485A29D8456FC7073921DE0A972E9DA4AC861";
		byte[] messageBuffer = util.HexUtil.hexStringToByteArray(datastr);
		
		String Signaturestr = "16B8630CE5268C4AF312EDF8791DB70FBB1F1ACDA6E1D468A3A266C221574D9E3C666373B4CCB68FDE86270A8EC7C1228A0E4E4073CFE9807364FED3CE231304";
		byte[] expectedSignature = util.HexUtil.hexStringToByteArray(Signaturestr);
		
		TweetNacl Nacl = new TweetNacl();
		
		// --- キーペア生成 ---
        byte[] sk = new byte[64]; // Secret key: 前半32バイトがprivate、後半32バイトがpublic
        System.arraycopy(privKey, 0, sk, 0, 32);

        byte[] pk = new byte[32]; // Public key
        Nacl.crypto_sign_keypair(pk, sk, true);

     // --- 公開鍵チェック ---
        System.out.println("Generated Public Key: " +util.HexUtil.byteArrayToHexString(pk));
        System.out.println("Matches expected? " + java.util.Arrays.equals(pk, pubKey));
        
        byte[] signed = new byte[64 + messageBuffer.length]; // 署名 + データ
        Nacl.crypto_sign(signed, (short)0, messageBuffer, messageBuffer.length, sk);

        // --- 署名部分とデータ部分に分けてチェック ---
        byte[] signature = new byte[64];
        byte[] signedData = new byte[messageBuffer.length];
        System.arraycopy(signed, 0, signature, 0, 64);
        System.arraycopy(signed, 64, signedData, 0, messageBuffer.length);

        System.out.println("Generated Signature: " + util.HexUtil.byteArrayToHexString(signature));
        System.out.println("Matches expected signature? " + java.util.Arrays.equals(signature, expectedSignature));

        System.out.println("Signed data matches original? " + java.util.Arrays.equals(signedData, messageBuffer));
    
    }

}
