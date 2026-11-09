package com.mandiritransport.ops.intelligence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.intelligence.entity.AnomalyRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnomalyRuleMapper extends BaseMapper<AnomalyRule> {
}
