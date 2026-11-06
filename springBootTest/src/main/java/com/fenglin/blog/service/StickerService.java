package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.CommunityModels.StickerRequest;
import com.fenglin.blog.dto.CommunityModels.StickerVO;
import com.fenglin.blog.entity.Sticker;
import com.fenglin.blog.mapper.StickerMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义表情：跟账号走，换设备 / 清缓存都不会丢（前端本地缓存只作镜像）。
 */
@Service
public class StickerService {

    /** 与前端 utils/stickers.js 的 MAX_COUNT 保持一致 */
    private static final int MAX_COUNT = 30;

    private final StickerMapper stickerMapper;

    public StickerService(StickerMapper stickerMapper) {
        this.stickerMapper = stickerMapper;
    }

    public List<StickerVO> list(long userId) {
        return stickerMapper.selectList(new LambdaQueryWrapper<Sticker>()
                        .eq(Sticker::getUserId, userId)
                        .orderByAsc(Sticker::getId))
                .stream().map(this::toVO).toList();
    }

    /** 新增一个表情，返回最新列表 */
    @Transactional
    public List<StickerVO> add(long userId, String name, String url) {
        String cleaned = url == null ? "" : url.trim();
        if (cleaned.isEmpty()) {
            throw BizException.badRequest("缺少表情图片地址");
        }
        if (stickerMapper.selectCount(new LambdaQueryWrapper<Sticker>()
                .eq(Sticker::getUserId, userId)) >= MAX_COUNT) {
            throw BizException.badRequest("最多只能存 " + MAX_COUNT + " 个表情");
        }
        insert(userId, name, cleaned);
        return list(userId);
    }

    /** 批量导入（首次登录时把本机缓存的表情搬到账号上），已存在的自动跳过 */
    @Transactional
    public List<StickerVO> importAll(long userId, List<StickerRequest> stickers) {
        if (stickers == null || stickers.isEmpty()) {
            return list(userId);
        }
        long count = stickerMapper.selectCount(new LambdaQueryWrapper<Sticker>()
                .eq(Sticker::getUserId, userId));
        for (StickerRequest item : stickers) {
            if (count >= MAX_COUNT) {
                break;
            }
            String url = item.url() == null ? "" : item.url().trim();
            if (url.isEmpty()) {
                continue;
            }
            boolean exists = stickerMapper.selectCount(new LambdaQueryWrapper<Sticker>()
                    .eq(Sticker::getUserId, userId)
                    .eq(Sticker::getUrl, url)) > 0;
            if (exists) {
                continue;
            }
            insert(userId, item.name(), url);
            count++;
        }
        return list(userId);
    }

    /** 删除自己的一个表情，返回最新列表 */
    @Transactional
    public List<StickerVO> delete(long userId, long stickerId) {
        Sticker sticker = stickerMapper.selectById(stickerId);
        if (sticker == null || !Long.valueOf(userId).equals(sticker.getUserId())) {
            throw BizException.notFound("表情不存在或已删除");
        }
        stickerMapper.deleteById(stickerId);
        return list(userId);
    }

    private void insert(long userId, String name, String url) {
        Sticker sticker = new Sticker();
        sticker.setUserId(userId);
        sticker.setName(name == null || name.isBlank() ? "表情" : name.trim());
        sticker.setUrl(url);
        sticker.setCreatedAt(LocalDateTime.now());
        try {
            stickerMapper.insert(sticker);
        } catch (DuplicateKeyException e) {
            throw BizException.badRequest("这个表情已经添加过了");
        }
    }

    private StickerVO toVO(Sticker sticker) {
        return new StickerVO(sticker.getId(), sticker.getName(), sticker.getUrl());
    }
}
