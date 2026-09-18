// 剧情脚本：章节 -> 行。行可以是旁白、对话或分支选择。
// speaker: 'narration' | 'naruto' | 'ichigo' | 'soul-king'
// choice 行会中断等待玩家选择，jump 指向对应分支章节。

export const CHAPTERS = [
  {
    id: 'ch1',
    title: '第一章 · 灵压与查克拉',
    lines: [
      { speaker: 'narration', text: '黄昏，天空裂开了一道缝。两个世界的边界，开始融化。' },
      { speaker: 'narration', text: '尸魂界察觉到了陌生的能量——不属于灵压，却同样狂暴。' },
      { speaker: 'naruto', text: '喂！那边那个穿黑衣服的，你就是把村子搞得天翻地覆的家伙？' },
      { speaker: 'ichigo', text: '……看你说话的样子，不像虚，也不是死神。你到底是什么？' },
      { speaker: 'naruto', text: '我是漩涡鸣人，木叶的忍者！未来的火影！' },
      { speaker: 'ichigo', text: '黑崎一护。代理死神。——先说好，我不想跟你打。' },
      {
        speaker: 'choice',
        prompt: '鸣人伸出了拳头。',
        options: [
          { label: '碰拳——用忍者的方式打招呼', goto: 'ch2-ninja' },
          { label: '拔刀——用死神的方式试探他', goto: 'ch2-shinigami' }
        ]
      }
    ]
  },
  {
    id: 'ch2-ninja',
    title: '第二章 · 忍者之路',
    lines: [
      { speaker: 'narration', text: '拳头停在半空，一护愣了一下，还是碰了上去。' },
      { speaker: 'ichigo', text: '……有意思。好，我信你一次。' },
      { speaker: 'naruto', text: '嘿嘿！那咱们就联手吧！不管那个裂缝是什么，一起把它堵上！' },
      { speaker: 'narration', text: '两股力量并肩而行。黄色闪光与黑色月牙，第一次朝向同一个敌人。' },
      { speaker: 'narration', text: '——就这样，两界的故事翻开了第一页。【结局：羁绊初成】' }
    ]
  },
  {
    id: 'ch2-shinigami',
    title: '第二章 · 死神之刃',
    lines: [
      { speaker: 'narration', text: '斩魄刀出鞘的一瞬，鸣人的眼睛亮了——不是愤怒，是兴奋。' },
      { speaker: 'naruto', text: '好！那就先打过再说！螺旋丸！！' },
      { speaker: 'ichigo', text: '月牙天冲！' },
      { speaker: 'narration', text: '蓝色查克拉与黑色灵压在半空相撞，裂缝被这一击震得剧烈颤抖。' },
      { speaker: 'ichigo', text: '……够了。再打下去，世界会先于我们毁灭。' },
      { speaker: 'naruto', text: '呼……呼……说得对。那，联手？' },
      { speaker: 'narration', text: '刀与拳，在废墟上握在了一起。【结局：不打不相识】' }
    ]
  }
]

const ichigoImg = new URL('../assets/ichigo_cut.png', import.meta.url).href
const narutoImg = new URL('../assets/naruto_cut.png', import.meta.url).href

export const CG_LIST = [
  { id: 'cg-ichigo', img: ichigoImg, title: '代理死神 · 黑崎一护', desc: '手持斩魄刀的黑衣死神，立于裂缝之前。' },
  { id: 'cg-naruto', img: narutoImg, title: '九尾人柱力 · 漩涡鸣人', desc: '掌中凝聚螺旋的木叶忍者，目光灼灼。' }
]

export const ENDINGS = [
  { id: 'ending-1', title: '羁绊初成', chapter: 'ch2-ninja', desc: '以忍者的方式相遇，以同伴的方式同行。' },
  { id: 'ending-2', title: '不打不相识', chapter: 'ch2-shinigami', desc: '刀光与螺旋碰撞之后，理解由此而生。' }
]

export function findChapter(id) {
  return CHAPTERS.find((c) => c.id === id)
}
