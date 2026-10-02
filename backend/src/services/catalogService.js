const { resources, HttpError } = require('../domain/catalog');

class CatalogService {
  constructor(repository) { this.repository = repository; }
  resource(name) { if (!Object.hasOwn(resources, name)) throw new HttpError(404, 'Recurso não encontrado.'); return resources[name]; }
  id(value) {
    if (!/^[1-9]\d*$/.test(String(value)) || !Number.isSafeInteger(Number(value)) || Number(value) > 2147483647) throw new HttpError(400, 'ID inválido.');
    return Number(value);
  }
  get(name, id) {
    this.resource(name);
    const item = this.repository.get(name, this.id(id));
    if (!item) throw new HttpError(404, 'Registo não encontrado.');
    return item;
  }
  list(name, query = {}) {
    this.resource(name);
    let items = this.repository.list(name);
    if (query.q) {
      if (typeof query.q !== 'string' || query.q.length > 200) throw new HttpError(400, 'Pesquisa inválida.');
      const q = query.q.toLocaleLowerCase('pt');
      items = items.filter(item => Object.values(item).some(value => String(value ?? '').toLocaleLowerCase('pt').includes(q)));
    }
    for (const key of ['bookId', 'chapter', 'type', 'category', 'testament']) {
      if (query[key] !== undefined) {
        if (typeof query[key] !== 'string') throw new HttpError(400, 'Filtro inválido.');
        items = items.filter(item => String(item[key]) === query[key]);
      }
    }
    return items;
  }
  save(name, body, id) {
    const config = this.resource(name);
    const existing = id ? this.get(name, id) : null;
    if (!body || Array.isArray(body) || typeof body !== 'object') throw new HttpError(400, 'Envie um objeto JSON.');
    for (const key of Object.keys(body)) if (!Object.hasOwn(config.fields, key)) throw new HttpError(400, `Campo desconhecido: ${key}`);
    const data = {};
    for (const [key, kind] of Object.entries(config.fields)) {
      let value = body[key];
      if (key === 'audioUrl' && value === undefined) value = existing?.audioUrl ?? null;
      if (key === 'createdAt' && value === undefined) value = existing?.createdAt ?? Date.now();
      if (typeof value === 'string') value = value.trim();
      if (typeof kind === 'string' && kind.endsWith('?') && (value == null || value === '')) { data[key] = null; continue; }
      if (kind === 'integer' || kind === 'timestamp') {
        if (!Number.isSafeInteger(value) || value < 1 || value > (kind === 'integer' ? 2147483647 : 8640000000000000)) throw new HttpError(400, `${key}: informe um número inteiro positivo válido.`);
      } else if (typeof value !== 'string' || !value || value.length > (['body','lyrics','text'].includes(key) ? 100000 : 2000)) {
        throw new HttpError(400, `${key}: texto obrigatório ou demasiado longo.`);
      }
      if (Array.isArray(kind) && !kind.includes(value)) throw new HttpError(400, `${key}: escolha ${kind.join(', ')}.`);
      if (kind === 'date' && (!/^\d{4}-\d{2}-\d{2}$/.test(value) || !Number.isFinite(Date.parse(value)) || new Date(value).toISOString().slice(0,10) !== value)) throw new HttpError(400, 'Data inválida (AAAA-MM-DD).');
      if (kind === 'url?') {
        try {
          const url = new URL(value);
          if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new Error();
        }
        catch { throw new HttpError(400, `${key}: informe um URL HTTP ou HTTPS válido, sem credenciais.`); }
      }
      data[key] = value;
    }
    if (name === 'verses') {
      const book = this.get('books', data.bookId);
      if (data.chapter > book.chapterCount) throw new HttpError(400, 'O capítulo excede o total de capítulos do livro.');
      if (existing && ['bookId','chapter','verse'].some(key => data[key] !== existing[key])) throw new HttpError(400, 'A referência de um versículo existente não pode ser alterada; crie outro registo.');
    }
    if (name === 'books' && existing && this.repository.list('verses').some(v => v.bookId === existing.id && v.chapter > data.chapterCount)) throw new HttpError(409, 'Existem versículos em capítulos superiores ao total informado.');
    return this.repository.save(name, data, existing?.id);
  }
  delete(name, id) { this.get(name, id); this.repository.delete(name, this.id(id)); }
}
module.exports = { CatalogService };
