package com.mandiritransport.ops.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.sync.entity.SyncConflictRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncConflictRecordMapper extends BaseMapper<SyncConflictRecord> {
}
