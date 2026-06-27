package hk.ljx.swiftmart.common.domain.mapper;

import hk.ljx.swiftmart.common.domain.dataobject.SeckillOrderDO;
import io.lettuce.core.dynamic.annotation.Param;

public interface SeckillOrderDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SeckillOrderDO record);

    int insertSelective(SeckillOrderDO record);

    SeckillOrderDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SeckillOrderDO record);

    int updateByPrimaryKey(SeckillOrderDO record);

}