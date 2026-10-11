package com.evernight.components.aliOssComponent;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class OssTools {


    // AES加密算法参数
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    // IV初始向量
    private static final byte[] IV = "2431576890123456".getBytes(); // 16字节IV
    // 文件类型映射表
    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();
    static {
        // 初始化文件类型映射
        CONTENT_TYPE_MAP.put(".jpg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".jpeg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".png", "image/png");
        CONTENT_TYPE_MAP.put(".gif", "image/gif");
        CONTENT_TYPE_MAP.put(".bmp", "image/bmp");
        CONTENT_TYPE_MAP.put(".webp", "image/webp");
        CONTENT_TYPE_MAP.put(".pdf", "application/pdf");
        CONTENT_TYPE_MAP.put(".mp4", "video/mp4");
        CONTENT_TYPE_MAP.put(".avi", "video/x-msvideo");
        CONTENT_TYPE_MAP.put(".mkv", "video/x-matroska");
        CONTENT_TYPE_MAP.put(".mp3", "audio/mpeg");
        CONTENT_TYPE_MAP.put(".wav", "audio/wav");
        CONTENT_TYPE_MAP.put(".flac", "audio/flac");
        CONTENT_TYPE_MAP.put(".doc", "application/msword");
        CONTENT_TYPE_MAP.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        CONTENT_TYPE_MAP.put(".xls", "application/vnd.ms-excel");
        CONTENT_TYPE_MAP.put(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        CONTENT_TYPE_MAP.put(".ppt", "application/vnd.ms-powerpoint");
        CONTENT_TYPE_MAP.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        CONTENT_TYPE_MAP.put(".txt", "text/plain");
        CONTENT_TYPE_MAP.put(".zip", "application/zip");
        CONTENT_TYPE_MAP.put(".rar", "application/x-rar-compressed");
        CONTENT_TYPE_MAP.put(".7z", "application/x-7z-compressed");
    }

    /**
     * AES加密
     *
     * @param plainText 明文
     * @param secretKey 原始密钥（任意长度）
     * @return URL安全的Base64编码的密文
     */
    static String encrypt(String plainText, String secretKey) throws Exception {
        // 使用SHA-256将任意长度密钥转换为32字节密钥
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));

        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 使用URL安全的Base64编码，避免特殊字符问题
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    }

    /**
     * AES解密
     *
     * @param encryptedText URL安全的Base64编码的密文
     * @param secretKey     原始密钥（任意长度）
     * @return 解密后的明文
     */
    static String decrypt(String encryptedText, String secretKey) throws Exception {
        // 使用SHA-256将任意长度密钥转换为32字节密钥
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));

        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));

        // 移除可能的空格，然后使用URL安全的Base64解码
        encryptedText = encryptedText.replaceAll("\\s+", "");
        byte[] decoded = Base64.getUrlDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 根据文件名获取对应的MIME类型。
     *
     * @param filename 文件名（需包含扩展名）
     * @return 对应的MIME类型，若未匹配到则返回null
     */
    public static String getContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null; // 处理空文件名情况
        }

        // 转换为小写并提取扩展名
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();

        // 从映射表中查找对应MIME类型
        return CONTENT_TYPE_MAP.getOrDefault(extension, null);
    }
}
