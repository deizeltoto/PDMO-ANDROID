'use strict';
const definitions = {
  contents: { label: 'Conteúdos', fields: { title: ['Título'], description: ['Descrição', 'textarea'], body: ['Texto completo', 'textarea'], author: ['Autor'], type: ['Tipo', ['ESTUDO','Pregação','ARTIGO']], imageUrl: ['URL da imagem', 'url', true] } },
  songs: { label: 'Cânticos', fields: { number: ['Número', 'number'], title: ['Título'], category: ['Categoria'], lyrics: ['Letra', 'textarea'], author: ['Autor', 'text', true], audioUrl: ['Link directo do áudio', 'url', true] } },
  'daily-messages': { label: 'Mensagens do dia', fields: { message: ['Mensagem', 'textarea'], bibleReference: ['Referência bíblica'], date: ['Data', 'date'] } },
  books: { label: 'Livros da Bíblia', fields: { name: ['Nome'], abbreviation: ['Abreviatura'], testament: ['Testamento', ['OLD_TESTAMENT', 'NEW_TESTAMENT']], bookOrder: ['Ordem', 'number'], chapterCount: ['Total de capítulos', 'number'] } },
  verses: { label: 'Versículos', fields: { bookId: ['Livro', 'book'], chapter: ['Capítulo', 'number'], verse: ['Versículo', 'number'], text: ['Texto', 'textarea'] } }
};
const $ = id => document.getElementById(id);
let resource = 'contents', token = '', items = [], editing = null, requestNumber = 0;
async function api(path, options = {}) {
  const response = await fetch('/api' + path, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}), ...options.headers } });
  if (response.status === 204) return null;
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'Falha na ligação ao servidor.');
  return data;
}
function status(message, error = false) { $('status').textContent = message; $('status').className = error ? 'error' : ''; }
function authState() { $('new').disabled = !token; $('logout').hidden = !token; $('authStatus').textContent = token ? 'Administração activa nesta página.' : 'Consulta disponível. Entre para criar, editar e eliminar registos.'; }
function button(text, action, className = '') { const el = document.createElement('button'); el.type = 'button'; el.textContent = text; el.className = className; el.addEventListener('click', action); return el; }
for (const [key, definition] of Object.entries(definitions)) {
  const el = button(definition.label, () => { resource = key; $('search').value = ''; load(); });
  el.dataset.resource = key;
  $('tabs').append(el);
}
async function load() {
  const request = ++requestNumber;
  const current = resource;
  $('title').textContent = definitions[current].label;
  document.querySelectorAll('#tabs button').forEach(el => el.setAttribute('aria-current', String(el.dataset.resource === current)));
  $('rows').replaceChildren(); $('count').textContent = 'A carregar…';
  try {
    const result = await api('/' + current + '?q=' + encodeURIComponent($('search').value));
    if (request !== requestNumber) return;
    items = result;
    $('count').textContent = `${items.length} registo(s)`;
    for (const item of items) {
      const tr = document.createElement('tr');
      const title = item.title || item.name || item.message || item.text;
      const details = current === 'verses' ? `Livro ${item.bookId} · ${item.chapter}:${item.verse}` : current === 'books' ? `${item.chapterCount} capítulos` : current === 'songs' ? `N.º ${item.number} · ${item.category}` : item.type || `${item.bibleReference} · ${item.date}`;
      for (const text of [item.id, title, details]) { const td = document.createElement('td'); td.textContent = String(text).slice(0, 180); tr.append(td); }
      const actions = document.createElement('td');
      actions.append(button(token ? 'Editar' : 'Ver', () => edit(item), 'secondary'));
      if (token) actions.append(button('Eliminar', () => remove(item), 'danger'));
      tr.append(actions); $('rows').append(tr);
    }
    if (!items.length) { const tr = document.createElement('tr'); const td = document.createElement('td'); td.colSpan = 4; td.textContent = 'Nenhum registo encontrado.'; tr.append(td); $('rows').append(tr); }
  } catch (error) { if (request === requestNumber) { status(error.message, true); $('count').textContent = 'Não foi possível carregar os registos.'; } }
}
async function edit(item = null) {
  editing = item;
  $('fields').replaceChildren(); $('formError').textContent = '';
  $('editTitle').textContent = `${item ? (token ? 'Editar' : 'Ver') : 'Novo'} · ${definitions[resource].label}`;
  let books = [];
  try { if (resource === 'verses') books = await api('/books'); }
  catch (error) { status(error.message, true); return; }
  for (const [key, [label, type = 'text', optional = false]] of Object.entries(definitions[resource].fields)) {
    const field = document.createElement('label'); field.textContent = label + (optional ? ' (opcional)' : '');
    const input = document.createElement(Array.isArray(type) || type === 'book' ? 'select' : type === 'textarea' ? 'textarea' : 'input');
    if (input.tagName === 'INPUT') input.type = type;
    if (Array.isArray(type) || type === 'book') {
      const options = type === 'book' ? books.map(book => [book.id, `${book.id} · ${book.name}`]) : type.map(value => [value, value === 'OLD_TESTAMENT' ? 'Antigo Testamento' : value === 'NEW_TESTAMENT' ? 'Novo Testamento' : value]);
      for (const [value, text] of options) { const option = document.createElement('option'); option.value = value; option.textContent = text; input.append(option); }
    }
    input.name = key; input.required = !optional;
    if (type === 'number') { input.min = '1'; input.max = '2147483647'; input.step = '1'; }
    if (item) input.value = item[key] ?? '';
    else if (type === 'date') input.value = new Date().toISOString().slice(0, 10);
    input.disabled = !token || Boolean(item && resource === 'verses' && ['bookId','chapter','verse'].includes(key));
    field.append(input); $('fields').append(field);
  }
  if (resource === 'songs') {
    const help = document.createElement('p');
    help.textContent = 'Use um link directo para MP3, M4A, OGG ou WAV acessível pelo telemóvel. Links de páginas do YouTube não são ficheiros de áudio. Deixe vazio para manter apenas a letra.';
    const audio = document.createElement('audio');
    audio.controls = true; audio.preload = 'none'; audio.id = 'audioPreview';
    audio.setAttribute('aria-label', 'Pré-escuta do cântico');
    const previewStatus = document.createElement('p');
    previewStatus.setAttribute('role', 'status');
    const input = $('editForm').elements.namedItem('audioUrl');
    const updateAudio = () => {
      audio.pause(); audio.removeAttribute('src'); audio.load();
      const value = input.value.trim();
      let valid = false;
      try { const url = new URL(value); valid = ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password; } catch { }
      audio.hidden = !valid;
      previewStatus.textContent = value && !valid ? 'Informe um link HTTP ou HTTPS válido.' : '';
      if (valid) audio.src = value;
    };
    audio.addEventListener('error', () => { if (audio.hasAttribute('src')) previewStatus.textContent = 'Não foi possível ouvir este áudio. Verifique o link, o acesso e o formato.'; });
    input.addEventListener('change', updateAudio);
    $('fields').append(help, audio, previewStatus);
    updateAudio();
  }
  $('save').hidden = !token; $('save').disabled = false; $('cancel').textContent = token ? 'Cancelar' : 'Fechar';
  $('editor').showModal();
}
async function remove(item) {
  if (!confirm(`Eliminar o registo #${item.id}? A aplicação irá removê-lo na próxima sincronização.`)) return;
  try { await api(`/${resource}/${item.id}`, { method: 'DELETE' }); status('Registo eliminado. Sincronize a aplicação.'); await load(); }
  catch (error) { status(error.message, true); }
}
$('editForm').addEventListener('submit', async event => {
  event.preventDefault(); $('save').disabled = true; $('formError').textContent = '';
  const data = {};
  for (const [key, [, type, optional]] of Object.entries(definitions[resource].fields)) {
    const value = $('editForm').elements.namedItem(key).value;
    data[key] = type === 'number' || type === 'book' ? Number(value) : optional && !value.trim() ? null : value;
  }
  try { await api('/' + resource + (editing ? '/' + editing.id : ''), { method: editing ? 'PUT' : 'POST', body: JSON.stringify(data) }); $('editor').close(); status('Guardado. Sincronize a aplicação para receber as alterações.'); await load(); }
  catch (error) { $('formError').textContent = error.message; }
  finally { $('save').disabled = false; }
});
$('login').addEventListener('submit', async event => {
  event.preventDefault(); token = $('token').value.trim();
  try { await api('/admin/session', { method: 'POST' }); $('token').value = ''; status('Sessão iniciada.'); }
  catch (error) { token = ''; status(error.message, true); }
  authState(); load();
});
$('logout').addEventListener('click', () => { token = ''; authState(); status('Sessão terminada.'); load(); });
$('new').addEventListener('click', () => edit());
$('cancel').addEventListener('click', () => $('editor').close());
$('editor').addEventListener('close', () => {
  const audio = $('audioPreview');
  if (audio) { audio.pause(); audio.removeAttribute('src'); audio.load(); }
});
$('refresh').addEventListener('click', () => { status(''); load(); });
$('searchForm').addEventListener('submit', event => { event.preventDefault(); load(); });
api('/health').then(() => { $('connection').textContent = 'Servidor disponível'; }).catch(() => { $('connection').textContent = 'Sem ligação'; });
load();
