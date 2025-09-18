package com.example.sighner;

public class TestSighner {

	public static void main(String[] args) {
		
		String privstr = "6AA6DAD25D3ACB3385D5643293133936CDDDD7F7E11818771DB1FF2F9D3F9215";
		byte[] privKey = util.HexUtil.hexStringToByteArray(privstr);

		
		String PubKeystr = "F7BBE3BB4DBF9698122DA02EB8A6EDE55F1EF90D0C64819E8A792231A2A0B143";
		byte[] pubKey = util.HexUtil.hexStringToByteArray(PubKeystr);
		
		//System.out.println(util.HexUtil.byteArrayToHexString(pubKey));
		
		String datastr = "E4A92208A6FC52282B620699191EE6FB9CF04DAF48B48FD542C5E43DAA9897763A199AAA4B6F10546109F47AC3564FADE0";
		byte[] messageBuffer = util.HexUtil.hexStringToByteArray(datastr);
		
		String Signaturestr = "F21E4BE0A914C0C023F724E1EAB9071A3743887BB8824CB170404475873A827B301464261E93700725E8D4427A3E39D365AFB2C9191F75D33C6BE55896E0CC00";
		byte[] expectedSignature = util.HexUtil.hexStringToByteArray(Signaturestr);
		
		// --- キーペア生成 ---
        byte[] sk = new byte[64]; // Secret key: 前半32バイトがprivate、後半32バイトがpublic
        System.arraycopy(privKey, 0, sk, 0, 32);

        byte[] pk = new byte[32]; // Public key
        TweetNacl.crypto_sign_keypair(pk, sk, true);
        
        System.out.println("length:" + pk.length);
        
     // --- 公開鍵チェック ---
        System.out.println("Generated Public Key: " +util.HexUtil.byteArrayToHexString(pk));
        System.out.println("Matches expected? " + java.util.Arrays.equals(pk, pubKey));
		
        byte[] signed = new byte[64 + messageBuffer.length]; // 署名 + データ
        TweetNacl.crypto_sign(signed, (short)0, messageBuffer, messageBuffer.length, sk);

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
