package com.mandiritransport.ops.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.auth.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
