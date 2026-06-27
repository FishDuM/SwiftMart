package hk.ljx.swiftmart.goods.controller;

import hk.ljx.swiftmart.common.aspect.ApiOperationLog;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListReqVO;
import hk.ljx.swiftmart.goods.model.vo.FindSeckillGoodsListRspVO;
import hk.ljx.swiftmart.goods.service.GoodsService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seckill/goods")
public class GoodsController {
    @Resource
    private GoodsService goodsService;

    @PostMapping("/list")
    @ApiOperationLog(description = "商品秒杀")
    public Response<List<FindSeckillGoodsListRspVO>>  getSeckillGoodsList(@RequestBody @Validated FindSeckillGoodsListReqVO reqVO) {
        return goodsService.findSeckillGoodsList(reqVO);
    }

}
