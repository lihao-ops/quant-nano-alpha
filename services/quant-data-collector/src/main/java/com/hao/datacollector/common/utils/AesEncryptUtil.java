package com.hao.datacollector.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author LiHao
 * @program: wstock-business-service
 * @description: 封装最新AES对称加密工具类
 * @Date 2024-08-09 14:35:31
 */
public class AesEncryptUtil {
    /**
     * 加密算法
     */
    private static final String ALGORITHM = "AES";
    /**
     * 编码方式
     */
    private static final String CHARSET = "UTF-8";

    /**
     * 生成随机密钥
     *
     * @return Base64编码的字符串密钥
     * @throws Exception 异常
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        //选择128位密钥
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * 将Base64编码的密钥字符串转换为SecretKeySpec对象
     *
     * @param key Base64编码的密钥字符串
     * @return SecretKeySpec 对象
     */
    private static SecretKeySpec getSecretKey(String key) {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密数据
     *
     * @param data 待加密数据
     * @param key  Base64编码的密钥字符串
     * @return 加密后的数据，使用Base64编码
     * @throws Exception 异常
     */
    public static String encrypt(String data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec secretKey = getSecretKey(key);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 解密数据
     *
     * @param encryptedData 已加密的数据，Base64编码
     * @param key           Base64编码的密钥字符串
     * @return 解密后的数据
     * @throws Exception 异常
     */
    public static String decrypt(String encryptedData, String key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec secretKey = getSecretKey(key);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        try {
            // 生成随机密钥
            String key = generateKey();
            System.out.println("生成的随机密钥: " + key);

            // 原始数据
            String originalData = "Hello Spring Boot!";

            // 加密
            String encryptedData = encrypt(originalData, key);
            System.out.println("加密后的数据: " + encryptedData);

            // 解密
            String decryptedData = decrypt(encryptedData, key);
            System.out.println("解密后的数据: " + decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}