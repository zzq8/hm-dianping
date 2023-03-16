package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Cacheable(value = RedisConstants.CACHE_SHOP_KEY, key = "#root.methodName", sync = true)
    @Override
    public List<ShopType> queryTypeList() {
        log.info("没有命中缓存，开始查询~~~~");
        List<ShopType> list = baseMapper.selectList(new QueryWrapper<ShopType>().orderByAsc("sort"));
        return list;
    }
}
