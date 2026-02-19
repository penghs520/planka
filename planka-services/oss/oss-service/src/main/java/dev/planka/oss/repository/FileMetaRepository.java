package dev.planka.oss.repository;

import dev.planka.oss.entity.FileMeta;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件元数据 Repository
 */
@Mapper
public interface FileMetaRepository extends BaseMapper<FileMeta> {
}
