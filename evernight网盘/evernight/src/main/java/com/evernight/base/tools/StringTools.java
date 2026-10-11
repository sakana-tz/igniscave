package com.evernight.base.tools;

import com.evernight.base.exception.BusinessException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class StringTools {

    /**
     * 检查对象参数是否包含非空字段,(确保至少有一个非空条件)
     *
     * @param param 待检查的对象参数
     * @throws BusinessException 当所有字段都为空时抛出异常
     */
    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;

            // 遍历所有字段，检查是否有非空字段
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);

                // 判断字段是否非空
                // 字符串类型：不为空且不等于"null"
                // 非字符串类型：不为null即可
                if (object instanceof String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }

            // 如果所有字段都为空，则抛出异常
            if (!notEmpty) {
                throw new BusinessException("多参数更新，删除，必须有非空条件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    /**
     * 将字符串首字母大写
     * 特殊情况：如果第二个字母是大写，则保持原样
     *
     * @param field 输入字符串
     * @return 首字母大写的字符串，如果输入为空则返回原值
     */
    public static String upperCaseFirstLetter(String field) {
        // 如果字符串为空，直接返回
        if (isEmpty(field)) {
            return field;
        }

        // 如果字符串长度大于1且第二个字符是大写，则不处理首字母
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }

        // 将首字母大写并拼接剩余部分
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    /**
     * 判断字符串是否为空
     * 包括null、空字符串、"null"字符串、"\u0000"字符串以及只包含空白字符的情况
     *
     * @param str 待检查的字符串
     * @return 如果字符串为空则返回true，否则返回false
     */
    public static boolean isEmpty(String str) {
        // 检查各种空值情况
        if (null == str || str.isEmpty() || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else {
            // 检查去除空白字符后是否为空
            return str.trim().isEmpty();
        }
    }

    /**
     * 对密码进行哈希处理（使用SHA-256算法加密）
     *
     * @param password 要处理的密码字符串
     * @return 处理后的密码字符串
     */
    public static String hashWithSHA256(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            // 创建SHA-256摘要实例
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            // 对密码进行哈希处理
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
