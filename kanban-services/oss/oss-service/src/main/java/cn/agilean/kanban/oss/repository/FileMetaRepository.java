package cn.agilean.kanban.oss.repository;

import cn.agilean.kanban.oss.entity.FileMeta;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件元数据 Repository
 */
@Mapper
public interface FileMetaRepository extends BaseMapper<FileMeta> {
}
