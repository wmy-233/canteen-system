<template>
  <!-- 悬浮智能助手：Q版胖嘟嘟小人 -->
  <div class="mascot" :class="{ open: chatOpen }">
    <!-- 对话窗口 -->
    <transition name="mascot-pop">
      <div v-if="chatOpen" class="chat-window">
        <div class="chat-header">
          <div class="chat-title">
            <span class="chat-avatar">🤗</span>
            <div>
              <div class="chat-name">胆子肥嘟嘟</div>
              <div class="chat-status">●在线 · 你的食堂小管家</div>
            </div>
          </div>
          <button class="chat-close" @click="chatOpen = false">✕</button>
        </div>

        <div ref="msgList" class="chat-body">
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

    <!-- 打招呼气泡 -->
    <transition name="mascot-pop">
      <div v-if="!chatOpen && showGreet" class="greet-bubble" @click="open()">
        <span class="greet-text">{{ greeting }}</span><span class="greet-tail"></span>
      </div>
    </transition>

    <!-- 小人本体（SVG 手绘） -->
    <button class="doll" :class="{ hop: hop }" @click="open()">
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
import { nextTick, onMounted, ref } from 'vue'
import { useUserStore } from '../stores/user'

const chatOpen = ref(false)
const showGreet = ref(false)
const greeting = ref('充能完毕！我是胆子肥嘟嘟')
const typing = ref(false)
const draft = ref('')
const msgList = ref()
const userStore = useUserStore()

const quickQuestions = ['推荐点啥？', '余额还剩多少？', '订单在哪看？', '给我讲个笑话']

const messages = ref([
  { role: 'bot', text: '嗨～我是胆子肥嘟嘟，你的专属食堂小管家！点击下方问题，或直接打字问我吧～ 🍚' }
])

// 打招呼语
onMounted(() => {
  showGreet.value = true
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username
  if (name) greeting.value = `${name}同学，今天想吃点啥？`
  setTimeout(() => { if (!chatOpen.value) showGreet.value = false }, 8000)
})

function open() {
  chatOpen.value = true
  showGreet.value = false
  ask('你是谁呀')
}

// 关键词规则问答（mock，后续可替换为 AI 接口）
function replyOf(q) {
  if (/你是谁|你好|嗨|hello/i.test(q)) return '我是胆子肥嘟嘟！虽然胆子肥，但饭量更肥～管你点餐、查余额、讲笑话，样样都行！'
  if (/推荐|吃|点餐|啥/.test(q)) return '今天一号档口的「红烧肉套餐」限量快抢完了，趁热闭眼冲！保守党可以选二号的番茄炒蛋，8 块钱吃着不心疼～'
  if (/余额|钱包|钱/.test(q)) return '你的钱包余额是 58.50 元（mock 数据），还够两顿小火锅！'
  if (/订单|物流|出餐/.test(q)) return '你有一条进行中的订单：番茄炒蛋 ×1，商家正在出餐～去「我的订单」就能看状态啦'
  if (/优惠券|券|优惠/.test(q)) return '你有 2 张可用优惠券，其中一张今天到期，别浪费啦！'
  if (/笑话|搞笑|逗|无聊/.test(q)) return '食堂阿姨看了我一眼手就抖了——所以我减肥全靠缘分 😂'
  if (/减肥|瘦身|热量/.test(q)) return '想要低卡？推荐清蒸系列和白灼青菜！但我胖嘟嘟的样子…你就当我没说 🙈'
  if (/档口|食堂|门/.test(q)) return '全校 12 个档口都在营业中，川湘风味排队长但值！'
  return '这个问题有点超出我圆滚滚的脑容量了 😥 你可以问吃什么、余额、订单、优惠券，或者让我讲个笑话～'
}

function ask(text) {
  draft.value = ''
  messages.value.push({ role: 'user', text })
  typing.value = true
  scrollToBottom()
  setTimeout(() => {
    typing.value = false
    messages.value.push({ role: 'bot', text: replyOf(text) })
    scrollToBottom()
  }, 700 + Math.random() * 600)
}

function send() {
  const text = draft.value.trim()
  if (text) ask(text)
}

function scrollToBottom() {
  nextTick(() => { if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight })
}
</script>

<style scoped>
.mascot {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 3000;
}

/* ---- 小人按钮 ---- */
.doll {
  display: block;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(0 8px 16px rgba(147, 151, 122, 0.45));
  animation: doll-float 3.4s ease-in-out infinite;
}
.doll:hover { transform: scale(1.12) rotate(-4deg); }
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
.chat-close {
  border: none; background: transparent;
  color: #fff; font-size: 15px; cursor: pointer;
  opacity: 0.75; transition: opacity 0.2s;
}
.chat-close:hover { opacity: 1; }

.chat-body {
  height: 260px;
  overflow-y: auto;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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
