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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 私信：会话列表、聊天记录、发送 / 删除 / 撤回与已读标记 */
@Service
public class MessageService {

    /** 会话列表最多回看的消息条数，防止一次性拉取全表 */
    private static final long SCAN_LIMIT = 500L;

    /** 撤回后双方看到的提示文案 */
    public static final String RECALLED_TEXT = "消息已撤回";

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
                    .and(w -> w.eq(Message::getSenderId, myId)
                            .eq(Message::getReceiverId, conversation.peerId())
                            .eq(Message::getSenderDeleted, 0))
                    .or(w -> w.eq(Message::getSenderId, conversation.peerId())
                            .eq(Message::getReceiverId, myId)
                            .eq(Message::getReceiverDeleted, 0))
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
                    latest == null ? null : preview(latest),
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
        // 单方删除只对自己生效：自己发出去的看 sender_deleted，收到的看 receiver_deleted
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getSenderId, myId)
                        .eq(Message::getReceiverId, peerId)
                        .eq(Message::getSenderDeleted, 0))
                .or(w -> w.eq(Message::getSenderId, peerId)
                        .eq(Message::getReceiverId, myId)
                        .eq(Message::getReceiverDeleted, 0))
                .orderByAsc(Message::getCreatedAt)
                .orderByAsc(Message::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500))));

        Message update = new Message();
        update.setReadFlag(1);
        messageMapper.update(update, new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, peerId)
                .eq(Message::getReceiverId, myId)
                .eq(Message::getReadFlag, 0));

        return toVOs(messages);
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

        // 引用必须落在「我和对方」这条会话里，避免通过 quoteId 窥探他人私信
        if (request.quoteId() != null) {
            Message quoted = messageMapper.selectById(request.quoteId());
            if (quoted == null) {
                throw BizException.badRequest("被引用的消息不存在");
            }
            boolean inConversation =
                    (quoted.getSenderId().equals(myId) && quoted.getReceiverId().equals(peerId))
                            || (quoted.getSenderId().equals(peerId) && quoted.getReceiverId().equals(myId));
            if (!inConversation) {
                throw BizException.badRequest("只能引用当前会话中的消息");
            }
        }

        Message message = new Message();
        message.setSenderId(myId);
        message.setReceiverId(peerId);
        message.setContent(request.content().trim());
        message.setReadFlag(0);
        message.setSenderDeleted(0);
        message.setReceiverDeleted(0);
        message.setQuoteId(request.quoteId());
        message.setRecalled(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return toVOs(List.of(message)).get(0);
    }

    /**
     * 删除一条私信：只对「我」这一侧生效，对方会话里这条消息照常可见。
     * 用 sender_deleted / receiver_deleted 标记，不清库。
     */
    @Transactional
    public void deleteForMe(Long messageId) {
        Long myId = CurrentUserHolder.require().userId();
        Message message = requireParticipant(messageId, myId);

        Message update = new Message();
        update.setId(message.getId());
        if (myId.equals(message.getSenderId())) {
            update.setSenderDeleted(1);
        } else {
            update.setReceiverDeleted(1);
        }
        messageMapper.updateById(update);
    }

    /**
     * 撤回一条私信：只有发送者能撤，且对双方同时生效——
     * 双方都会看到「消息已撤回」。原文从库中清空，不再下发。
     */
    @Transactional
    public MessageVO recall(Long messageId) {
        Long myId = CurrentUserHolder.require().userId();
        Message message = requireParticipant(messageId, myId);
        if (!myId.equals(message.getSenderId())) {
            throw BizException.forbidden("只能撤回自己发送的消息");
        }
        if (isRecalled(message)) {
            return toVOs(List.of(message)).get(0);
        }

        Message update = new Message();
        update.setId(message.getId());
        update.setRecalled(1);
        update.setContent("");
        messageMapper.updateById(update);

        message.setRecalled(1);
        message.setContent("");
        return toVOs(List.of(message)).get(0);
    }

    /** 取出一条消息，并确认当前用户确实是这条会话的参与者 */
    private Message requireParticipant(Long messageId, Long myId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw BizException.notFound("消息不存在");
        }
        boolean involved = myId.equals(message.getSenderId()) || myId.equals(message.getReceiverId());
        if (!involved) {
            // 别人的私信对当前用户就是不存在，不泄露任何信息
            throw BizException.notFound("消息不存在");
        }
        return message;
    }

    private boolean isRecalled(Message message) {
        return message.getRecalled() != null && message.getRecalled() == 1;
    }

    /** 会话列表里显示的最后一条：撤回的消息显示为提示文案 */
    private String preview(Message message) {
        return isRecalled(message) ? RECALLED_TEXT : message.getContent();
    }

    private ConversationVO findConversation(List<ConversationVO> conversations, Long peerId) {
        for (ConversationVO conversation : conversations) {
            if (conversation.peerId().equals(peerId)) {
                return conversation;
            }
        }
        return null;
    }

    /**
     * 批量转换。引用信息（quoteId 指向的原消息）在这里一次性查回并回填，
     * 避免在循环里对每条引用各查一次导致 N+1。
     */
    private List<MessageVO> toVOs(List<Message> messages) {
        // 先把本次涉及到的被引用消息收集起来
        Map<Long, Message> quoted = new HashMap<>();
        for (Message message : messages) {
            if (message.getQuoteId() != null) {
                quoted.computeIfAbsent(message.getQuoteId(), messageMapper::selectById);
            }
        }
        // 被引用的消息可能已被清理，computeIfAbsent 会存入 null，这里剔除
        quoted.values().removeIf(Objects::isNull);

        Map<Long, String> senderNames = new HashMap<>();
        for (Message quote : quoted.values()) {
            senderNames.computeIfAbsent(quote.getSenderId(), id -> {
                User user = userMapper.selectById(id);
                return user == null ? null : user.getNickname();
            });
        }

        List<MessageVO> list = new ArrayList<>(messages.size());
        for (Message message : messages) {
            Message quote = message.getQuoteId() == null ? null : quoted.get(message.getQuoteId());
            list.add(new MessageVO(
                    message.getId(),
                    message.getSenderId(),
                    message.getReceiverId(),
                    message.getContent(),
                    message.getReadFlag(),
                    message.getCreatedAt(),
                    quote == null ? null : quote.getId(),
                    quote == null ? null : senderNames.get(quote.getSenderId()),
                    quote == null ? null
                            : (isRecalled(quote) ? RECALLED_TEXT : summarize(quote.getContent())),
                    isRecalled(message) ? 1 : 0));
        }
        return list;
    }

    /** 引用区只显示摘要，长消息截断，避免一条引用把气泡撑爆 */
    private String summarize(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > 100 ? content.substring(0, 100) + "…" : content;
    }
}
