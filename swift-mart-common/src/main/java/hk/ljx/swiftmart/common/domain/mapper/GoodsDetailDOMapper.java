package hk.ljx.swiftmart.common.domain.mapper;

import hk.ljx.swiftmart.common.domain.dataobject.GoodsDetailDO;

public interface GoodsDetailDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsDetailDO record);

    int insertSelective(GoodsDetailDO record);

    GoodsDetailDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsDetailDO record);

    int updateByPrimaryKeyWithBLOBs(GoodsDetailDO record);

    int updateByPrimaryKey(GoodsDetailDO record);

    /**
     * 根据商品 ID 查询商品详情
     */
    GoodsDetailDO selectByGoodsId(Long goodsId);

}