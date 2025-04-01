package fun.timu.live.common.interfaces.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.Key;
import java.security.SecureRandom;

public class DESUtils {
    // 算法名称
    public static final String KEY_ALGORITHM = "DES";
    // 算法名称/加密模式/填充方式
    // DES共有四种工作模式-->>ECB：电子密码本模式、CBC：加密分组链接模式、CFB：加密反馈模式、OFB：输出反馈模式
    public static final String CIPHER_ALGORITHM = "DES/ECB/PKCS5Padding";
    public static final String PUBLIC_KEY = "BAS9j2C3D4E5F60708";


    /**
     * 生成一个SecretKey对象，用于加密和解密数据
     * 此方法使用DES算法，基于输入的密钥字符串生成一个密钥对象
     *
     * @param keyStr 一个代表密钥的十六进制字符串，将被转换为密钥对象
     * @return SecretKey对象，用于加密和解密数据
     * @throws Exception 如果密钥字符串不符合要求或密钥生成过程中出现问题，则抛出异常
     */
    private static SecretKey keyGenerator(String keyStr) throws Exception {
        // 将十六进制字符串格式的密钥转换为字节数组
        byte input[] = HexString2Bytes(keyStr);
        // 创建一个DES密钥规范对象，基于输入的字节数组
        DESKeySpec desKey = new DESKeySpec(input);
        // 创建一个密匙工厂，然后用它把DESKeySpec转换成
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        // 使用密钥工厂将DES密钥规范对象转换为SecretKey对象
        SecretKey securekey = keyFactory.generateSecret(desKey);
        // 返回生成的SecretKey对象
        return securekey;
    }

    /**
     * 将字符转换为它的十进制表示形式
     * 此方法专注于处理十六进制字符（0-9，A-F，a-f），并将其映射到0-15的十进制值
     * 对于非十六进制字符，结果是未定义的
     *
     * @param c 待转换的字符
     * @return 字符的十进制值，对于十六进制字符，返回0到15之间的值
     */
    private static int parse(char c) {
        // 当字符为小写字母时，将其转换为对应的十进制值
        if (c >= 'a') return (c - 'a' + 10) & 0x0f;
        // 当字符为大写字母时，将其转换为对应的十进制值
        if (c >= 'A') return (c - 'A' + 10) & 0x0f;
        // 当字符为数字时，直接将其转换为十进制值
        return (c - '0') & 0x0f;
    }

    /**
     * 将十六进制字符串转换为字节数组
     * 此方法用于解析十六进制字符串，将其转换为对应的字节数组
     * 十六进制字符串是字节数据的常见表示形式，每个字节可以表示为两个十六进制字符
     * 通过这个方法，我们可以恢复原始的字节数组，以便进行进一步的处理
     *
     * @param hexstr 十六进制字符串，包含要转换的数据
     * @return 字节数组，转换后的原始数据
     */
    public static byte[] HexString2Bytes(String hexstr) {
        // 根据十六进制字符串的长度，初始化字节数组的长度
        // 每两个十六进制字符代表一个字节，因此字节数组的长度是十六进制字符串长度的一半
        byte[] b = new byte[hexstr.length() / 2];
        // 用于遍历十六进制字符串的索引变量
        int j = 0;
        // 遍历字节数组，将每两个十六进制字符转换为一个字节
        for (int i = 0; i < b.length; i++) {
            // 获取当前字节的高四位对应的十六进制字符
            char c0 = hexstr.charAt(j++);
            // 获取当前字节的低四位对应的十六进制字符
            char c1 = hexstr.charAt(j++);
            // 将两个十六进制字符转换为一个字节，并存储在字节数组中
            // 高位字符左移4位，然后与低位字符进行按位或操作，以组合成一个完整的字节
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
        }
        // 返回转换后的字节数组
        return b;
    }

    /**
     * 加密数据
     *
     * @param data 待加密数据
     * @return 加密后的数据
     */
    public static String encrypt(String data) {
        Key deskey = null;
        try {
            // 生成密钥
            deskey = keyGenerator(PUBLIC_KEY);
            // 实例化Cipher对象，它用于完成实际的加密操作
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            // 使用SecureRandom来初始化Cipher对象的加密模式，以提高安全性
            SecureRandom random = new SecureRandom();
            // 初始化Cipher对象，设置为加密模式
            cipher.init(Cipher.ENCRYPT_MODE, deskey, random);
            // 执行加密操作。加密后的结果通常都会用Base64编码进行传输
            byte[] results = cipher.doFinal(data.getBytes());
            return Base64.encodeBase64String(results);
        } catch (Exception e) {
            // 如果加密过程中发生异常，将其包装为RuntimeException并抛出
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密数据
     *
     * @param data 待解密数据
     * @return 解密后的数据
     */
    public static String decrypt(String data) {
        Key deskey = null;
        try {
            deskey = keyGenerator(PUBLIC_KEY);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            // 初始化Cipher对象，设置为解密模式
            cipher.init(Cipher.DECRYPT_MODE, deskey);
            // 执行解密操作
            return new String(cipher.doFinal(Base64.decodeBase64(data)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String phone = "17889289032";
        String encryptStr = DESUtils.encrypt(phone);
        String decryStr = DESUtils.decrypt(encryptStr);
        System.out.println(encryptStr);
        System.out.println(decryStr);
    }

}
