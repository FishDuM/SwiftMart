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
    /**
     * 扣减秒杀库存
     *
     * @param id 秒杀商品关联表主键 ID
     * @return 影响行数
     */
    int deductStock(@io.lettuce.core.dynamic.annotation.Param("id") Long id);
}