package xiaolin.Util;

import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAUtil {

    public static final String TAG = "RSAUtil";
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * RSA公密 加密数据
     *
     * @param publicKey 公钥
     * @param val       加密的数据
     */
    public static byte[] encrypt(String publicKey, byte[] val) {
        byte[] keyBytes = Base64.decode(publicKey, Base64.NO_WRAP);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            Key publicK = keyFactory.generatePublic(x509KeySpec);
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicK);
            return cipher.doFinal(val);
        } catch (NoSuchAlgorithmException
                | InvalidKeySpecException
                | NoSuchPaddingException
                | IllegalBlockSizeException
                | InvalidKeyException
                | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * RSA公密 加密数据, 加密后通过Base64编码
     *
     * @param publicKey 公钥
     * @param val       加密的数据
     */
    public static String encryptToBase64(String publicKey, byte[] val) {
        return Base64.encodeToString(encrypt(publicKey, val), Base64.NO_WRAP);
    }

}
