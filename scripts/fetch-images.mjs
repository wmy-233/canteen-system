// 从 Bing 图片搜索按菜名抓真实照片，下载到 frontend/public/img/
// 运行：node scripts/fetch-images.mjs
import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'

const DISHES = [
  [1, '宫保鸡丁'],
  [2, '辣椒炒肉'],
  [3, '红烧排骨'],
  [4, '干煸四季豆'],
  [5, '麻婆豆腐'],
  [6, '米饭 白米饭'],
  [7, '酸梅汤'],
  [8, '兰州牛肉拉面'],
  [9, '羊肉泡馍'],
  [10, '刀削面'],
  [11, '凉拌黄瓜'],
  [12, '烤肠'],
  [13, '麻辣香锅'],
  [14, '麻辣香锅 微辣'],
  [15, '冰粉'],
  [16, '糖醋里脊'],
  [17, '蛋炒饭'],
  [18, '酸汤水饺'],
  [19, '卤牛肉'],
  [20, '柠檬水'],
]

const OUT = path.resolve('frontend/public/img')
await mkdir(OUT, { recursive: true })

const UA = { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126 Safari/537.36' }
const results = []

for (const [id, q] of DISHES) {
  try {
    const searchUrl = `https://www.bing.com/images/search?q=${encodeURIComponent(q)}&form=HDRSC2&first=1`
    const page = await (await fetch(searchUrl, { headers: UA })).text()
    const m = page.match(/class="iusc"[^>]*? m="([^"]+)"/)
    if (!m) { console.log(id, q, 'NO RESULT'); results.push([id, null]); continue }
    const meta = JSON.parse(m[1].replace(/&quot;/g, '"').replace(/&amp;/g, '&'))
    // turl = Bing 缩略图 CDN，改大尺寸
    let url = (meta.turl || '').replace(/w=\d+/, 'w=640').replace(/h=\d+/, 'h=480')
    if (!url && meta.murl) url = meta.murl
    if (!url) { console.log(id, q, 'NO URL'); results.push([id, null]); continue }
    const img = await fetch(url, { headers: { 'User-Agent': UA['User-Agent'], Referer: 'https://www.bing.com/' } })
    if (!img.ok) { console.log(id, q, 'DL FAIL', img.status); results.push([id, null]); continue }
    const buf = Buffer.from(await img.arrayBuffer())
    if (buf.length < 4000) { console.log(id, q, 'TOO SMALL', buf.length); results.push([id, null]); continue }
    await writeFile(path.join(OUT, `dish-${id}.jpg`), buf)
    console.log(id, q, 'OK', buf.length, 'bytes  src=', (meta.murl || '').slice(0, 60))
    results.push([id, `/img/dish-${id}.jpg`])
    await new Promise(r => setTimeout(r, 300))
  } catch (e) {
    console.log(id, q, 'ERROR', e.message)
    results.push([id, null])
  }
}

console.log('\n---- SQL ----')
const lines = results.filter(r => r[1]).map(([id, p]) => `UPDATE dish SET image='${p}' WHERE id=${id};`)
lines.forEach(l => console.log(l))
await writeFile('scripts/dish_images.sql', lines.join('\n') + '\n')
console.log('\nSQL saved to scripts/dish_images.sql')
