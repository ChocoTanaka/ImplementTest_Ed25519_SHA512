package com.example.sighner;

public class TestSighner2 {

	public static void main(String[] args) {
		
		int num = 3;
		
		String privstr =   teststr.PriKey[num];
		byte[] privKey = util.HexUtil.hexStringToByteArray(privstr);

		
		String PubKeystr = teststr.PubKey[num];
		byte[] pubKey = util.HexUtil.hexStringToByteArray(PubKeystr);
		
		//System.out.println(util.HexUtil.byteArrayToHexString(pubKey));
		
		String datastr = teststr.Data[num];
		byte[] messageBuffer = util.HexUtil.hexStringToByteArray(datastr);
		
		String Signaturestr =  teststr.Sig[num];
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
