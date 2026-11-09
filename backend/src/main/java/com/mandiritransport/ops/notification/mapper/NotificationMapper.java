package com.mandiritransport.ops.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
