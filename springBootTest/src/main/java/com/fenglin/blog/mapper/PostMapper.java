package com.fenglin.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.blog.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE blog_post SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
