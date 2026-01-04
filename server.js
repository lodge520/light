// server.js
const dgram     = require('dgram');
const WebSocket = require('ws');
const express   = require('express');
const cors      = require('cors');
const path      = require('path');
const lastSeenFile = path.join(__dirname, 'lastSeen.json');
const axios = require('axios');
const fs = require('fs');
if (!fs.existsSync(lastSeenFile)) {
  fs.writeFileSync(lastSeenFile, '{}');  
}

const luxFile = path.join(__dirname, 'luxData.json');
if (!fs.existsSync(luxFile)) fs.writeFileSync(luxFile, '{}');

function updateLastSeen(id, timestamp = Date.now()) {
  const map = JSON.parse(fs.readFileSync(lastSeenFile));
  map[id] = timestamp;
  fs.writeFileSync(lastSeenFile, JSON.stringify(map, null, 2));
}
const http = require('http'); 
const app         = express();
const udpServer   = dgram.createSocket('udp4');
const udpPort     = 4210;
const wss         = new WebSocket.Server({ port: 8081 });
const httpPort    = 3000;
const devicesFile = path.join(__dirname, 'devices.json');
const uploadDir   = path.join(__dirname, 'uploads');
const Jimp = require('jimp');
const KMeansLib = require('ml-kmeans');
const KMeans = KMeansLib.kmeans; 
const { spawn } = require('child_process');

const deviceStatusMap = {}; 

app.use(cors());
app.use(express.json());

if (!fs.existsSync(devicesFile)) fs.writeFileSync(devicesFile, '[]');
if (!fs.existsSync(uploadDir))  fs.mkdirSync(uploadDir);

app.get('/devices', (req, res) => {
  const list = JSON.parse(fs.readFileSync(devicesFile));
  res.json(list);
});

app.get('/onlineDevices', (req, res) => {
  const now = Date.now();
  const onlineMap = {};

  const lastSeenMap = JSON.parse(fs.readFileSync(lastSeenFile)); 

  for (const id in lastSeenMap) {
    const last = lastSeenMap[id];
    onlineMap[id] = {
      online: now - last < 60000,  
      lastSeen: last
    };
  }

  res.json(onlineMap);
});


app.post('/lux', (req, res) => {
  const { lux, id } = req.body;

  if (typeof lux !== 'number' || !id) {
    return res.status(400).json({ message: '缺少 id 或 lux 值无效' });
  }

  // 更新文件
  const luxMap = JSON.parse(fs.readFileSync(luxFile));
  luxMap[id] = {
    value: lux,
    time: Date.now()
  };
  fs.writeFileSync(luxFile, JSON.stringify(luxMap, null, 2));

 // console.log(`📩 光照数据：${id} = ${lux} lux`);

  const message = JSON.stringify({
    type: 'lux',
    id,
    value: lux,
    time: Date.now()
  });

  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(message);
    }
  });

  res.json({ message: '光照值已接收', id, lux });
});


app.get('/lux/:id', (req, res) => {
  const id = req.params.id;
  const luxMap = JSON.parse(fs.readFileSync(luxFile));

  if (!luxMap[id]) {
    return res.status(404).json({ message: '未找到该 ID 的光照数据' });
  }

  res.json(luxMap[id]);
});


app.post('/devices', (req, res) => {
  const device = req.body;
  let list = JSON.parse(fs.readFileSync(devicesFile));

  if (list.some(d => d.id === device.id)) {
    return res.status(400).json({ message: '设备已存在' });
  }

  const fullDevice = { ...device,
  auto: true,
  recommendedBrightness: device.brightness,
  recommendedTemp:       device.temp,
  fabric:                device.fabric,
  mainColorRGB:          device.mainColorRGB,
  brightness:            device.brightness,
  temp:                  device.temp
};
  list.push(fullDevice);
  fs.writeFileSync(devicesFile, JSON.stringify(list, null, 2));
  console.log(`✅ 设备 ${device.id} 已由客户端添加`);

  // 通知设备停止广播和上报
  const stopAnnounceUrl = `http://${device.ip}/stopAnnounce`;
  http.get(stopAnnounceUrl, () => {
    console.log(`✅ 已通知设备 ${device.id} 停止上报`);
  }).on('error', err => {
    console.warn(`⚠️ 通知 ${device.id} 停止上报失败:`, err.message);
  });

  const stopBroadcastUrl = `http://${device.ip}/stopBroadcast`;
  http.get(stopBroadcastUrl, () => {
    console.log(`✅ 已通知设备 ${device.id} 停止广播`);
  }).on('error', err => {
    console.warn(`⚠️ 通知 ${device.id} 停止广播失败:`, err.message);
  });
const devicesList = JSON.parse(fs.readFileSync(devicesFile));
 wss.clients.forEach(ws => {
  if (ws.readyState === ws.OPEN) {
    devicesList.forEach(dev => {
      ws.send(JSON.stringify({
        type:                   'state',
        id:                     dev.id,
        ip:                     dev.ip,
        brightness:             dev.brightness,
        temp:                   dev.temp,
        recommendedBrightness:  dev.recommendedBrightness,
        recommendedTemp:        dev.recommendedTemp,
        auto:                   dev.auto,
        mainColorRGB:           dev.mainColorRGB,
        fabric:                 dev.fabric
      }));
    });console.log(`所有客户端更新`)
  }
});
  res.json({ message: '添加成功', device: fullDevice });
});



app.post('/announce', (req, res) => {
  const { id, ip } = req.body;
  console.log(`设备上线：${id} @ ${ip}`);

  const list = JSON.parse(fs.readFileSync(devicesFile));
  const exists = list.some(dev => dev.id === id);

  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'announce', id, ip }));
    }
  });

  res.json({ status: 'received', added: exists });  
});



app.delete('/devices/:id', (req, res) => {
  const id = req.params.id;
  const list = JSON.parse(fs.readFileSync(devicesFile));

  const device = list.find(dev => dev.id === id); 
  if (!device) {
    return res.status(404).json({ message: '设备不存在' });
  }

  const newList = list.filter(dev => dev.id !== id);
  fs.writeFileSync(devicesFile, JSON.stringify(newList, null, 2));
  console.log(`🗑️ 设备 ${id} 已被客户端删除`);

  const lastSeenMap = JSON.parse(fs.readFileSync(lastSeenFile));
  delete lastSeenMap[id];
  fs.writeFileSync(lastSeenFile, JSON.stringify(lastSeenMap, null, 2));
  console.log(`🧹 已清除 ${id} 的 lastSeen 记录`);

  if (fs.existsSync(durationFile)) {
    const durationMap = JSON.parse(fs.readFileSync(durationFile));
    if (durationMap[id]) {
      delete durationMap[id];
      fs.writeFileSync(durationFile, JSON.stringify(durationMap, null, 2));
      console.log(`🧽 已清除 ${id} 的 duration 记录`);
    }
  }

  //  通知所有网页端刷新当前所有设备状态
  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN) {
      newList.forEach(dev => {
        ws.send(JSON.stringify({
          type: 'state',
          id: dev.id,
          ip: dev.ip,
          brightness: dev.brightness,
          temp: dev.temp,
          recommendedBrightness: dev.recommendedBrightness,
          recommendedTemp: dev.recommendedTemp,
          auto: dev.auto,
          mainColorRGB: dev.mainColorRGB,
          fabric: dev.fabric
        }));
      });
    }
  });
  console.log(`📤 已同步所有客户端设备列表更新`);

  // 通知设备恢复广播
  if (device.ip) {
    const resumeUrl = `http://${device.ip}/resumeBroadcast`;
    http.get(resumeUrl, () => {
      console.log(`📢 已通知设备 ${id} 恢复广播`);
    }).on("error", err => {
      console.log(`❌ 通知设备 ${id} 恢复广播失败:`, err.message);
    });
  }

  res.json({ message: '删除成功' });
});


app.post('/api/status', (req, res) => {
  const { id } = req.body;
  res.json({ online: !!deviceStatusMap[id] });
});

app.put('/devices/:id', (req, res) => {
  const { brightness, temp, auto, mainColorRGB, fabric,recommendedTemp,recommendedBrightness} = req.body;
  const list = JSON.parse(fs.readFileSync(devicesFile));
  const idx  = list.findIndex(d => d.id === req.params.id);
  if (idx < 0) return res.status(404).json({ message: '设备不存在' });
  list[idx] = {
    ...list[idx],
    brightness: Number(brightness),
    temp: Number(temp),
    auto: Boolean(auto),
    recommendedBrightness: Number(recommendedBrightness),
    recommendedTemp: Number(recommendedTemp),
    mainColorRGB,
    fabric
  };
  fs.writeFileSync(devicesFile, JSON.stringify(list, null, 2));

  const msg = JSON.stringify({
    type: 'state',
    id: req.params.id,
    brightness: list[idx].brightness,
    temp: list[idx].temp,
    recommendedTemp: list[idx].recommendedTemp,
    recommendedBrightness: list[idx].recommendedBrightness,
    auto: list[idx].auto,
    mainColorRGB: list[idx].mainColorRGB,
    fabric: list[idx].fabric
  });
  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN) ws.send(msg);
  });

  res.json({ message: '更新成功', device: list[idx] });
});

app.post('/uploadImage', express.raw({ type: 'image/jpeg', limit: '10mb' }), async (req, res) => {
  const deviceId = req.query.device || 'unknown';
  if (!req.body || !req.body.length) return res.status(400).json({ error: 'No image data' });

  const filename = `${deviceId}_${Date.now()}.jpg`;
  const filepath = path.join(uploadDir, filename);
  fs.writeFileSync(filepath, req.body);
  console.log(`Saved upload from ${deviceId} → ${filename}`);

  let mainColorRGB = null;
  let recommendedBrightness = null;
  let recommendedTemp = null;
  try {
    const result = await extractMainColor(filepath);
    mainColorRGB = result.mainColor;
    recommendedBrightness = result.recommendedBrightness;
    recommendedTemp = result.recommendedTemp;
  } catch (e) {
    console.error('主色提取失败:', e);
  }

  let fabric = '未知';
  try {
    fabric = await recognizeFabric(filepath);
  } catch (e) {
    console.error('面料识别失败:', e);
  }

  console.log('🎨 AI 分析完成：');
  console.log('主色 RGB:', mainColorRGB);
  console.log('推荐亮度:', recommendedBrightness);
  console.log('推荐色温:', recommendedTemp);
  console.log('识别面料:', fabric);

  const devicesList = JSON.parse(fs.readFileSync(devicesFile));
  const idx = devicesList.findIndex(d => d.id === deviceId);
  const oldAuto = devicesList[idx]?.auto ?? true;

  if (idx >= 0) {
    devicesList[idx].mainColorRGB = mainColorRGB;
    devicesList[idx].fabric = fabric;
    devicesList[idx].recommendedBrightness = recommendedBrightness ?? devicesList[idx].brightness ?? 50;
    devicesList[idx].recommendedTemp = recommendedTemp ?? devicesList[idx].temp ?? 5000;
    devicesList[idx].auto = oldAuto;
    fs.writeFileSync(devicesFile, JSON.stringify(devicesList, null, 2));
  }

  const payload = JSON.stringify({
    type: 'state',
    id: deviceId,
    mainColorRGB,
    fabric,
    recommendedBrightness,
    recommendedTemp,
    auto: oldAuto
  });

  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN) ws.send(payload);
  });

  res.json({ message: 'Uploaded', filename });
});

app.listen(httpPort, () => {
  console.log(`HTTP 服务已启动: http://localhost:${httpPort}`);
});

wss.on('connection', ws => {
  console.log('有网页端通过 WebSocket 接入');
  const devicesList = JSON.parse(fs.readFileSync(devicesFile));
  devicesList.forEach(dev => {
    ws.send(JSON.stringify({
       type:                   'state',
      id:                     dev.id,
      ip:                     dev.ip,
      brightness:             dev.brightness,
      temp:                   dev.temp,
      recommendedBrightness:  dev.recommendedBrightness,
      recommendedTemp:        dev.recommendedTemp,
      auto:                   dev.auto,
      mainColorRGB:           dev.mainColorRGB,
      fabric:                 dev.fabric
    }));
  });
 try {
  const durationData = JSON.parse(fs.readFileSync(durationFile));
  const msg = { type: 'durationUpdate', data: durationData };
  ws.send(JSON.stringify(msg));
 } catch (e) {
  console.error('发送 duration 数据失败:', e);
 }
  if (fs.existsSync(luxFile)) {
    const luxMap = JSON.parse(fs.readFileSync(luxFile));
    Object.entries(luxMap).forEach(([id, data]) => {
      ws.send(JSON.stringify({
        type: 'lux',
        id,
        value: data.value,
        time: data.time
      }));
    });
  }

  ws.on('message', (msg, isBinary) => {
    if (!msg) return;

    if (isBinary) {
      wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) client.send(msg, { binary: true });
      });
      return;
    }

    const text = msg.toString();
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      return;
    }

    if (data.type === 'announce') {
      deviceStatusMap[data.id] = {
        ws,
        lastSeen: Date.now()
      };
    } else if (data.type === 'ping') {
  if (deviceStatusMap[data.id]) {
    deviceStatusMap[data.id].lastSeen = Date.now();
  } else {
    deviceStatusMap[data.id] = { ws, lastSeen: Date.now() };
  }
  updateLastSeen(data.id);  
 }
    
    if (data.type === 'image' && typeof data.data === 'string') {
      const imgBuf = Buffer.from(data.data, 'base64');
      const filename = `${data.id}_${Date.now()}.jpg`;
      const filepath = path.join(uploadDir, filename);
      try {
        fs.writeFileSync(filepath, imgBuf);
        console.log(`Saved WS image from ${data.id} → ${filename}`);
      } catch (e) {
        console.error('写入图片失败：', e);
      }
    }

    wss.clients.forEach(client => {
      if (client.readyState === WebSocket.OPEN) client.send(text);
    });
  });

  ws.on('close', () => {
    for (const id in deviceStatusMap) {
      if (deviceStatusMap[id].ws === ws) {
        console.log(`设备断开连接: ${id}`);
        delete deviceStatusMap[id];
        break;
      }
    }
  });
});

setInterval(() => {
  const now = Date.now();
  for (const id in deviceStatusMap) {
    if (now - deviceStatusMap[id].lastSeen > 60000) {
      console.log(`设备超时未响应: ${id}`);
      delete deviceStatusMap[id];
    }
  }
}, 10000);

udpServer.on('message', (msg, rinfo) => {
  console.log(`UDP 广播: ${msg} 来自 ${rinfo.address}:${rinfo.port}`);
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) client.send(msg.toString());
  });
});

udpServer.on('listening', () => {
  const addr = udpServer.address();
  console.log(`UDP 监听中 ${addr.address}:${addr.port}`);
});

udpServer.bind(udpPort);

async function extractMainColor(imagePath) {
  const image = await Jimp.read(imagePath);
  const { width, height } = image.bitmap;
  const samples = [];
  const step = 4;
  for (let y = 0; y < height; y += step) {
    for (let x = 0; x < width; x += step) {
      const color = Jimp.intToRGBA(image.getPixelColor(x, y));
      samples.push([color.r, color.g, color.b]);
    }
  }
  const MAX_SAMPLES = 5000;
  const finalSamples = samples.length > MAX_SAMPLES ? samples.sort(() => 0.5 - Math.random()).slice(0, MAX_SAMPLES) : samples;
  const result = await KMeans(finalSamples, 3);
  const center = result.centroids[0].centroid || result.centroids[0].center || result.centroids[0];
  if (!Array.isArray(center)) throw new Error('聚类中心无效: ' + JSON.stringify(center));
  const mainColor = {
    r: Math.round(center[0]),
    g: Math.round(center[1]),
    b: Math.round(center[2])
  };
  const avg = (mainColor.r + mainColor.g + mainColor.b) / 3;
  const recommendedBrightness = Math.max(30, Math.round((1 - avg / 255) * 100));
  let colorRatio = (mainColor.r - mainColor.b) / 255;
  colorRatio = Math.max(-1, Math.min(1, colorRatio));
  const recommendedTemp = Math.round(5250 - 1250 * colorRatio);
  return { mainColor, recommendedBrightness, recommendedTemp };
}

function recognizeFabric(imagePath) {
  return new Promise(async (resolve, reject) => {
    try {
      const imageData = fs.readFileSync(imagePath);
      const res = await axios.post('http://localhost:5001/predict', {
        image_base64: imageData.toString('base64')
      });
      if (res.data && res.data.label) {
        resolve(res.data.label);
      } else {
        resolve('未知');
      }
    } catch (e) {
      console.error('面料识别失败:', e);
      resolve('未知');
    }
  });
}

const durationFile = path.join(__dirname, 'durationMap.json');
if (!fs.existsSync(durationFile)) fs.writeFileSync(durationFile, '{}');

function getDateString() {
  const d = new Date();
  return d.toLocaleDateString('en-CA'); 
}

function updateDuration(id, duration) {
  try {
    let map = {};
    if (fs.existsSync(durationFile)) {
      const data = fs.readFileSync(durationFile);
      map = JSON.parse(data);
    }

    const dateStr = getDateString();

    if (!map[id]) map[id] = {};
    if (!map[id][dateStr]) map[id][dateStr] = 0;

    map[id][dateStr] += duration;

    fs.writeFileSync(durationFile, JSON.stringify(map, null, 2));
  } catch (err) {
    console.error('更新duration失败:', err);
  }
}


app.post('/duration', (req, res) => {
  const { id, duration } = req.body;

  if (!id || typeof duration !== 'number') {
    return res.status(400).json({ message: '缺少 id 或 duration，或类型不正确' });
  }

  updateDuration(id, duration);
  const today = getDateString();

  console.log(`📥 接收到 ${id} 的停留时长：${duration}，已累计到 ${today}`);

  let allDurationData = {};
  try {
    if (fs.existsSync(durationFile)) {
      const data = fs.readFileSync(durationFile);
      allDurationData = JSON.parse(data);
    }
  } catch (err) {
    console.error('读取累计停留数据失败:', err);
  }

  const message = JSON.stringify({
    type: 'durationUpdate',
    data: allDurationData
  });

  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(message);
    }
  });

  res.json({ message: '接收成功', id, today });
});


app.get('/durations', (req, res) => {
  const { start, end } = req.query;
  if (!start || !end) {
    return res.status(400).json({ message: '请提供起始(start)和结束(end)日期，格式为 YYYY-MM-DD' });
  }

  const startDate = new Date(start);
  const endDate = new Date(end);
  if (isNaN(startDate) || isNaN(endDate)) {
    return res.status(400).json({ message: '日期格式不正确，应为 YYYY-MM-DD' });
  }
  if (startDate > endDate) {
    return res.status(400).json({ message: '起始日期不得晚于结束日期' });
  }

  fs.readFile(durationFile, (err, data) => {
    if (err) {
      console.error('读取durationMap失败:', err);
      return res.status(500).json({ message: '服务器内部错误' });
    }
    let map = {};
    try {
      map = JSON.parse(data);
    } catch (e) {
      console.error('JSON解析失败:', e);
      return res.status(500).json({ message: '服务器内部错误' });
    }

    const result = {};
    for (const id in map) {
      const dayMap = map[id];
      let sum = 0;
      for (const date in dayMap) {
        const currentDate = new Date(date);
        if (currentDate >= startDate && currentDate <= endDate) {
          sum += dayMap[date];
        }
      }
      result[id] = sum;
    }

    console.log(`[查询] 期间 ${start} - ${end} 设备数：${Object.keys(result).length}`);

    res.json(result);
  });
});

app.post('/uploadImageDetect', express.raw({ type: 'image/jpeg', limit: '10mb' }), async (req, res) => {
  console.log('收到上传请求，body 长度:', req.body ? req.body.length : 0);

  if (!req.body || !req.body.length) {
    console.log('没有图像数据，返回400');
    return res.status(400).json({ error: 'No image data' });
  }

  const filename = `detect_${Date.now()}.jpg`;
  const filepath = path.join(uploadDir, filename);
  fs.writeFileSync(filepath, req.body);
  console.log(`保存图片到 ${filepath}`);

  try {
    console.log('开始调用 Flask detect_binary 接口');
    const flaskResponse = await axios({
      method: 'post',
      url: 'http://localhost:5000/detect_binary',
      data: req.body,
      headers: { 'Content-Type': 'application/octet-stream' },
      responseType: 'json'
    });
    console.log('Flask 返回结果:', flaskResponse.data);

    const message = JSON.stringify({
      type: 'personDetection',
      data: flaskResponse.data,
      filename
    });
    wss.clients.forEach(client => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });

    res.json({ message: 'AI 推理完成，结果已通过 WebSocket 广播' });
  } catch (err) {
    console.error('调用 AI 推理失败:', err);
    res.status(500).json({ error: 'AI 推理调用失败' });
  }
});

// 服装分析上传（单次触发）
app.post('/devices/:id/clothUpload', (req, res) => {
  console.log('clothUpload 路由被调用');
  const deviceId = req.params.id;
  const socket = deviceStatusMap[deviceId]?.ws;

  if (socket) {
    socket.send(JSON.stringify({ type: 'command' ,cmd:'upload_cloth' }));
    console.log(`[${deviceId}] 触发服装分析上传`);
    res.sendStatus(200);
  } else {
    res.status(404).json({ error: '设备未连接' });
  }
});

// 人流分析上传（开启/关闭）
app.post('/devices/:id/flowUpload', (req, res) => {
  const deviceId = req.params.id;
  const { flowUpload } = req.body;  
  const socket = deviceStatusMap[deviceId]?.ws;

  if (socket) {
    socket.send(JSON.stringify({
      type: 'command',
      cmd:'flow_upload',
      enabled: !!flowUpload
    }));
    console.log(`[${deviceId}] 设置人流上传状态: ${!!flowUpload}`);
    res.sendStatus(200);
  } else {
    res.status(404).json({ error: '设备未连接' });
  }
});

app.post('/devices/:id/arm', (req, res) => {
  const deviceId = req.params.id;
  const { direction } = req.body;

  const socket = deviceStatusMap[deviceId]?.ws;
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    return res.status(404).json({ error: '设备未连接' });
  }

  const message = {
    type: "arm",     
    id: deviceId,
    direction: direction
  };

  socket.send(JSON.stringify(message));
  console.log(`[${deviceId}] 转发机械臂控制指令:`, message);

  res.json({ result: "ok" });
});
