/**
 * 后端 VO 里的作者信息是扁平字段（PostVO: authorId/authorName/authorAvatar/authorFaction，
 * CommentVO: userId/userName/userAvatar/userFaction），而组件（UserAvatar、评论列表）按
 * `item.author` 对象取值。这里做一次归一，避免每个页面各写一遍。
 *
 * 归一后形如：{ id, nickname, username, avatar, faction }，缺字段时尽量不丢失原值。
 */

function build(id, nickname, avatar, faction, username) {
  return {
    id: id ?? null,
    nickname: nickname || '',
    username: username || '',
    avatar: avatar || '',
    faction: faction || 'ninja'
  }
}

/** 文章：authorId / authorName / authorAvatar / authorFaction */
export function withPostAuthor(post) {
  if (!post) return post
  if (post.author) return post
  if (post.authorId == null && !post.authorName) return post
  return {
    ...post,
    author: build(post.authorId, post.authorName, post.authorAvatar, post.authorFaction)
  }
}

/** 评论：userId / userName / userAvatar / userFaction */
export function withCommentAuthor(comment) {
  if (!comment) return comment
  if (comment.author) return comment
  if (comment.userId == null && !comment.userName) return comment
  return {
    ...comment,
    author: build(comment.userId, comment.userName, comment.userAvatar, comment.userFaction)
  }
}

export function withPostAuthors(list) {
  return (list || []).map(withPostAuthor)
}

export function withCommentAuthors(list) {
  return (list || []).map(withCommentAuthor)
}

/** 当前登录用户：每次都从本地重新读，避免组件 setup 时缓存到过期快照 */
export function readMe() {
  try {
    return JSON.parse(localStorage.getItem('bn-user') || 'null')
  } catch {
    return null
  }
}

/** 登录态：有用户信息或有访问令牌都算已登录 */
export function isLogged() {
  if (readMe()) return true
  try {
    return !!localStorage.getItem('bn-token')
  } catch {
    return false
  }
}
