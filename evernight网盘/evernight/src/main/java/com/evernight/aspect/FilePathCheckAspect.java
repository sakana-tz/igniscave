package com.evernight.aspect;

import com.evernight.base.exception.BusinessException;
import com.evernight.entity.annotation.CheckFilePath;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.regex.Pattern;


/**
 * 文件路径安全检查切面
 */
@Aspect
@Component
@Order(1)
public class FilePathCheckAspect {

    // 定义安全字符：0-9, A-Z, a-z, 中文字符
    private static final Pattern SAFE_CHAR_PATTERN = Pattern.compile("[0-9A-Za-z\\u4e00-\\u9fa5]");
    private static final Pattern CONSECUTIVE_UNSAFE_CHARS = Pattern.compile("[^0-9A-Za-z\\u4e00-\\u9fa5]+");
    private static final Pattern UNSAFE_PATH_PATTERN = Pattern.compile("(\\.\\.|\\/\\/|\\\\\\\\)");
    private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("^([a-zA-Z]:\\\\|\\\\\\\\)|^/");

    // 专门处理连续斜杠的正则
    private static final Pattern MULTIPLE_SLASHES_PATTERN = Pattern.compile("[/\\\\]+");
    private static final Pattern WINDOWS_PATH_PREFIX = Pattern.compile("^[a-zA-Z]:\\\\");


    /**
     * 拦截所有带有 @CheckFilePath 注解的参数
     */
    @Around("execution(* *(.., @com.evernight.entity.annotation.CheckFilePath (*), ..))")
    public Object checkAndNormalizeFilePath(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名和参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        // 遍历参数，找到标注了@CheckFilePath的参数
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CheckFilePath.class)) {
                CheckFilePath annotation = parameters[i].getAnnotation(CheckFilePath.class);

                // 获取路径值
                Object pathObj = args[i];
                if (pathObj == null) {
                    throw new IllegalArgumentException("路径参数不能为null:");
                }

                String path = pathObj.toString();

                // 1. 格式修正
                if (annotation.formatCorrection()) {
                    path = normalizePath(path);
                }

                // 2. 如果是目录，确保以斜杠结尾
                if (annotation.isDirectory()) {
                    path = ensureDirectoryEndsWithSlash(path);
                }

                // 3. 应用前缀（如果有）
                if (!annotation.prefix().isEmpty()) {
                    path = applyPrefix(path, annotation.prefix(), annotation.isDirectory());
                }

                // 4. 路径安全检查
                try {
                    validatePath(path, annotation);
                }catch (Exception e){
                    throw new BusinessException(annotation.message());
                }

                // 5. 更新参数值
                args[i] = path;
            }
        }

        // 使用处理后的参数继续执行
        return joinPoint.proceed(args);
    }

    /**
     * 路径规范化：将连续不安全字符替换为单个斜杠
     * 特别处理：将//、\\、///等都替换为/
     */
    private String normalizePath(String originalPath) {

        // 将所有反斜杠统一为正斜杠
        String normalized = originalPath.replace('\\', '/');

        // 将连续的非安全字符（除斜杠外）替换为单个斜杠
        normalized = CONSECUTIVE_UNSAFE_CHARS.matcher(normalized).replaceAll("/");

        // 再次确保没有连续的斜杠（处理可能由第三步产生的新连续斜杠）
        normalized = MULTIPLE_SLASHES_PATTERN.matcher(normalized).replaceAll("/");

        // 移除开头和结尾的斜杠（后续会根据需要添加）
        // 但如果是根路径"/"，我们保留
        if ("/".equals(normalized)) {
            return normalized;
        }

        // 移除开头和结尾的斜杠
        normalized = normalized.replaceAll("^/+", "");
        normalized = normalized.replaceAll("/+$", "");

        return normalized;
    }

    /**
     * 如果是目录，确保以斜杠结尾
     */
    private String ensureDirectoryEndsWithSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // 如果已经是根路径，直接返回
        if ("/".equals(path)) {
            return path;
        }

        if (!path.endsWith("/")) {
            return path + "/";
        }

        return path;
    }

    /**
     * 应用路径前缀
     */
    private String applyPrefix(String path, String prefix, boolean isDirectory) {
        if (prefix == null || prefix.isEmpty()) {
            return path;
        }

        // 规范化前缀
        String normalizedPrefix = normalizePath(prefix);

        // 确保前缀以斜杠结尾（如果是目录）或不以斜杠开头（如果不是目录）
        if (isDirectory) {
            if (!normalizedPrefix.endsWith("/")) {
                normalizedPrefix = normalizedPrefix + "/";
            }
        } else {
            // 如果不是目录，确保前缀不以斜杠结尾
            if (normalizedPrefix.endsWith("/")) {
                normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
            }
        }

        // 如果路径为空，只返回前缀
        if (path == null || path.isEmpty()) {
            return normalizedPrefix;
        }

        // 如果前缀已经是根路径，直接组合
        if ("/".equals(normalizedPrefix)) {
            return normalizedPrefix + path;
        }

        // 确保路径以指定前缀开头
        if (!path.startsWith(normalizedPrefix)) {
            // 如果前缀以斜杠结尾，路径以斜杠开头，去掉一个斜杠
            if (normalizedPrefix.endsWith("/") && path.startsWith("/")) {
                return normalizedPrefix + path.substring(1);
            }
            // 如果前缀不以斜杠结尾，路径不以斜杠开头，添加斜杠
            else if (!normalizedPrefix.endsWith("/") && !path.startsWith("/")) {
                return normalizedPrefix + "/" + path;
            }
            // 其他情况直接拼接
            else {
                return normalizedPrefix + path;
            }
        }

        return path;
    }

    /**
     * 路径安全检查
     */
    private void validatePath(String path, CheckFilePath annotation) {
        // 检查路径长度
        if (path.length() > annotation.maxLength()) {
            throw new SecurityException(annotation.message() + ": 路径长度超过限制 (" + annotation.maxLength() + ")");
        }

        // 检查是否包含危险路径模式
        if (UNSAFE_PATH_PATTERN.matcher(path).find()) {
            throw new SecurityException(annotation.message() + ": 路径包含危险字符或模式");
        }

        // 检查是否为绝对路径（可选，根据需求调整）
        if (ABSOLUTE_PATH_PATTERN.matcher(path).find()) {
            throw new SecurityException(annotation.message() + ": 不允许使用绝对路径");
        }

        // 检查文件扩展名（如果不是目录）
        if (!annotation.isDirectory() && annotation.allowedExtensions().length > 0) {
            String filename = extractFilename(path);
            if (!isExtensionAllowed(filename, annotation.allowedExtensions())) {
                throw new SecurityException(annotation.message() +
                        ": 文件类型不允许。允许的类型: " +
                        String.join(", ", annotation.allowedExtensions()));
            }
        }

        // 检查路径遍历攻击 (../ 或 ..\)
        if (path.contains("..")) {
            throw new SecurityException(annotation.message() + ": 路径包含目录遍历攻击");
        }

        // 检查空字节攻击
        if (path.contains("\0") || path.contains("%00")) {
            throw new SecurityException(annotation.message() + ": 路径包含空字节");
        }

        // 额外检查：确保没有非法的斜杠（规范化后应该已经处理，但作为安全验证）
        if (path.contains("//") || path.contains("\\")) {
            throw new SecurityException(annotation.message() + ": 路径包含非法斜杠");
        }
    }

    /**
     * 从路径中提取文件名
     */
    private String extractFilename(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 处理以斜杠结尾的目录路径
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        int lastSlashIndex = cleanPath.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < cleanPath.length() - 1) {
            return cleanPath.substring(lastSlashIndex + 1);
        }

        return cleanPath;
    }

    /**
     * 检查文件扩展名是否允许
     */
    private boolean isExtensionAllowed(String filename, String[] allowedExtensions) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == filename.length() - 1) {
            // 没有扩展名或扩展名为空
            return false;
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();

        for (String allowedExt : allowedExtensions) {
            if (allowedExt != null && allowedExt.toLowerCase().equals(extension)) {
                return true;
            }
        }

        return false;
    }

}

