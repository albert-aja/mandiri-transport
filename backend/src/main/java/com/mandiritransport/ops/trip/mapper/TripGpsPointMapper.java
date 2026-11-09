package com.mandiritransport.ops.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.trip.entity.TripGpsPoint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TripGpsPointMapper extends BaseMapper<TripGpsPoint> {
}
