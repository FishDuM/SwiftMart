package hk.ljx.swiftmart.goods.service;

import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListReqVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListRspVO;

import java.util.List;

public interface GoodsService {

    /**
     * 查询秒杀商品列表
     *
     * @param reqVO
     * @return
     */
    Response<List<FindSeckillGoodsListRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO);
}
