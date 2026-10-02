const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { once } = require('node:events');
const { openDatabase } = require('../src/database/connection');
const { createApp } = require('../src/app');

async function fixture(t, token = 'test-admin-token') {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'pdmo-api-'));
  const filename = path.join(directory, 'test.sqlite');
  const db = openDatabase(filename);
  const server = createApp(db, { adminToken: token }).listen(0, '127.0.0.1');
  await once(server, 'listening');
  t.after(async () => {
    await new Promise(resolve => server.close(resolve));
    db.close();
    fs.rmSync(directory, { recursive: true });
  });
  const base = `http://127.0.0.1:${server.address().port}`;
  async function request(url, method = 'GET', data, authenticated = true) {
    const response = await fetch(base + url, { method, headers: {
      'Content-Type': 'application/json', ...(authenticated ? { Authorization: 'Bearer test-admin-token' } : {})
    }, body: data === undefined ? undefined : JSON.stringify(data) });
    const body = response.status === 204 ? null : await response.json();
    return { status: response.status, body };
  }
  return { request, db, filename, base };
}
const content = { title: 'Teste de integração', description: 'Descrição', body: 'Texto completo', author: 'Autor', type: 'ESTUDO', imageUrl: null };

test('áudio do cântico: criação, sincronização, compatibilidade e remoção', async t => {
  const { request } = await fixture(t);
  const song = { number: 100, title: 'Com áudio', category: 'Louvor', lyrics: 'Letra completa', author: null, audioUrl: 'https://example.com/cantico.mp3' };
  const created = await request('/api/songs', 'POST', song);
  assert.equal(created.status, 201);
  assert.equal(created.body.audioUrl, song.audioUrl);
  const id = created.body.id;
  const synced = (await request('/api/sync')).body.songs.find(item => item.id === id);
  assert.equal(synced.audioUrl, song.audioUrl);
  assert.equal(synced.lyrics, song.lyrics);
  const { audioUrl, ...legacy } = song;
  assert.equal((await request(`/api/songs/${id}`, 'PUT', legacy)).body.audioUrl, audioUrl);
  for (const invalid of ['javascript:alert(1)', 'file:///audio.mp3', 'https://user:password@example.com/audio.mp3', 'nao-e-url']) {
    assert.equal((await request(`/api/songs/${id}`, 'PUT', { ...song, audioUrl: invalid })).status, 400);
  }
  assert.equal((await request(`/api/songs/${id}`, 'PUT', { ...song, audioUrl: null })).body.audioUrl, null);
  assert.equal((await request('/api/songs/1')).body.audioUrl, null);
});

test('migração do banco antigo conserva os cânticos e acrescenta áudio opcional', async t => {
  const { db, filename } = await fixture(t);
  const original = db.prepare('SELECT id, lyrics FROM songs ORDER BY id').all();
  db.exec('ALTER TABLE songs DROP COLUMN audioUrl');
  const migrated = openDatabase(filename);
  try {
    assert.deepEqual(migrated.prepare('SELECT id, lyrics FROM songs ORDER BY id').all(), original);
    assert.equal(migrated.prepare('SELECT audioUrl FROM songs LIMIT 1').get().audioUrl, null);
  } finally { migrated.close(); }
});

test('API pública, painel e contrato completo de sincronização', async t => {
  const { request, base } = await fixture(t);
  assert.equal((await request('/api/health')).body.status, 'ok');
  const snapshot = (await request('/api/sync')).body;
  assert.equal(snapshot.schemaVersion, 1);
  assert.equal(snapshot.contents.length, 9);
  assert.equal(snapshot.songs.length, 8);
  assert.equal(snapshot.books.length, 13);
  assert.equal(snapshot.verses.length, 70);
  assert.equal(typeof snapshot.contents[0].createdAt, 'number');
  const page = await fetch(base);
  assert.match(await page.text(), /PDMO · Gestão/);
  assert.match(page.headers.get('content-security-policy'), /script-src 'self'/);
  for (const asset of ['/app.js', '/styles.css']) assert.equal((await fetch(base + asset)).status, 200);
});
test('CRUD, pesquisa, IDs não reutilizados e revisão do catálogo', async t => {
  const { request } = await fixture(t);
  const before = (await request('/api/sync')).body.revision;
  const created = await request('/api/contents', 'POST', content);
  assert.equal(created.status, 201);
  const id = created.body.id;
  assert.equal((await request('/api/contents?q=integra%C3%A7%C3%A3o')).body.length, 1);
  assert.equal((await request(`/api/contents/${id}`, 'PUT', { ...content, title: 'Actualizado' })).body.title, 'Actualizado');
  assert.equal((await request(`/api/contents/${id}`, 'DELETE')).status, 204);
  assert.equal((await request(`/api/contents/${id}`)).status, 404);
  assert.equal((await request('/api/sync')).body.revision, before + 3);
  assert.ok((await request('/api/contents', 'POST', content)).body.id > id);
});
test('autenticação obrigatória em todas as operações de escrita', async t => {
  const { request } = await fixture(t);
  for (const [url, method] of [['/api/admin/session','POST'], ['/api/contents','POST'], ['/api/contents/1','PUT'], ['/api/contents/1','DELETE']]) {
    assert.equal((await request(url, method, method === 'DELETE' ? undefined : content, false)).status, 401);
  }
  assert.equal((await request('/api/admin/session', 'POST')).status, 200);
});
test('servidor sem chave funciona apenas em leitura', async t => {
  const { request } = await fixture(t, '');
  assert.equal((await request('/api/contents')).status, 200);
  assert.equal((await request('/api/contents', 'POST', content)).status, 503);
});
test('validação de campos, tipos, data, URL, ID, pesquisa e JSON', async t => {
  const { request, base } = await fixture(t);
  for (const body of [{}, { ...content, type: 'INVALIDO' }, { ...content, id: 50 }, { ...content, imageUrl: 'javascript:alert(1)' }, { ...content, title: ' ' }, { ...content, createdAt: 'ontem' }]) {
    assert.equal((await request('/api/contents', 'POST', body)).status, 400);
  }
  assert.equal((await request('/api/contents/1x')).status, 400);
  assert.equal((await request('/api/contents?q=a&q=b')).status, 400);
  assert.equal((await request('/api/desconhecido')).status, 404);
  assert.equal((await request('/api/constructor')).status, 404);
  assert.equal((await request('/api/daily-messages', 'POST', { message: 'Olá', bibleReference: 'Salmos', date: '2026-02-30' })).status, 400);
  const invalid = await fetch(base + '/api/contents', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{' });
  assert.equal(invalid.status, 400);
  assert.equal((await invalid.json()).error, 'JSON inválido.');
});
test('integridade de livros, capítulos e versículos', async t => {
  const { request } = await fixture(t);
  const book = (await request('/api/books', 'POST', { name: 'Livro de teste', abbreviation: 'LT', testament: 'NEW_TESTAMENT', bookOrder: 100, chapterCount: 2 })).body;
  const verse = { bookId: book.id, chapter: 2, verse: 1, text: 'Texto de teste' };
  assert.equal((await request('/api/verses', 'POST', { ...verse, chapter: 3 })).status, 400);
  const created = await request('/api/verses', 'POST', verse);
  assert.equal(created.status, 201);
  assert.equal((await request('/api/verses', 'POST', verse)).status, 409);
  assert.equal((await request(`/api/books/${book.id}`, 'DELETE')).status, 409);
  const { id, ...bookFields } = book;
  assert.equal((await request(`/api/books/${id}`, 'PUT', { ...bookFields, chapterCount: 1 })).status, 409);
  assert.equal((await request(`/api/verses/${created.body.id}`, 'PUT', { ...verse, chapter: 1 })).status, 400);
  assert.equal((await request(`/api/verses?bookId=${id}&chapter=2`)).body.length, 1);
  assert.equal((await request(`/api/verses/${created.body.id}`, 'DELETE')).status, 204);
  assert.equal((await request(`/api/books/${id}`, 'DELETE')).status, 204);
});
test('cânticos e mensagens guardados no formato consumido pelo Android', async t => {
  const { request } = await fixture(t);
  const song = { number: 100, title: 'Cântico novo', category: 'Louvor', lyrics: 'Linha 1\nLinha 2', author: null };
  assert.equal((await request('/api/songs', 'POST', song)).status, 201);
  assert.equal((await request('/api/songs', 'POST', song)).status, 409);
  assert.equal((await request('/api/songs', 'POST', { ...song, number: '101' })).status, 400);
  const message = { message: 'Mensagem nova', bibleReference: 'Salmos 1:1', date: '2026-10-02' };
  assert.equal((await request('/api/daily-messages', 'POST', message)).status, 201);
  const snapshot = (await request('/api/sync')).body;
  assert.equal(snapshot.dailyMessages[0].message, message.message);
  assert.equal(snapshot.songs.find(item => item.number === 100).lyrics, song.lyrics);
});
test('alterações e catálogo vazio persistem ao reabrir a base', async t => {
  const { request, filename } = await fixture(t);
  const original = (await request('/api/contents')).body;
  for (const item of original) assert.equal((await request(`/api/contents/${item.id}`, 'DELETE')).status, 204);
  const reopened = openDatabase(filename);
  try {
    assert.equal(reopened.prepare('SELECT COUNT(*) AS count FROM contents').get().count, 0);
    assert.equal(Number(reopened.prepare("SELECT value FROM metadata WHERE key='revision'").get().value), 10);
  } finally { reopened.close(); }
});
