package com.mandiritransport.ops.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.trip.entity.TripStop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TripStopMapper extends BaseMapper<TripStop> {
}
