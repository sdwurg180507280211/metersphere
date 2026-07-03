const crypto = require('node:crypto');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

const port = Number(process.env.PORT || 3000);
const jwtSecret = process.env.JWT_SECRET || 'metersphere-onlyoffice-poc-secret';
const onlyofficePublicUrl = process.env.ONLYOFFICE_PUBLIC_URL || 'http://localhost:8089';
const onlyofficeInternalUrl = process.env.ONLYOFFICE_INTERNAL_URL || 'http://document-server';
const pocInternalUrl = process.env.POC_INTERNAL_URL || 'http://poc-app:3000';
const dataDir = '/app/data';
const templateFile = '/app/template.xlsx';
const documentFile = path.join(dataDir, 'sample.xlsx');

let callbackState = {
  status: 'ready',
  message: 'Excel file is ready',
  updatedAt: new Date().toISOString(),
};

fs.mkdirSync(dataDir, { recursive: true });
if (!fs.existsSync(documentFile)) {
  fs.copyFileSync(templateFile, documentFile);
}

function json(res, status, body) {
  const payload = Buffer.from(JSON.stringify(body));
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': payload.length,
    'Cache-Control': 'no-store',
  });
  res.end(payload);
}

function base64url(value) {
  return Buffer.from(value).toString('base64url');
}

function signJwt(payload) {
  const header = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = base64url(JSON.stringify(payload));
  const signature = crypto
    .createHmac('sha256', jwtSecret)
    .update(`${header}.${body}`)
    .digest('base64url');
  return `${header}.${body}.${signature}`;
}

function verifyJwt(token) {
  const parts = token.split('.');
  if (parts.length !== 3) return false;
  const expected = crypto
    .createHmac('sha256', jwtSecret)
    .update(`${parts[0]}.${parts[1]}`)
    .digest();
  const actual = Buffer.from(parts[2], 'base64url');
  return expected.length === actual.length && crypto.timingSafeEqual(expected, actual);
}

function editorConfig() {
  const stat = fs.statSync(documentFile);
  const key = crypto
    .createHash('sha256')
    .update(`${stat.size}:${stat.mtimeMs}`)
    .digest('hex')
    .slice(0, 32);
  const config = {
    document: {
      fileType: 'xlsx',
      key,
      title: 'MeterSphere Excel POC.xlsx',
      url: `${pocInternalUrl}/files/sample.xlsx?v=${stat.mtimeMs}`,
      permissions: {
        download: true,
        edit: true,
        print: true,
      },
    },
    documentType: 'cell',
    editorConfig: {
      callbackUrl: `${pocInternalUrl}/callback`,
      lang: 'zh-CN',
      mode: 'edit',
      user: {
        id: 'metersphere-local-user',
        name: 'MeterSphere 本地用户',
      },
      customization: {
        autosave: true,
        forcesave: true,
      },
    },
  };
  return { ...config, token: signJwt(config) };
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > 1024 * 1024) {
        reject(new Error('Request body is too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

async function saveEditedDocument(downloadUrl, status) {
  const resolvedUrl = new URL(downloadUrl);
  if (resolvedUrl.hostname === 'localhost' || resolvedUrl.hostname === '127.0.0.1') {
    const internalUrl = new URL(onlyofficeInternalUrl);
    resolvedUrl.protocol = internalUrl.protocol;
    resolvedUrl.hostname = internalUrl.hostname;
    resolvedUrl.port = internalUrl.port;
  }
  console.log(`Downloading edited document from ${resolvedUrl}`);
  const response = await fetch(resolvedUrl);
  if (!response.ok) {
    throw new Error(`Document download failed with HTTP ${response.status}`);
  }
  const tempFile = `${documentFile}.tmp`;
  const data = Buffer.from(await response.arrayBuffer());
  fs.writeFileSync(tempFile, data);
  fs.renameSync(tempFile, documentFile);
  callbackState = {
    status: 'saved',
    documentStatus: status,
    downloadUrl: resolvedUrl.toString(),
    message: `Saved ${data.length} bytes`,
    updatedAt: new Date().toISOString(),
  };
}

function htmlPage() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ONLYOFFICE Excel POC</title>
  <style>
    * { box-sizing: border-box; }
    html, body { width: 100%; height: 100%; margin: 0; overflow: hidden; font-family: Arial, sans-serif; color: #202124; }
    body { display: grid; grid-template-rows: 48px minmax(0, 1fr); background: #f4f5f7; }
    header { display: flex; align-items: center; gap: 12px; padding: 0 16px; border-bottom: 1px solid #dfe1e5; background: #fff; }
    h1 { margin: 0; font-size: 16px; font-weight: 600; letter-spacing: 0; white-space: nowrap; }
    .spacer { flex: 1; }
    #status { max-width: 45vw; overflow: hidden; text-overflow: ellipsis; color: #5f6368; font-size: 13px; white-space: nowrap; }
    button { height: 32px; padding: 0 12px; border: 1px solid #c7cbd1; border-radius: 4px; background: #fff; color: #202124; cursor: pointer; font-size: 13px; }
    button:hover { background: #f1f3f4; }
    button:disabled { cursor: wait; opacity: .6; }
    #editor { width: 100%; height: 100%; min-height: 0; }
    .error { display: grid; place-items: center; height: 100%; padding: 24px; color: #b3261e; background: #fff; }
    @media (max-width: 680px) {
      header { padding: 0 8px; gap: 6px; }
      h1 { font-size: 14px; }
      #status { display: none; }
      button { padding: 0 8px; }
    }
  </style>
</head>
<body>
  <header>
    <h1>ONLYOFFICE Excel POC</h1>
    <span id="status">正在连接</span>
    <span class="spacer"></span>
    <button id="reload" type="button">重新加载</button>
    <button id="reset" type="button">重置文件</button>
  </header>
  <main id="editor"></main>
  <script src="${onlyofficePublicUrl}/web-apps/apps/api/documents/api.js"></script>
  <script>
    let editor;

    async function updateStatus() {
      try {
        const response = await fetch('/state', { cache: 'no-store' });
        const state = await response.json();
        document.getElementById('status').textContent = state.status + ' · ' + state.updatedAt;
      } catch (error) {
        document.getElementById('status').textContent = '状态获取失败';
      }
    }

    async function openEditor() {
      const root = document.getElementById('editor');
      try {
        if (editor) editor.destroyEditor();
        root.innerHTML = '';
        const response = await fetch('/config', { cache: 'no-store' });
        if (!response.ok) throw new Error('配置请求失败');
        const config = await response.json();
        editor = new DocsAPI.DocEditor('editor', config);
      } catch (error) {
        root.innerHTML = '<div class="error">' + error.message + '</div>';
      }
    }

    document.getElementById('reload').addEventListener('click', openEditor);
    document.getElementById('reset').addEventListener('click', async (event) => {
      event.currentTarget.disabled = true;
      try {
        await fetch('/reset', { method: 'POST' });
        await openEditor();
        await updateStatus();
      } finally {
        event.currentTarget.disabled = false;
      }
    });

    openEditor();
    updateStatus();
    setInterval(updateStatus, 3000);
  </script>
</body>
</html>`;
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  try {
    if (req.method === 'GET' && url.pathname === '/') {
      const body = Buffer.from(htmlPage());
      res.writeHead(200, {
        'Content-Type': 'text/html; charset=utf-8',
        'Content-Length': body.length,
        'Cache-Control': 'no-store',
      });
      res.end(body);
      return;
    }

    if (req.method === 'GET' && url.pathname === '/health') {
      json(res, 200, { status: 'ok' });
      return;
    }

    if (req.method === 'GET' && url.pathname === '/config') {
      json(res, 200, editorConfig());
      return;
    }

    if (req.method === 'GET' && url.pathname === '/state') {
      json(res, 200, callbackState);
      return;
    }

    if ((req.method === 'GET' || req.method === 'HEAD') && url.pathname === '/files/sample.xlsx') {
      const stat = fs.statSync(documentFile);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'Content-Length': stat.size,
        'Content-Disposition': 'inline; filename="sample.xlsx"',
        'Cache-Control': 'no-store',
      });
      if (req.method === 'HEAD') {
        res.end();
        return;
      }
      fs.createReadStream(documentFile).pipe(res);
      return;
    }

    if (req.method === 'POST' && url.pathname === '/reset') {
      fs.copyFileSync(templateFile, documentFile);
      const now = new Date();
      fs.utimesSync(documentFile, now, now);
      callbackState = {
        status: 'reset',
        message: 'Template restored',
        updatedAt: now.toISOString(),
      };
      json(res, 200, { error: 0 });
      return;
    }

    if (req.method === 'POST' && url.pathname === '/callback') {
      const authorization = req.headers.authorization || '';
      const token = authorization.replace(/^Bearer\s+/i, '');
      if (!token || !verifyJwt(token)) {
        json(res, 403, { error: 1, message: 'Invalid callback token' });
        return;
      }
      const payload = JSON.parse(await readBody(req));
      callbackState = {
        status: 'callback',
        documentStatus: payload.status,
        message: `Callback status ${payload.status}`,
        updatedAt: new Date().toISOString(),
      };
      if ((payload.status === 2 || payload.status === 6) && payload.url) {
        await saveEditedDocument(payload.url, payload.status);
      }
      json(res, 200, { error: 0 });
      return;
    }

    json(res, 404, { error: 'Not found' });
  } catch (error) {
    callbackState = {
      status: 'error',
      message: error.message,
      updatedAt: new Date().toISOString(),
    };
    json(res, 500, { error: error.message });
  }
});

server.listen(port, '0.0.0.0', () => {
  console.log(`ONLYOFFICE POC host listening on port ${port}`);
  console.log(`Document Server internal URL: ${onlyofficeInternalUrl}`);
});
