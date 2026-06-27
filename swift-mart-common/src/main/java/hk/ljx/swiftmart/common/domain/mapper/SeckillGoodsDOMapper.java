package hk.ljx.swiftmart.common.domain.mapper;

import hk.ljx.swiftmart.common.domain.dataobject.GoodsDO;
import hk.ljx.swiftmart.common.domain.dataobject.SeckillGoodsDO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeckillGoodsDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SeckillGoodsDO record);

    int insertSelective(SeckillGoodsDO record);

    SeckillGoodsDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SeckillGoodsDO record);

    int updateByPrimaryKey(SeckillGoodsDO record);

    List<SeckillGoodsDO> selectByActivityId(@Param("activityId") Long activityId);

    /**
     * 根据活动 ID 和商品 ID 查询秒杀商品
     *
     * @param activityId
     * @param goodsId
     * @return
     */
    SeckillGoodsDO selectByActivityIdAndGoodsId(@Param("activityId") Long activityId,
                                                @Param("goodsId") Long goodsId);
}