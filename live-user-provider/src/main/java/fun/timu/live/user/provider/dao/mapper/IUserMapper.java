package fun.timu.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.timu.live.user.provider.dao.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserMapper extends BaseMapper<UserPO> {
}
