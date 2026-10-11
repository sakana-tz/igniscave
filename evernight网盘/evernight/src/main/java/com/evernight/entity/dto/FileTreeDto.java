package com.evernight.entity.dto;

import com.evernight.base.entity.po.File;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)// 忽略未知属性
@Accessors(chain = true)
public class FileTreeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long fileId;              // 文件ID
    private boolean isDel;            // 是否删除
    private String label;             // 节点显示名称
    private String path;              // 完整路径
    private Integer type;             // 类型：0:文件,1:文件夹
    private Long size;                // 文件大小
    private LocalDateTime updateTime;       // 更新时间
    @JsonDeserialize(contentAs = FileTreeDto.class)
    private List<FileTreeDto> children = new ArrayList<>();

    // 构造函数,文件夹
    public FileTreeDto(String label, String path, Integer type) {
        this.label = label;
        this.path = path;
        this.type = type;
    }

    // 构造函数,文件
    public FileTreeDto(Long fileId, boolean isDel, String label, String path, Integer type, Long size, LocalDateTime updateTime) {
        this.fileId = fileId;
        this.isDel = isDel;
        this.label = label;
        this.path = path;
        this.type = type;
        this.size = size;
        this.updateTime = updateTime;
    }

    /**
     * 使用流式API递归查找FileTreeDto
     *
     * @param trees  文件树列表
     * @param fileId 目标文件ID
     * @return 找到的FileTreeDto节点，未找到返回null
     */
    public static FileTreeDto findFileTreeDtoById(List<FileTreeDto> trees, Long fileId) {
        return trees.stream()
                .filter(Objects::nonNull)
                .map(tree -> searchTree(tree, fileId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 递归搜索单棵树
     */
    private static FileTreeDto searchTree(FileTreeDto node, Long targetId) {
        if (node == null) return null;

        // 检查当前节点
        if (targetId.equals(node.getFileId())) {
            return node;
        }

        // 递归搜索子节点
        return node.getChildren().stream()
                .map(child -> searchTree(child, targetId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public boolean getDel() {
        return isDel;
    }

    public String getFileName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * 将文件列表转换为树形结构
     *
     * @param files 文件列表，每个文件包含 filePath 属性（如："11/22/3.png", "11/33/4.png", "11/44/55/6.jpg"）
     * @return 树形结构列表
     */
    public List<FileTreeDto> files2Tree(List<File> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用 Map 来存储已创建的节点，key 为路径
        Map<String, FileTreeDto> nodeMap = new HashMap<>();
        List<FileTreeDto> rootNodes = new ArrayList<>();

        // 先对文件按路径进行排序，确保父节点先被处理
        List<File> sortedFiles = files.stream()
                .sorted(Comparator.comparing(File::getFilePath))
                .toList();
        for (File file : sortedFiles) {
            String filePath = file.getFilePath();
            if (filePath == null || filePath.trim().isEmpty()) {
                continue;
            }

            // 分割路径，获取所有层级
            String[] pathParts = filePath.split("/");
            String currentPath = "";

            // 逐级构建文件夹节点
            for (int i = 0; i < pathParts.length; i++) {
                String part = pathParts[i];
                String parentPath = currentPath;
                currentPath = currentPath.isEmpty() ? part : currentPath + "/" + part;

                // 检查节点是否已存在
                FileTreeDto currentNode = nodeMap.get(currentPath);

                if (currentNode == null) {
                    // 创建新节点
                    if (i == pathParts.length - 1) {
                        // 最后一部分是文件
                        currentNode = new FileTreeDto(
                                file.getFileName() != null ? file.getFileName() : part,
                                currentPath,
                                0  // 文件类型
                        );
                    } else {
                        // 中间部分是文件夹
                        currentNode = new FileTreeDto(
                                part,
                                currentPath,
                                1  // 文件夹类型
                        );
                    }

                    // 添加到 Map
                    nodeMap.put(currentPath, currentNode);

                    // 添加到父节点的 children 中，或者作为根节点
                    if (parentPath.isEmpty()) {
                        // 没有父路径，是根节点
                        if (!rootNodes.contains(currentNode)) {
                            rootNodes.add(currentNode);
                        }
                    } else {
                        // 有父路径，添加到父节点的 children 中
                        FileTreeDto parentNode = nodeMap.get(parentPath);
                        if (parentNode != null && parentNode.getChildren() != null) {
                            // 检查是否已存在相同的子节点
                            final String finalCurrentPath = currentPath; // 创建final副本供Lambda使用
                            boolean exists = parentNode.getChildren().stream()
                                    .anyMatch(child -> child.getPath().equals(finalCurrentPath));
                            if (!exists) {
                                parentNode.getChildren().add(currentNode);
                            }
                        }
                    }
                }

                // 如果是文件，可以设置额外的文件信息（如果需要）
                if (i == pathParts.length - 1 && currentNode.getType() == 0) {
                    // 这里可以设置文件的额外属性，例如：
                    // currentNode.setFileSize(file.getFileSize());
                    // currentNode.setFileType(file.getFileType());
                    currentNode.setFileId(file.getFileId());
                    currentNode.setDel(file.getIsDeleted() == 1);
                    currentNode.setSize(file.getFileSize());
                    currentNode.setUpdateTime(file.getUpdatedTime());

                }
            }
        }

        // 对根节点按标签进行排序（可选）
        rootNodes.sort(Comparator.comparing(FileTreeDto::getLabel));

        return rootNodes;
    }


}
