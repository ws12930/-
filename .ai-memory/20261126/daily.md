## [13:58] - [功能实现]: 社区广场新增「不感兴趣」（帖子+吧，可撤销）与「解散该吧」

- **文件**:
  - 新增 `springBootTest/src/main/resources/db/schema_community.sql`
  - 新增 `entity/PostDislike.java`、`entity/BaDislike.java`、对应 Mapper
  - 新增 `service/DislikeService.java`、`controller/DislikeController.java`
  - 修改 `PostService.java`（listPublished/关注流过滤 + categoryStats 过滤）、`TaxonomyService.java` + `TaxonomyController.java`（解散吧）
  - 修改 `7h/src/api/community.js`、`7h/src/views/CommunityView.vue`
  - 修改 `start.py`（增量脚本白名单）、`springBootTest/fenglin_blog.sql`
- **决策**: 不感兴趣按用户维度记录，只影响自己信息流；解散吧采用软下架 status=0 保留帖子；解散后释放创建名额；迁移脚本用显式白名单避免误跑 rollback.sql。
- **验证**: vite build ✓；Maven 3.9.6 离线 compile ✓（MVN_EXIT=0）；py_compile ✓。
