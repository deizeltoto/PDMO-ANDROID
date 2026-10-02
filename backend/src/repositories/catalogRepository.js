const { resources } = require('../domain/catalog');
const { transaction } = require('../database/connection');

class CatalogRepository {
  constructor(db) { this.db = db; }
  list(resource) {
    const { table, order } = resources[resource];
    return this.db.prepare(`SELECT * FROM ${table} ORDER BY ${order}`).all();
  }
  get(resource, id) { return this.db.prepare(`SELECT * FROM ${resources[resource].table} WHERE id = ?`).get(id); }
  save(resource, data, id) {
    const { table } = resources[resource];
    const keys = Object.keys(data);
    return transaction(this.db, () => {
      if (id) this.db.prepare(`UPDATE ${table} SET ${keys.map(key => `${key} = ?`).join(', ')} WHERE id = ?`).run(...Object.values(data), id);
      else id = Number(this.db.prepare(`INSERT INTO ${table} (${keys.join(', ')}) VALUES (${keys.map(() => '?').join(', ')})`).run(...Object.values(data)).lastInsertRowid);
      this.bumpRevision();
      return this.get(resource, id);
    });
  }
  delete(resource, id) {
    transaction(this.db, () => {
      this.db.prepare(`DELETE FROM ${resources[resource].table} WHERE id = ?`).run(id);
      this.bumpRevision();
    });
  }
  bumpRevision() { this.db.exec("UPDATE metadata SET value = CAST(value AS INTEGER) + 1 WHERE key = 'revision'"); }
  snapshot() {
    return transaction(this.db, () => ({
      schemaVersion: 1,
      revision: Number(this.db.prepare("SELECT value FROM metadata WHERE key = 'revision'").get().value),
      generatedAt: new Date().toISOString(),
      contents: this.list('contents'), songs: this.list('songs'), dailyMessages: this.list('daily-messages'),
      books: this.list('books'), verses: this.list('verses')
    }));
  }
}
module.exports = { CatalogRepository };
