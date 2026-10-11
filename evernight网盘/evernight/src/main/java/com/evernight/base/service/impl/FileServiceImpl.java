package com.evernight.base.service.impl;

import com.evernight.base.entity.enums.PageSizeEnum;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.po.File;
import com.evernight.base.entity.query.FileQuery;
import com.evernight.base.entity.query.SimplePage;
import com.evernight.base.entity.vo.PaginationResultVO;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.mapper.FileMapper;
import com.evernight.base.service.FileService;
import com.evernight.base.tools.StringTools;
import com.evernight.entity.dto.UserSessionDto;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 文件表 业务接口实现
 */
@Service("fileService")
public class FileServiceImpl implements FileService {

    @Resource
    private FileMapper<File, FileQuery> fileMapper;

    /**
     * 查询存储总文件大小
     */
    @Override
    public long getStorageTotalSize(UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        long totalSize = this.fileMapper.selectStorageTotalSize();
        System.out.println("##############totalSize:" + totalSize);
        return totalSize;

    }

    /**
     * 查询文件总数
     */
    @Override
    public long getFileTotalCount(UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return this.fileMapper.selectFileTotalCount();
    }


    /**
     * 根据条件查询列表
     */
    @Override
    public List<File> findListByParam(FileQuery param) {
        return this.fileMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(FileQuery param) {
        return this.fileMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<File> findListByPage(FileQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSizeEnum.SIZE_15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<File> list = this.findListByParam(param);
        PaginationResultVO<File> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(File bean) {
        return this.fileMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<File> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<File> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(File bean, FileQuery param) {
        StringTools.checkParam(param);
        return this.fileMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(FileQuery param) {
        StringTools.checkParam(param);
        return this.fileMapper.deleteByParam(param);
    }

    /**
     * 根据FileId获取对象
     */
    @Override
    public File getFileByFileId(Long fileId) {
        return this.fileMapper.selectByFileId(fileId);
    }

    /**
     * 根据FileId修改
     */
    @Override
    public Integer updateFileByFileId(File bean, Long fileId) {
        return this.fileMapper.updateByFileId(bean, fileId);
    }

    /**
     * 根据FileId删除
     */
    @Override
    public Integer deleteFileByFileId(Long fileId) {
        return this.fileMapper.deleteByFileId(fileId);
    }

}