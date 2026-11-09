package com.mandiritransport.ops.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mandiritransport.ops.customer.entity.CustomerAddress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerAddressMapper extends BaseMapper<CustomerAddress> {
}
