package com.mandiritransport.ops.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.trip.entity.Trip;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TripMapper extends BaseMapper<Trip> {
}
