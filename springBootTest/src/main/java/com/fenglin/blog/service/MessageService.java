package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.SocialModels.ConversationVO;
import com.fenglin.blog.dto.SocialModels.MessageSendRequest;
import com.fenglin.blog.dto.SocialModels.MessageVO;
import com.fenglin.blog.entity.Message;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.MessageMapper;
import com.fenglin.blog.mapper.UserMapper;
import com.fenglin.blog.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 私信：会话列表、聊天记录、发送与已读标记 */
@Service
public class MessageService {

    /** 会话列表最多回看的消息条数，防止一次性拉取全表 */
    private static final long SCAN_LIMIT = 500L;

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    public MessageService(MessageMapper messageMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    /** 会话列表：按最近一条消息倒序 */
    public List<ConversationVO> conversations() {
        Long myId = CurrentUserHolder.require().userId();
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .and(wrapper -> wrapper.eq(Message::getSenderId, myId).eq(Message::getSenderDeleted, 0))
                .or(wrapper -> wrapper.eq(Message::getReceiverId, myId).eq(Message::getReceiverDeleted, 0))
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT " + SCAN_LIMIT));

        List<ConversationVO> conversations = new ArrayList<>();
        for (Message message : messages) {
            Long peerId = myId.equals(message.getSenderId())
                    ? message.getReceiverId() : message.getSenderId();
            ConversationVO existing = findConversation(conversations, peerId);
            if (existing == null) {
                conversations.add(new ConversationVO(
                        peerId, null, null, null,
                        message.getContent(), message.getCreatedAt(), 0L));
            }
        }

        // 每个会话补充对方资料、最近一条与未读数
        List<ConversationVO> result = new ArrayList<>(conversations.size());
        for (ConversationVO conversation : conversations) {
            User peer = userMapper.selectById(conversation.peerId());
            if (peer == null) {
                continue;
            }
            Message latest = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                    .and(w -> w.eq(Message::getSenderId, myId).eq(Message::getReceiverId, conversation.peerId()))
                    .or(w -> w.eq(Message::getSenderId, conversation.peerId()).eq(Message::getReceiverId, myId))
                    .orderByDesc(Message::getCreatedAt)
                    .last("LIMIT 1"));
            long unread = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getSenderId, conversation.peerId())
                    .eq(Message::getReceiverId, myId)
                    .eq(Message::getReadFlag, 0));
            result.add(new ConversationVO(
                    peer.getId(),
                    peer.getNickname(),
                    peer.getAvatar(),
                    peer.getFaction(),
                    latest == null ? null : latest.getContent(),
                    latest == null ? null : latest.getCreatedAt(),
                    unread));
        }
        result.sort(Comparator.comparing(ConversationVO::lastAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public long unreadCount() {
        Long myId = CurrentUserHolder.require().userId();
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, myId)
                .eq(Message::getReadFlag, 0));
    }

    /** 与某人的聊天记录（正序），并把对方发给我的消息标为已读 */
    @Transactional
    public List<MessageVO> history(Long peerId, long limit) {
        Long myId = CurrentUserHolder.require().userId();
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getSenderId, myId).eq(Message::getReceiverId, peerId))
                .or(w -> w.eq(Message::getSenderId, peerId).eq(Message::getReceiverId, myId))
                .orderByAsc(Message::getCreatedAt)
                .orderByAsc(Message::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500))));

        Message update = new Message();
        update.setReadFlag(1);
        messageMapper.update(update, new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, peerId)
                .eq(Message::getReceiverId, myId)
                .eq(Message::getReadFlag, 0));

        List<MessageVO> list = new ArrayList<>(messages.size());
        for (Message message : messages) {
            list.add(toVO(message));
        }
        return list;
    }

    @Transactional
    public MessageVO send(Long peerId, MessageSendRequest request) {
        Long myId = CurrentUserHolder.require().userId();
        if (myId.equals(peerId)) {
            throw BizException.badRequest("不能给自己发消息");
        }
        User peer = userMapper.selectById(peerId);
        if (peer == null) {
            throw BizException.notFound("收件人不存在");
        }

        Message message = new Message();
        message.setSenderId(myId);
        message.setReceiverId(peerId);
        message.setContent(request.content().trim());
        message.setReadFlag(0);
        message.setSenderDeleted(0);
        message.setReceiverDeleted(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return toVO(message);
    }

    private ConversationVO findConversation(List<ConversationVO> conversations, Long peerId) {
        for (ConversationVO conversation : conversations) {
            if (conversation.peerId().equals(peerId)) {
                return conversation;
            }
        }
        return null;
    }

    private MessageVO toVO(Message message) {
        return new MessageVO(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                message.getReadFlag(),
                message.getCreatedAt());
    }
}
