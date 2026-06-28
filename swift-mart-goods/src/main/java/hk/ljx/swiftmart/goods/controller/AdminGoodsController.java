package hk.ljx.swiftmart.goods.controller;

import hk.ljx.swiftmart.common.aspect.ApiOperationLog;
import hk.ljx.swiftmart.common.utils.Response;
import hk.ljx.swiftmart.goods.model.vo.PreheatActivityCacheReqVO;
import hk.ljx.swiftmart.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seckill/goods")
@Slf4j
public class AdminGoodsController {

    @Resource
    private GoodsService goodsService;

    @PostMapping("/cache/preheat")
    @ApiOperationLog(description = "手动预热商品缓存")
    public Response<?> preheatCache(@RequestBody @Validated PreheatActivityCacheReqVO reqVO) {
        return goodsService.preheatActivityGoods(reqVO.getActivityId());
    }

}