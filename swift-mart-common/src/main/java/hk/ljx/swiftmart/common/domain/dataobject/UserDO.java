package hk.ljx.swiftmart.common.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDO {
    private Long id;

    private String nickname;

    private String password;

    private String mobile;

    private String avatar;

    private Integer status;

    private Date createTime;

    private Date updateTime;
}