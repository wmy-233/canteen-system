<template>
  <!-- 悬浮智能助手：Q版胖嘟嘟小人（可拖动，窗口跟随） -->
  <div class="mascot" :class="{ open: chatOpen }" :style="mascotStyle">
    <!-- 对话窗口 -->
    <transition name="mascot-pop">
      <div v-if="chatOpen" class="chat-window" :style="winStyle">
        <div class="chat-header">
          <div class="chat-title">
            <span class="chat-avatar">🤗</span>
            <div>
              <div class="chat-name">胆子肥嘟嘟</div>
              <div class="chat-status">●DeepSeek 在线 · 你的食堂小管家</div>
            </div>
          </div>
          <div class="chat-tools">
            <button class="chat-tool" title="历史会话" @click="histOpen = !histOpen">🕘</button>
            <el-dropdown trigger="click" popper-class="dudu-menu" @command="onMenu">
              <button class="chat-tool" title="更多">⚙</button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="new">🆕 新建对话</el-dropdown-item>
                  <el-dropdown-item command="clear" divided>🗑 清空当前记录…</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <button class="chat-tool" @click="chatOpen = false">✕</button>
          </div>
        </div>

        <!-- 点击面板外空白处关闭的遮罩 -->
        <div v-if="histOpen" class="hist-mask" @click="histOpen = false" />

        <!-- 历史会话侧滑面板 -->
        <transition name="hist-slide">
          <div v-if="histOpen" class="hist-panel">
            <div class="hist-head">
              <span>🕘 聊天历史（{{ sessions.length }} 个会话）</span>
              <button class="hist-close" title="收起" @click="histOpen = false">‹</button>
            </div>
            <div
              v-for="s in sessions" :key="s.id"
              class="hist-item" :class="{ active: s.id === currentId }"
              @click="switchSession(s.id)"
            >
              <div class="hi-main">
                <div class="hi-title">{{ s.title }}</div>
                <div class="hi-meta">{{ (s.msgs.length - 1) }} 条 · {{ fmtTime(s.updateTime) }}</div>
              </div>
              <button class="hi-del" title="删除此会话" @click.stop="removeSession(s.id)">🗑</button>
            </div>
            <el-empty v-if="sessions.length <= 1" description="还没有更多历史会话" :image-size="40" />
          </div>
        </transition>

        <!-- 查看历史会话提示条 -->
        <div v-if="viewingHistory" class="viewing-tip">
          正在查看历史会话，点击左下角新建对话可开启新话题
        </div>

        <div ref="msgList" class="chat-body">
          <div v-if="messages.length <= 1" class="empty-tip">
            <div class="et-emoji">🍚</div>
            今天想吃点什么？点下方问题或直接问我～
          </div>
          <template v-for="(m, i) in messages" :key="i">
            <div :class="['msg', m.role]">
              <div class="bubble" v-html="m.role === 'bot' ? md(m.text) : esc(m.text)" />
            </div>
            <!-- 菜品推荐微型卡片（识别到菜名时渲染） -->
            <div v-if="m.role === 'bot' && dishOf(m.text)" class="msg bot">
              <div class="dish-mini">
                <img :src="dishOf(m.text).image" alt="">
                <div class="dm-info">
                  <div class="dm-name">{{ dishOf(m.text).name }}</div>
                  <div class="dm-meta">¥{{ fmt(dishOf(m.text).price) }} · {{ dishOf(m.text).stallName }}</div>
                </div>
                <el-button size="small" type="primary" round @click="addCart(dishOf(m.text))">去加购</el-button>
              </div>
            </div>
          </template>
          <div v-if="typing" class="msg bot">
            <div class="bubble typing">正在输入<span class="dot">·</span><span class="dot">·</span><span class="dot">·</span></div>
          </div>
        </div>

        <div class="chat-quick">
          <button v-for="q in quickQuestions" :key="q" @click="ask(q)">{{ q }}</button>
        </div>

        <form class="chat-input" @submit.prevent="send">
          <el-tooltip content="新建对话，开启新话题" placement="top" popper-class="dudu-tip" :teleported="true">
            <button type="button" class="new-chat-btn" title="新建对话" @click="createSession">🆕</button>
          </el-tooltip>
          <el-tooltip content="图片/附件（多模态扩展中，敬请期待）" placement="top" popper-class="dudu-tip" :teleported="true">
            <button type="button" class="attach-btn">📎</button>
          </el-tooltip>
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
      @pointerdown="dragStart"
      @pointermove="dragMove"
      @pointerup="dragEnd"
      @pointercancel="dragEnd"
    >
      <svg viewBox="0 0 120 130" width="64" height="70" aria-label="胆子肥嘟嘟">
        <ellipse class="shadow" cx="60" cy="122" rx="26" ry="5" fill="rgba(110,110,95,0.25)" />
        <g class="doll-group">
          <path d="M56 12 Q52 4 58 2 Q60 6 62 10 Z" fill="#a4b08a" />
          <ellipse cx="26" cy="38" rx="9" ry="11" fill="#fff6e8" stroke="#e8dcc4" stroke-width="2" />
          <ellipse cx="94" cy="38" rx="9" ry="11" fill="#fff6e8" stroke="#e8dcc4" stroke-width="2" />
          <path d="M60 14 C 88 14 104 32 104 62 C 104 96 86 114 60 114 C 34 114 16 96 16 62 C 16 32 32 14 60 14 Z"
                fill="#fff8ec" stroke="#ecdfc6" stroke-width="2.5" />
          <path d="M44 20 Q50 14 60 18 Q70 14 76 20" fill="none" stroke="#e3d5ba" stroke-width="2.5" stroke-linecap="round" />
          <path d="M52 24 Q60 19 68 24" fill="none" stroke="#e9dcc3" stroke-width="2" stroke-linecap="round" />
          <ellipse class="eye" cx="44" cy="58" rx="5.2" ry="6.4" fill="#574f42" />
          <ellipse class="eye" cx="76" cy="58" rx="5.2" ry="6.4" fill="#574f42" />
          <ellipse class="eye" cx="45.6" cy="55.4" rx="1.7" ry="2.1" fill="#fff" />
          <ellipse class="eye" cx="77.6" cy="55.4" rx="1.7" ry="2.1" fill="#fff" />
          <ellipse cx="32" cy="70" rx="6.5" ry="4" fill="#f2b7a6" opacity="0.55" />
          <ellipse cx="88" cy="70" rx="6.5" ry="4" fill="#f2b7a6" opacity="0.55" />
          <path class="mouth" d="M52 72 Q56 77 60 72 Q64 77 68 72" fill="none" stroke="#574f42" stroke-width="2.4" stroke-linecap="round" />
          <ellipse cx="60" cy="94" rx="24" ry="16" fill="#fffdf7" stroke="#f0e6d2" stroke-width="2" />
          <circle cx="60" cy="94" r="8.5" fill="#a4b08a" opacity="0.9" />
          <circle cx="60" cy="91" r="3" fill="#fff" opacity="0.85" />
          <path d="M53 97 h14" stroke="#fff" stroke-width="2" stroke-linecap="round" opacity="0.85" />
          <g class="arm arm-left">
            <ellipse cx="17" cy="78" rx="8" ry="11" fill="#fff8ec" stroke="#ecdfc6" stroke-width="2.2" />
          </g>
          <ellipse cx="103" cy="78" rx="8" ry="11" fill="#fff8ec" stroke="#ecdfc6" stroke-width="2.2" />
          <ellipse cx="42" cy="116" rx="9" ry="6" fill="#fff6e8" stroke="#ecdfc6" stroke-width="2" />
          <ellipse cx="78" cy="116" rx="9" ry="6" fill="#fff6e8" stroke="#ecdfc6" stroke-width="2" />
        </g>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { cartAdd, dishPage, dishHot } from '../api/dish'
import { walletInfo } from '../api/wallet'

const chatOpen = ref(false)
const histOpen = ref(false)
const showGreet = ref(false)
const greeting = ref('充能完毕！我是胆子肥嘟嘟')
const typing = ref(false)
const viewingHistory = ref(false)
const draft = ref('')
const msgList = ref()
const hop = ref(false)
const userStore = useUserStore()

const quickQuestions = ['推荐点啥？', '📅 查今日特价', '💰 查余额', '📜 查订单', '给我讲个笑话', '有什么低卡的？']

// ================= 多会话历史（localStorage 持久化） =================
const SES_KEY = 'dududu-sessions'

function newSession() {
  return {
    id: Date.now() + '-' + Math.random().toString(36).slice(2, 6),
    title: '新的对话',
    msgs: [{ role: 'bot', text: '嗨～我是胆子肥嘟嘟，你的专属食堂小管家！点下方问题，或直接打字问我吧～ 🍚' }],
    seek: [],
    updateTime: Date.now()
  }
}

let sessions = ref([])
let currentId = ref('')

const messages = computed(() => {
  const s = sessions.value.find(x => x.id === currentId.value)
  return s ? s.msgs : []
})

function persist() {
  try { localStorage.setItem(SES_KEY, JSON.stringify(sessions.value.slice(0, 15))) } catch { }
}

function loadSessions() {
  try {
    const s = JSON.parse(localStorage.getItem(SES_KEY) || 'null')
    if (Array.isArray(s) && s.length) {
      sessions.value = s
      currentId.value = s[0].id
      return
    }
  } catch { }
  const ns = newSession()
  sessions.value = [ns]
  currentId.value = ns.id
}

function switchSession(id) {
  currentId.value = id
  histOpen.value = false
  // 切到含真实对话的旧会话时提示上下文已恢复
  const s = sessions.value.find(x => x.id === id)
  viewingHistory.value = !!s && s.msgs.length > 1
  scrollToBottom()
}

function createSession() {
  const ns = newSession()
  sessions.value = [ns, ...sessions.value]
  currentId.value = ns.id
  histOpen.value = false
  viewingHistory.value = false
  persist()
}

// 删除单条会话（带确认）
async function removeSession(id) {
  try {
    await ElMessageBox.confirm('确定要删除此条会话吗？', '删除会话', {
      confirmButtonText: '删除', cancelButtonText: '再想想', type: 'warning',
      customClass: 'dudu-confirm'
    })
  } catch { return }
  const idx = sessions.value.findIndex(x => x.id === id)
  if (idx < 0) return
  sessions.value.splice(idx, 1)
  if (currentId.value === id) {
    if (sessions.value.length) currentId.value = sessions.value[0].id
    else { const ns = newSession(); sessions.value = [ns]; currentId.value = ns.id }
    viewingHistory.value = false
  }
  persist()
  ElMessage.success('已删除该会话')
}

async function clearCurrent() {
  try {
    await ElMessageBox.confirm('确定要清空当前聊天记录吗？', '清空记录', {
      confirmButtonText: '清空', cancelButtonText: '再想想', type: 'warning',
      customClass: 'dudu-confirm'
    })
  } catch { return }
  const s = sessions.value.find(x => x.id === currentId.value)
  if (s) {
    s.msgs = [{ role: 'bot', text: '记录清空啦！我是胆子肥嘟嘟，重新认识一下～ 🍚' }]
    s.seek = []
    s.updateTime = Date.now()
  }
  viewingHistory.value = false
  persist()
  ElMessage.success('已清空当前对话')
}

function onMenu(cmd) {
  if (cmd === 'new') createSession()
  if (cmd === 'clear') clearCurrent()
}

function fmtTime(t) {
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ================= Markdown 迷你渲染 =================
function esc(t) {
  return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
function md(t) {
  return esc(t)
    .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^\s*[-·]\s+(.+)$/gm, '· $1')
    .replace(/\n/g, '<br>')
}

// ================= 菜品索引（推荐卡片 + 去加购） =================
const dishIndex = ref([])

async function loadDishIndex() {
  if (userStore.role !== 'STUDENT') return
  try {
    const res = await dishPage({ pageNum: 1, pageSize: 100 })
    dishIndex.value = (res.data?.list || []).filter(d => d.image)
  } catch { }
}

function dishOf(text) {
  if (!text || !dishIndex.value.length) return null
  return dishIndex.value.find(d => d.name && text.includes(d.name)) || null
}

async function addCart(d) {
  try {
    await cartAdd({ dishId: d.id, quantity: 1 })
    ElMessage.success(`已把「${d.name}」加入购物车 🛒`)
  } catch {
    ElMessage.info('请先登录学生账号再加购哦～')
  }
}

const fmt = v => Number(v ?? 0).toFixed(2)

// ================= 时间段问候 + 待机搭话 =================
function timeGreet() {
  const h = new Date().getHours()
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username
  const who = name ? `${name}同学` : '你'
  if (h < 10) return `${who}早上好！记得吃早饭哦 🌅`
  if (h < 14) return `${who}中午吃啥想好了吗？我帮你参谋！🍚`
  if (h < 18) return `${who}下午茶时间～来点小吃犒劳自己？🧋`
  return `${who}晚上好！夜宵虽香，可不要贪嘴哦 🌙`
}

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

// ================= 拖动（窗口跟随容器一起动） =================
const DOLL_KEY = 'dududu-pos'
const pos = reactive(JSON.parse(localStorage.getItem(DOLL_KEY) || 'null') || { right: 26, bottom: 26 })
let dragState = null

const mascotStyle = computed(() => ({ right: pos.right + 'px', bottom: pos.bottom + 'px' }))

// 窗口防出屏：靠左时向右展开，靠上时向下展开
const winStyle = computed(() => {
  const st = {}
  // pos.right 很大 = 小人贴着屏幕左侧 → 窗口改向右展开
  if (pos.right > window.innerWidth - 400) { st.left = '0'; st.right = 'auto' }
  // pos.bottom 很大 = 小人贴着屏幕上方 → 窗口改为向下展开
  if (pos.bottom > window.innerHeight - 540) { st.top = '70px'; st.bottom = 'auto' }
  return st
})

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
  chatOpen.value ? (chatOpen.value = false) : open()
}

// ================= DeepSeek 真实 AI 问答 =================
const DS_KEY = import.meta.env.VITE_DEEPSEEK_API_KEY || ''

const SYSTEM_PROMPT = [
  '你是「胆子肥嘟嘟」，智慧校园食堂点餐系统的吉祥物兼小管家，一只圆滚滚的包子小人。',
  '人设：活泼可爱、爱吃爱玩、有点小胖但自信，说话带一点颜文字或 emoji，回复简短（不超过 4 句），口语化。',
  '支持 Markdown：适当用 **加粗** 强调菜名/金额。',
  '推荐菜品时尽量点名具体菜品（系统会自动渲染菜品卡片）。',
  '系统里有川湘家常菜、兰州拉面、麻辣香锅三个档口，菜品如宫保鸡丁、红烧排骨、麻婆豆腐、牛肉拉面、羊肉泡馍、麻辣香锅、酸梅汤、冰粉等。',
  '涉及余额/订单状态等个人数据时，若下方有【实时数据】就直接引用具体数字；没有就提醒用户去「我的钱包」「我的订单」页面查看。',
  '与食堂、吃饭、学习生活无关的敏感话题请礼貌带回到吃饭话题。'
].join('\n')

// 实时数据（余额缓存 + 热销榜）
let lastBalance = null

async function buildRealtimeContext() {
  const parts = []
  try {
    if (userStore.role === 'STUDENT') {
      const w = await walletInfo()
      if (w?.data?.balance != null) {
        lastBalance = w.data.balance
        parts.push(`用户当前钱包余额 ${w.data.balance} 元`)
      }
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

// 本地兜底（AI 挂了也能聊，余额用缓存真数据）
function replyOf(q) {
  if (/你是谁|你好|嗨|hello/i.test(q)) return '我是胆子肥嘟嘟！虽然胆子肥，但饭量更肥～管你点餐、查余额、讲笑话，样样都行！'
  if (/推荐|吃|点餐|啥/.test(q)) return '今天热销的「红烧排骨」超受欢迎，趁热冲！保守党可以来碗**牛肉拉面**，暖胃又顶饱～'
  if (/余额|钱包|钱/.test(q)) return lastBalance != null ? `你当前钱包余额 **${lastBalance} 元**，还够几顿小火锅我也好奇 😋` : '去「我的钱包」看看就知道啦～'
  if (/订单|出餐|取餐/.test(q)) return '去「我的订单」看状态～商家出餐后凭取餐码取餐，配送单就等骑手电话哦'
  if (/优惠券|券|优惠/.test(q)) return '「优惠券」页面有可领的券，下单时能自动帮你选最划算的一张～'
  if (/投诉/.test(q)) return '对订单不满意可以在「我的订单」里发起投诉，商家回应后管理员会裁定，放心维权！'
  if (/笑话|搞笑|逗|无聊/.test(q)) return '食堂阿姨看了我一眼手就抖了——所以我减肥全靠缘分 😂'
  if (/减肥|瘦身|热量/.test(q)) return '想要低卡？推荐**凉拌黄瓜**和**酸汤水饺**！但我胖嘟嘟的样子…你就当我没说 🙈'
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
      max_tokens: 260,
      temperature: 1.1
    })
  })
  if (!res.ok) throw new Error('deepseek http ' + res.status)
  const j = await res.json()
  const text = j?.choices?.[0]?.message?.content?.trim()
  if (!text) throw new Error('empty reply')
  return text
}

// 根据用户第一句提问智能命名
function smartTitle(text) {
  const t = text.trim()
  if (/特价|打折|优惠活动/.test(t)) return '查今日特价'
  if (/余额|钱包|还有多少钱/.test(t)) return '查余额'
  if (/订单|出餐|取餐|配送/.test(t)) return '查订单'
  if (/推荐|吃啥|点什么|好吃的| suggest/i.test(t)) return '菜品推荐'
  if (/优惠券|券/.test(t)) return '优惠券'
  if (/笑话|搞笑|逗/.test(t)) return '讲笑话'
  if (/低卡|减肥|热量|瘦/.test(t)) return '低卡推荐'
  return t.slice(0, 14) || '新的对话'
}

function push(role, text) {
  const s = sessions.value.find(x => x.id === currentId.value)
  if (!s) return
  s.msgs.push({ role, text })
  if (role === 'user') {
    const isFresh = s.msgs.length <= 2 // 第一句用户消息
    if (isFresh || s.title === '新的对话') s.title = smartTitle(text)
  }
  s.updateTime = Date.now()
  persist()
  scrollToBottom()
}

// 打字机逐字输出
let typeTimer = null
function typeOut(botText) {
  typing.value = false
  const s = sessions.value.find(x => x.id === currentId.value)
  if (!s) { push('bot', botText); return }
  s.msgs.push({ role: 'bot', text: '' })
  s.updateTime = Date.now()
  scrollToBottom()
  let i = 0
  clearInterval(typeTimer)
  typeTimer = setInterval(() => {
    const cur = sessions.value.find(x => x.id === currentId.value)
    if (!cur) { clearInterval(typeTimer); return }
    const m = cur.msgs[cur.msgs.length - 1]
    if (!m || m.role !== 'bot') { clearInterval(typeTimer); return }
    // 每次蹦 1~2 个字，节奏更自然
    i += Math.random() < 0.35 ? 2 : 1
    m.text = botText.slice(0, i)
    if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
    if (i >= botText.length) {
      clearInterval(typeTimer)
      persist()
    }
  }, 20)
}

async function ask(text) {
  draft.value = ''
  viewingHistory.value = false
  push('user', text)
  typing.value = true
  scrollToBottom()

  const s = sessions.value.find(x => x.id === currentId.value)
  let history = [...(s?.seek || []), { role: 'user', content: text }]
  if (!s?.seek?.length) history = [{ role: 'system', content: SYSTEM_PROMPT }, ...history]
  try {
    const parts = await buildRealtimeContext()
    if (parts.length) history = [...history, { role: 'system', content: '【实时数据】' + parts.join('；') }]
  } catch { }

  const finish = (botText) => {
    typeOut(botText)
  }

  if (!DS_KEY) { finish(replyOf(text)); return }

  askDeepSeek(history)
    .then(answer => {
      if (s) {
        s.seek = [...history.filter(m => !(m.role === 'system' && m.content.startsWith('【实时数据】'))),
          { role: 'assistant', content: answer }].slice(-20)
        s.seek = [{ role: 'system', content: SYSTEM_PROMPT }, ...s.seek.filter(m => m.role !== 'system')]
        persist()
      }
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

// ---------- 打开对话窗 ----------
function open() {
  chatOpen.value = true
  showGreet.value = false
  greeting.value = timeGreet()
  scrollToBottom()
}

onMounted(() => {
  loadSessions()
  loadDishIndex()
  greeting.value = timeGreet()
  showGreet.value = true
  setTimeout(() => { if (!chatOpen.value) showGreet.value = false }, 8000)
  idleTimer = setInterval(idleChatter, 100000)
})

onBeforeUnmount(() => { if (idleTimer) clearInterval(idleTimer); if (typeTimer) clearInterval(typeTimer) })
</script>

<style scoped>
.mascot {
  position: fixed;
  z-index: 3000;
}

/* ---- 小人按钮（容器已带拖动位置） ---- */
.doll {
  display: block;
  border: none;
  background: transparent;
  padding: 0;
  cursor: grab;
  touch-action: none;
  user-select: none;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s;
  filter: drop-shadow(0 8px 16px rgba(147, 151, 122, 0.45));
  animation: doll-float 3.4s ease-in-out infinite;
}
.doll:hover { transform: scale(1.12) rotate(-4deg); }
.doll:active { cursor: grabbing; }
.doll.hop { animation: doll-hop 0.5s ease; }
/* 聊天窗打开时：小人缩小让位，不遮挡聊天记录 */
.mascot.open .doll {
  transform: scale(0.55);
  opacity: 0.85;
  animation: none;
}
@keyframes doll-float {
  0%, 100% { translate: 0 0; }
  50% { translate: 0 -7px; }
}
@keyframes doll-hop {
  0%, 100% { scale: 1 1; }
  40% { scale: 0.92 1.12; translate: 0 -14px; }
  70% { scale: 1.08 0.9; translate: 0 0; }
}
.eye { transform-origin: center; animation: blink 4.6s infinite; }
@keyframes blink {
  0%, 94%, 100% { transform: scaleY(1); }
  97% { transform: scaleY(0.1); }
}
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
  width: 350px;
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
.chat-tools { display: flex; gap: 4px; align-items: center; }
.chat-tool {
  border: none; background: transparent;
  color: #fff; font-size: 14px; cursor: pointer;
  opacity: 0.8; transition: opacity 0.2s;
  padding: 2px 5px;
}
.chat-tool:hover { opacity: 1; }

/* 历史面板遮罩（点击空白处关闭） */
.hist-mask {
  position: absolute;
  inset: 0;
  z-index: 4;
}
/* 历史会话侧滑面板 */
.hist-panel {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 240px;
  background: #f8f6ec;
  box-shadow: 6px 0 20px rgba(94, 95, 78, 0.18);
  padding: 12px;
  overflow-y: auto;
  z-index: 5;
}
.hist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 700;
  font-size: 13px;
  color: #5e5f4e;
  margin-bottom: 10px;
}
.hist-close {
  border: none;
  background: transparent;
  color: #7d8168;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 8px;
  transition: all 0.15s;
}
.hist-close:hover { background: #e3e6d0; color: #4f5040; }
.hist-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 9px 6px 9px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 6px;
}
.hist-item:hover { background: #eeeadd; }
.hist-item.active { background: #e3e6d0; }
.hi-main { flex: 1; min-width: 0; }
.hi-title {
  font-size: 13px;
  color: #4f5040;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hi-meta { font-size: 11px; color: #9b9d84; margin-top: 3px; }
/* hover 显示删除按钮 */
.hi-del {
  flex: none;
  border: none;
  background: transparent;
  font-size: 13px;
  cursor: pointer;
  opacity: 0;
  padding: 3px 4px;
  border-radius: 6px;
  transition: opacity 0.15s, background 0.15s;
}
.hist-item:hover .hi-del { opacity: 0.75; }
.hi-del:hover { opacity: 1 !important; background: #f3ded6; }
.hist-slide-enter-active, .hist-slide-leave-active { transition: transform 0.25s ease, opacity 0.25s; }
.hist-slide-enter-from, .hist-slide-leave-to { transform: translateX(-100%); opacity: 0; }

/* 查看历史会话提示条 */
.viewing-tip {
  padding: 6px 14px;
  font-size: 11.5px;
  color: #8a6d3b;
  background: #f6efdc;
  border-bottom: 1px dashed #e3d9bd;
}
/* 消息区 */
.chat-body {
  height: 280px;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
/* 细浅色圆角滚动条 */
.chat-body::-webkit-scrollbar { width: 4px; }
.chat-body::-webkit-scrollbar-track { background: transparent; }
.chat-body::-webkit-scrollbar-thumb {
  background: #d6d5c2;
  border-radius: 4px;
}
.chat-body::-webkit-scrollbar-thumb:hover { background: #c2c1ae; }
.empty-tip {
  margin: auto;
  text-align: center;
  color: #8a8b7a;
  font-size: 13px;
}
.et-emoji { font-size: 34px; margin-bottom: 8px; }
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.bubble {
  max-width: 80%;
  padding: 9px 12px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.55;
  color: #4f5040;
  background: #eceada;
  border-bottom-left-radius: 4px;
  word-break: break-word;
}
.bubble :deep(b) { color: #4a4c3a; }
.bubble :deep(code) {
  background: #fffdf2;
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
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

/* 菜品推荐微型卡片 */
.dish-mini {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #e7e4d2;
  border-radius: 12px;
  padding: 8px;
  max-width: 88%;
  box-shadow: 0 3px 10px rgba(147, 151, 122, 0.12);
}
.dish-mini img {
  width: 54px;
  height: 44px;
  object-fit: cover;
  border-radius: 8px;
  flex: none;
}
.dm-info { flex: 1; min-width: 0; }
.dm-name {
  font-size: 13px;
  font-weight: 600;
  color: #4f5040;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dm-meta { font-size: 11.5px; color: #9b9d84; margin-top: 2px; }

/* 快捷问题：横向滑动紧凑气泡 */
.chat-quick {
  display: flex;
  gap: 6px;
  padding: 0 12px 10px;
  overflow-x: auto;
  scrollbar-width: none;
}
.chat-quick::-webkit-scrollbar { display: none; }
.chat-quick button {
  flex: none;
  border: 1px solid #cfd3b8;
  background: #f4f2e6;
  color: #7d8168;
  font-size: 12px;
  padding: 4px 11px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.chat-quick button:hover { background: #93977a; color: #fff; border-color: #93977a; }

/* 输入区：加宽 + 附件占位 */
.chat-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #eceada;
  background: #fbf9f1;
}
.attach-btn {
  flex: none;
  border: none;
  background: transparent;
  font-size: 16px;
  cursor: not-allowed;
  opacity: 0.55;
  padding: 2px;
}
.new-chat-btn {
  flex: none;
  border: none;
  background: transparent;
  font-size: 15px;
  cursor: pointer;
  opacity: 0.75;
  padding: 2px;
  transition: opacity 0.15s, transform 0.15s;
}
.new-chat-btn:hover { opacity: 1; transform: scale(1.15); }
.chat-input :deep(.el-input) { flex: 1; }

/* 出场动画 */
.mascot-pop-enter-active, .mascot-pop-leave-active {
  transition: opacity 0.24s ease, transform 0.24s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.mascot-pop-enter-from, .mascot-pop-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.92);
}
</style>

<!-- popper / message-box 默认已挂载到 body，但被 mascot(z-index:3000) 盖住，需全局提层 -->
<style>
.dudu-menu { z-index: 4000 !important; }
/* Tooltip 提示词浮于聊天窗之上 */
.dudu-tip { z-index: 9999 !important; }
/* 确认框本体 + 它的全屏半透明遮罩都提到顶层 */
.dudu-confirm { z-index: 9999 !important; }
.el-overlay:has(.dudu-confirm) {
  z-index: 9999 !important;
  background: rgba(0, 0, 0, 0.45);
}
</style>
