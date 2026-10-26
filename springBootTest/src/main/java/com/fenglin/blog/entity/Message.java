package com.fenglin.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("blog_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer readFlag;
    private Integer senderDeleted;
    private Integer receiverDeleted;
    /** 被引用的消息 id（同一会话内），可为空 */
    private Long quoteId;
    /** 是否已撤回：撤回对双方生效，双方都会看到「已撤回」提示 */
    private Integer recalled;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getReadFlag() { return readFlag; }
    public void setReadFlag(Integer readFlag) { this.readFlag = readFlag; }
    public Integer getSenderDeleted() { return senderDeleted; }
    public void setSenderDeleted(Integer senderDeleted) { this.senderDeleted = senderDeleted; }
    public Integer getReceiverDeleted() { return receiverDeleted; }
    public void setReceiverDeleted(Integer receiverDeleted) { this.receiverDeleted = receiverDeleted; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public Integer getRecalled() { return recalled; }
    public void setRecalled(Integer recalled) { this.recalled = recalled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
