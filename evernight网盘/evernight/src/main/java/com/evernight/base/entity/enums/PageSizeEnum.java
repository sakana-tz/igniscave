package com.evernight.base.entity.enums;


/**
 * 页面大小枚举类
 * 定义系统中可用的分页大小选项
 */
public enum PageSizeEnum {
    SIZE_15(15, "15条/页"),
    SIZE_20(20, "20条/页"),
    SIZE_30(30, "30条/页"),
    SIZE_40(40, "40条/页"),
    SIZE_50(50, "50条/页");

    private final int size;
    private final String description;

    /**
     * 构造函数
     *
     * @param size        分页大小
     * @param description 描述信息
     */
    PageSizeEnum(int size, String description) {
        this.size = size;
        this.description = description;
    }

    /**
     * 获取分页大小
     *
     * @return 分页大小数值
     */
    public int getSize() {
        return this.size;
    }

    /**
     * 获取描述信息
     *
     * @return 描述信息
     */
    public String getDescription() {
        return this.description;
    }

}
