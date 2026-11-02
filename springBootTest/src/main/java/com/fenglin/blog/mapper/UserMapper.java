package com.fenglin.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.blog.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    /** 经验值自增：并发下单条 SQL 更新，避免「先查后写」丢增量 */
    @Update("UPDATE blog_user SET exp = exp + #{delta} WHERE id = #{id}")
    int addExp(@Param("id") Long id, @Param("delta") int delta);
}
