<template>
  <!-- 悬浮智能助手：Q版胖嘟嘟小人（可拖动） -->
  <div class="mascot" :class="{ open: chatOpen }">
    <!-- 对话窗口 -->
    <transition name="mascot-pop">
      <div v-if="chatOpen" class="chat-window">
        <div class="chat-header">
          <div class="chat-title">
            <span class="chat-avatar">🤗</span>
            <div>
              <div class="chat-name">胆子肥嘟嘟</div>
              <div class="chat-status">●DeepSeek 在线 · 你的食堂小管家</div>
            </div>
          </div>
          <div class="chat-tools">
            <button class="chat-tool" title="清空聊天记录" @click="clearHistory">🗑</button>
            <button class="chat-tool" @click="chatOpen = false">✕</button>
          </div>
        </div>

        <div ref="msgList" class="chat-body">
          <div class="hist-tip">上方是本次的聊天历史（已自动保存），点 🗑 可清空</div>
          <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
            <div class="bubble">{{ m.text }}</div>
          </div>
          <div v-if="typing" class="msg bot">
            <div class="bubble typing">肥嘟嘟正在敲键盘<span class="dot">·</span><span class="dot">·</span><span class="dot">·</span></div>
          </div>
        </div>

        <div class="chat-quick">
          <button v-for="q in quickQuestions" :key="q" @click="ask(q)">{{ q }}</button>
        </div>

        <form class="chat-input" @submit.prevent="send">
          <el-input v-model="draft" placeholder="问我吃什么…" size="small" clearable />
          <el-button type="primary" size="small" native-type="submit" round>发送</el-button>
        </form>
      </div>
    </transition>

    <!-- 打招呼/待机搭话气泡 -->
    <transition name="mascot-pop">
      <div v-if="!chatOpen && showGreet" class="greet-bubble" @click="open()">
        <span class="greet-text">{{ greeting }}</span><span class="greet-tail"></span>
      </div>
    </transition>

    <!-- 小人本体（SVG 手绘，可拖动） -->
    <button
      class="doll" :class="{ hop: hop }"
      :style="dollStyle"
      @pointerdown="dragStart"
      @pointermove="dragMove"
      @pointerup="dragEnd"
      @pointercancel="dragEnd"
    >
      <svg viewBox="0 0 120 130" width="64" height="70" aria-label="胆子肥嘟嘟">
        <!-- 弹跳影子 -->
        <ellipse class="shadow" cx="60" cy="122" rx="26" ry="5" fill="rgba(110,110,95,0.25)" />
        <g class="doll-group">
          <!-- 呆毛 -->
          <path d="M56 12 Q52 4 58 2 Q60 6 62 10 Z" fill="#a4b08a" />
          <!-- 小耳朵 -->
          <ellipse cx="26" cy="38" rx="9" ry="11" fill="#fff6e8" stroke="#e8dcc4" stroke-width="2" />
          <ellipse cx="94" cy="38" rx="9" ry="11" fill="#fff6e8" stroke="#e8dcc4" stroke-width="2" />
          <!-- 圆滚滚的身体（二头身） -->
          <path d="M60 14 C 88 14 104 32 104 62 C 104 96 86 114 60 114 C 34 114 16 96 16 62 C 16 32 32 14 60 14 Z"
                fill="#fff8ec" stroke="#ecdfc6" stroke-width="2.5" />
          <!-- 顶部的褶皱（包子造型） -->
          <path d="M44 20 Q50 14 60 18 Q70 14 76 20" fill="none" stroke="#e3d5ba" stroke-width="2.5" stroke-linecap="round" />
          <path d="M52 24 Q60 19 68 24" fill="none" stroke="#e9dcc3" stroke-width="2" stroke-linecap="round" />
          <!-- 眼睛（呆萌圆眼） -->
          <ellipse class="eye" cx="44" cy="58" rx="5.2" ry="6.4" fill="#574f42" />
          <ellipse class="eye" cx="76" cy="58" rx="5.2" ry="6.4" fill="#574f42" />
          <ellipse class="eye" cx="45.6" cy="55.4" rx="1.7" ry="2.1" fill="#fff" />
          <ellipse class="eye" cx="77.6" cy="55.4" rx="1.7" ry="2.1" fill="#fff" />
          <!-- 红晕 -->
          <ellipse cx="32" cy="70" rx="6.5" ry="4" fill="#f2b7a6" opacity="0.55" />
          <ellipse cx="88" cy="70" rx="6.5" ry="4" fill="#f2b7a6" opacity="0.55" />
          <!-- 开心的嘴（w形） -->
          <path class="mouth" d="M52 72 Q56 77 60 72 Q64 77 68 72" fill="none" stroke="#574f42" stroke-width="2.4" stroke-linecap="round" />
          <!-- 小肚皮 -->
          <ellipse cx="60" cy="94" rx="24" ry="16" fill="#fffdf7" stroke="#f0e6d2" stroke-width="2" />
          <!-- 肚皮上的饭团标志 -->
          <circle cx="60" cy="94" r="8.5" fill="#a4b08a" opacity="0.9" />
          <circle cx="60" cy="91" r="3" fill="#fff" opacity="0.85" />
          <path d="M53 97 h14" stroke="#fff" stroke-width="2" stroke-linecap="round" opacity="0.85" />
          <!-- 左手（按压波动的打招呼手） -->
          <g class="arm arm-left">
            <ellipse cx="17" cy="78" rx="8" ry="11" fill="#fff8ec" stroke="#ecdfc6" stroke-width="2.2" />
          </g>
          <!-- 右手 -->
          <ellipse cx="103" cy="78" rx="8" ry="11" fill="#fff8ec" stroke="#ecdfc6" stroke-width="2.2" />
          <!-- 小短腿 -->
          <ellipse cx="42" cy="116" rx="9" ry="6" fill="#fff6e8" stroke="#ecdfc6" stroke-width="2" />
          <ellipse cx="78" cy="116" rx="9" ry="6" fill="#fff6e8" stroke="#ecdfc6" stroke-width="2" />
        </g>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '../stores/user'
import { dishHot } from '../api/dish'
import { walletInfo } from '../api/wallet'

const chatOpen = ref(false)
const showGreet = ref(false)
const greeting = ref('充能完毕！我是胆子肥嘟嘟')
const typing = ref(false)
const draft = ref('')
const msgList = ref()
const hop = ref(false)
const userStore = useUserStore()

const quickQuestions = ['推荐点啥？', '余额还剩多少？', '订单在哪看？', '给我讲个笑话']

// ---------- 聊天历史：localStorage 持久化，刷新不丢 ----------
const HIST_KEY = 'dududu-chat'
const SAVE_KEY = 'dududu-seek-history'

const messages = ref([])

// DeepSeek 多轮上下文（持久化，刷新后仍记得聊过什么）
const seekHistory = ref([])

function persist() {
  try {
    localStorage.setItem(HIST_KEY, JSON.stringify(messages.value.slice(-40)))
    localStorage.setItem(SAVE_KEY, JSON.stringify(seekHistory.value.slice(-20)))
  } catch { /* 空间满忽略 */ }
}

function loadHistory() {
  try {
    const m = JSON.parse(localStorage.getItem(HIST_KEY) || 'null')
    if (Array.isArray(m) && m.length) messages.value = m
    const s = JSON.parse(localStorage.getItem(SAVE_KEY) || 'null')
    if (Array.isArray(s) && s.length) seekHistory.value = s
  } catch { /* 坏数据忽略 */ }
}

function clearHistory() {
  messages.value = [{ role: 'bot', text: '记录清空啦！我是胆子肥嘟嘟，重新认识一下～ 🍚' }]
  seekHistory.value = []
  persist()
}

// ---------- 时间段问候语 ----------
function timeGreet() {
  const h = new Date().getHours()
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username
  const who = name ? `${name}同学` : '你'
  if (h < 10) return `${who}早上好！记得吃早饭哦 🌅`
  if (h < 14) return `${who}中午吃啥想好了吗？我帮你参谋！🍚`
  if (h < 18) return `${who}下午茶时间～来点小吃犒劳自己？🧋`
  return `${who}晚上好！夜宵虽香，可不要贪嘴哦 🌙`
}

// ---------- 待机搭话：关着窗也会偶尔冒泡 ----------
const IDLE_WORDS = [
  '肚子咕咕叫的声音你听到了吗 🥺',
  '悄悄说：今天的麻辣香锅看起来很香！',
  '点餐卡住了？点我聊聊呀～',
  '听说你不是全校最会吃，我不信 😏',
  '余额和饭量一样，都是迷 🤫'
]
let idleTimer = null
function idleChatter() {
  if (chatOpen.value || document.hidden) return
  greeting.value = IDLE_WORDS[Math.floor(Math.random() * IDLE_WORDS.length)]
  showGreet.value = true
  hop.value = true
  setTimeout(() => { hop.value = false }, 600)
  setTimeout(() => { if (!chatOpen.value) showGreet.value = false }, 7000)
}

// ---------- 拖动小人 ----------
const DOLL_KEY = 'dududu-pos'
const pos = reactive(JSON.parse(localStorage.getItem(DOLL_KEY) || 'null') || { right: 26, bottom: 26 })
let dragState = null

const dollStyle = computed(() => ({ right: pos.right + 'px', bottom: pos.bottom + 'px' }))

function dragStart(e) {
  e.preventDefault()
  dragState = { x: e.clientX, y: e.clientY, right: pos.right, bottom: pos.bottom, moved: false }
  e.currentTarget.setPointerCapture?.(e.pointerId)
}
function dragMove(e) {
  if (!dragState) return
  const dx = dragState.x - e.clientX
  const dy = dragState.y - e.clientY
  if (Math.abs(dx) + Math.abs(dy) > 4) dragState.moved = true
  pos.right = Math.min(Math.max(4, dragState.right + dx), window.innerWidth - 90)
  pos.bottom = Math.min(Math.max(4, dragState.bottom + dy), window.innerHeight - 90)
}
function dragEnd() {
  if (!dragState) return
  const wasDrag = dragState.moved
  dragState = null
  if (wasDrag) {
    try { localStorage.setItem(DOLL_KEY, JSON.stringify({ ...pos })) } catch { }
    return
  }
  // 未拖动 = 单击 → 开窗
  open()
}

// ---------- DeepSeek 真实 AI 问答（失败时回退本地规则） ----------
const DS_KEY = import.meta.env.VITE_DEEPSEEK_API_KEY || ''

const SYSTEM_PROMPT = [
  '你是「胆子肥嘟嘟」，智慧校园食堂点餐系统的吉祥物兼小管家，一只圆滚滚的包子小人。',
  '人设：活泼可爱、爱吃爱玩、有点小胖但自信，说话带一点颜文字或 emoji，回复简短（不超过 3 句），口语化。',
  '你是食堂点餐助手：可以推荐菜品、聊饮食搭配、帮用户想点餐思路、回答关于食堂/点餐/钱包/订单/投诉的常规问题。',
  '系统里有川湘家常菜、兰州拉面、麻辣香锅三个档口，菜品如宫保鸡丁、红烧排骨、麻婆豆腐、牛肉拉面、羊肉泡馍、麻辣香锅、酸梅汤、冰粉等。',
  '涉及具体余额/订单状态等个人数据时，若下方提供了【实时数据】就直接引用；没有就提醒用户去「我的钱包」「我的订单」页面查看。',
  '对食堂管理系统的问题可以正常回答；与食堂、吃饭、学习生活无关的敏感话题请礼貌带回到吃饭话题。'
].join('\n')

// 抓真实数据注入上下文（余额/热销榜），失败静默
async function buildRealtimeContext() {
  const parts = []
  try {
    if (userStore.role === 'STUDENT') {
      const w = await walletInfo()
      if (w?.data?.balance != null) parts.push(`用户当前钱包余额 ${w.data.balance} 元`)
    }
  } catch { }
  try {
    const h = await dishHot()
    const list = Array.isArray(h?.data) ? h.data : (h?.data?.list || [])
    const names = list.slice(0, 5).map(d => d.name ? `${d.name}(${d.saleCount ?? '?'}份)` : '').filter(Boolean).join('、')
    if (names) parts.push(`今日热销榜前五：${names}`)
  } catch { }
  return parts
}

// 本地兜底规则（AI 挂了也能聊）
function replyOf(q) {
  if (/你是谁|你好|嗨|hello/i.test(q)) return '我是胆子肥嘟嘟！虽然胆子肥，但饭量更肥～管你点餐、查余额、讲笑话，样样都行！'
  if (/推荐|吃|点餐|啥/.test(q)) return '今天一号档口的「红烧排骨」超受欢迎，趁热冲！保守党可以来碗牛肉拉面，暖胃又顶饱～'
  if (/余额|钱包|钱/.test(q)) return '去「我的钱包」看看就知道啦，还够几顿小火锅我自己也好奇 😋'
  if (/订单|出餐|取餐/.test(q)) return '去「我的订单」看状态～商家出餐后凭取餐码取餐，配送单就等骑手电话哦'
  if (/优惠券|券|优惠/.test(q)) return '「优惠券」页面有可领的券，下单时能自动帮你选最划算的一张～'
  if (/投诉/.test(q)) return '对订单不满意可以在「我的订单」里发起投诉，商家回应后管理员会裁定，放心维权！'
  if (/笑话|搞笑|逗|无聊/.test(q)) return '食堂阿姨看了我一眼手就抖了——所以我减肥全靠缘分 😂'
  if (/减肥|瘦身|热量/.test(q)) return '想要低卡？推荐凉拌黄瓜和酸汤水饺！但我胖嘟嘟的样子…你就当我没说 🙈'
  return '这个问题有点超出我圆滚滚的脑容量了 😥 不过吃饭的事我都懂，要不要我帮你推荐几个菜？'
}

async function askDeepSeek(history) {
  const res = await fetch('https://api.deepseek.com/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${DS_KEY}`
    },
    body: JSON.stringify({
      model: 'deepseek-chat',
      messages: history,
      max_tokens: 220,
      temperature: 1.1
    })
  })
  if (!res.ok) throw new Error('deepseek http ' + res.status)
  const j = await res.json()
  const text = j?.choices?.[0]?.message?.content?.trim()
  if (!text) throw new Error('empty reply')
  return text
}

function push(role, text) {
  messages.value.push({ role, text })
  scrollToBottom()
  persist()
}

async function ask(text) {
  draft.value = ''
  push('user', text)
  typing.value = true
  scrollToBottom()

  // 实时数据注入（余额/热销榜）
  let history = [ ...seekHistory.value, { role: 'user', content: text } ]
  if (!seekHistory.value.length) history = [{ role: 'system', content: SYSTEM_PROMPT }, ...history]
  try {
    const parts = await buildRealtimeContext()
    if (parts.length) history = [...history, { role: 'system', content: '【实时数据】' + parts.join('；') }]
  } catch { }

  const finish = (botText) => {
    typing.value = false
    push('bot', botText)
  }

  if (!DS_KEY) { finish(replyOf(text)); return }

  askDeepSeek(history)
    .then(answer => {
      seekHistory.value = [...history.filter(m => m.role !== 'system' || !m.content.startsWith('【实时数据】')),
        { role: 'assistant', content: answer }].slice(-20)
      seekHistory.value = [{ role: 'system', content: SYSTEM_PROMPT }, ...seekHistory.value.filter(m => m.role !== 'system')]
      persist()
      finish(answer)
    })
    .catch(() => finish(replyOf(text)))
}

function send() {
  const text = draft.value.trim()
  if (text) ask(text)
}

function scrollToBottom() {
  nextTick(() => { if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight })
}

onMounted(() => {
  loadHistory()
  if (!messages.value.length) {
    messages.value = [{ role: 'bot', text: '嗨～我是胆子肥嘟嘟，你的专属食堂小管家！点下方问题，或直接打字问我吧～ 🍚' }]
  }
  greeting.value = timeGreet()
  showGreet.value = true
  setTimeout(() => { if (!chatOpen.value) showGreet.value = false }, 8000)
  idleTimer = setInterval(idleChatter, 100000) // 每 100 秒待机搭话一次
})

onBeforeUnmount(() => { if (idleTimer) clearInterval(idleTimer) })
</script>

<style scoped>
.mascot {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 3000;
}

/* ---- 小人按钮（可拖动，位置由内联样式控制） ---- */
.doll {
  display: block;
  position: relative;
  border: none;
  background: transparent;
  padding: 0;
  cursor: grab;
  touch-action: none;
  user-select: none;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(0 8px 16px rgba(147, 151, 122, 0.45));
  animation: doll-float 3.4s ease-in-out infinite;
}
.doll:hover { transform: scale(1.12) rotate(-4deg); }
.doll:active { cursor: grabbing; }
.doll.hop { animation: doll-hop 0.5s ease; }
@keyframes doll-float {
  0%, 100% { translate: 0 0; }
  50% { translate: 0 -7px; }
}
@keyframes doll-hop {
  0%, 100% { scale: 1 1; }
  40% { scale: 0.92 1.12; translate: 0 -14px; }
  70% { scale: 1.08 0.9; translate: 0 0; }
}
.shadow { animation: shadow-breathe 3.4s ease-in-out infinite; }
@keyframes shadow-breathe { 50% { opacity: 0.13; } }
/* 呼吸空隙：眨眼 */
.eye { transform-origin: center; animation: blink 4.6s infinite; }
@keyframes blink {
  0%, 94%, 100% { transform: scaleY(1); }
  97% { transform: scaleY(0.1); }
}
/* 挥手 */
.arm-left { transform-origin: 22px 70px; animation: wave 2.6s ease-in-out infinite; }
@keyframes wave {
  0%, 55%, 100% { transform: rotate(0); }
  65% { transform: rotate(-38deg); }
  75% { transform: rotate(-14deg); }
  85% { transform: rotate(-34deg); }
}

/* ---- 打招呼气泡 ---- */
.greet-bubble {
  position: absolute;
  right: 76px;
  bottom: 22px;
  display: flex;
  align-items: center;
  white-space: nowrap;
  cursor: pointer;
  animation: greet-in 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes greet-in {
  from { opacity: 0; transform: translateX(14px); }
}
.greet-text {
  padding: 9px 14px;
  border-radius: 14px;
  border-bottom-right-radius: 4px;
  font-size: 13px;
  color: #5e5f4e;
  background: #fff;
  box-shadow: 0 6px 18px rgba(147, 151, 122, 0.3);
}
.greet-tail {
  width: 10px; height: 10px;
  margin-left: -3px;
  background: #fff;
  clip-path: polygon(0 0, 100% 50%, 0 100%);
  box-shadow: 4px 2px 8px rgba(147, 151, 122, 0.12);
}

/* ---- 对话窗口 ---- */
.chat-window {
  position: absolute;
  right: 0;
  bottom: 82px;
  width: 320px;
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  overflow: hidden;
  background: #fdfbf5;
  box-shadow: 0 18px 48px rgba(94, 95, 78, 0.3);
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: linear-gradient(120deg, #93977a 0%, #a4ab8d 100%);
  color: #fff;
}
.chat-title { display: flex; align-items: center; gap: 10px; }
.chat-avatar {
  display: grid; place-items: center;
  width: 36px; height: 36px;
  border-radius: 50%;
  background: #fff8ec;
  font-size: 18px;
}
.chat-name { font-weight: 600; font-size: 14px; }
.chat-status { font-size: 11px; opacity: 0.85; }
.chat-tools { display: flex; gap: 6px; }
.chat-tool {
  border: none; background: transparent;
  color: #fff; font-size: 13px; cursor: pointer;
  opacity: 0.75; transition: opacity 0.2s;
  padding: 2px 4px;
}
.chat-tool:hover { opacity: 1; }

.chat-body {
  height: 260px;
  overflow-y: auto;
  padding: 10px 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hist-tip {
  text-align: center;
  font-size: 10.5px;
  color: #b3b5a0;
  padding-bottom: 2px;
}
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.bubble {
  max-width: 78%;
  padding: 9px 12px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.55;
  color: #4f5040;
  background: #eceada;
  border-bottom-left-radius: 4px;
}
.msg.user .bubble {
  color: #fff;
  background: #93977a;
  border-bottom-left-radius: 14px;
  border-bottom-right-radius: 4px;
}
.bubble.typing .dot {
  display: inline-block;
  margin-left: 3px;
  animation: dot-blink 1.2s infinite;
}
.bubble.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.bubble.typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot-blink { 0%, 60% { opacity: 0.3; } 30% { opacity: 1; } }

.chat-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 12px 10px;
}
.chat-quick button {
  border: 1px solid #cfd3b8;
  background: #f4f2e6;
  color: #7d8168;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}
.chat-quick button:hover { background: #93977a; color: #fff; border-color: #93977a; }

.chat-input {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #eceada;
  background: #fbf9f1;
}

/* 出场动画 */
.mascot-pop-enter-active, .mascot-pop-leave-active {
  transition: opacity 0.24s ease, transform 0.24s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.mascot-pop-enter-from, .mascot-pop-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.92);
}
</style>
