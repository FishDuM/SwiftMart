package hk.ljx.swiftmart.common.domain.mapper;

import hk.ljx.swiftmart.common.domain.dataobject.GoodsDO;

import java.util.List;

public interface GoodsDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsDO record);

    int insertSelective(GoodsDO record);

    GoodsDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsDO record);

    int updateByPrimaryKey(GoodsDO record);

    List<GoodsDO> selectByIds(List<Long> ids);
}