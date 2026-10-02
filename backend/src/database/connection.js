const { DatabaseSync } = require('node:sqlite');
const fs = require('node:fs');
const path = require('node:path');

function transaction(db, action) {
  db.exec('BEGIN IMMEDIATE');
  try { const result = action(); db.exec('COMMIT'); return result; }
  catch (error) { db.exec('ROLLBACK'); throw error; }
}
function openDatabase(filename) {
  if (filename !== ':memory:') fs.mkdirSync(path.dirname(filename), { recursive: true });
  const db = new DatabaseSync(filename);
  db.exec('PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 5000;');
  db.exec(fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8'));
  if (!db.prepare("SELECT 1 FROM metadata WHERE key = 'seeded'").get()) {
    transaction(db, () => {
      const assets = path.resolve(__dirname, '../../..', 'app/src/main/assets');
      const read = name => JSON.parse(fs.readFileSync(path.join(assets, name + '.json'), 'utf8'));
      const contents = read('contents');
      const months = ['janeiro','fevereiro','março','abril','maio','junho','julho','agosto','setembro','outubro','novembro','dezembro'];
      for (const item of contents) {
        const parts = String(item.createdAt).toLowerCase().split(' ');
        const date = Date.UTC(Number(parts[2]), months.indexOf(parts[1]), Number(parts[0]));
        db.prepare('INSERT INTO contents VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run(item.id, item.title, item.description, item.body, item.author, item.type, item.imageUrl ?? null, Number.isFinite(date) ? date : Date.now());
      }
      for (const item of read('songs')) db.prepare('INSERT INTO songs VALUES (?, ?, ?, ?, ?, ?)').run(item.id, item.number, item.title, item.category, item.lyrics, item.author ?? null);
      for (const book of read('bible').books) {
        db.prepare('INSERT INTO bible_books VALUES (?, ?, ?, ?, ?, ?)').run(book.id, book.name, book.abbreviation, book.testament, book.bookOrder, book.chapterCount);
        for (const chapter of book.chapters ?? []) for (const verse of chapter.verses) {
          db.prepare('INSERT INTO bible_verses(bookId, chapter, verse, text) VALUES (?, ?, ?, ?)').run(book.id, chapter.chapter, verse.verse, verse.text);
        }
      }
      db.prepare('INSERT INTO daily_messages(message, bibleReference, date) VALUES (?, ?, ?)').run('Entrega o teu caminho ao Senhor, confia nele, e ele tudo fará.', 'Salmos 37:5', new Date().toISOString().slice(0, 10));
      db.exec("INSERT INTO metadata VALUES ('seeded', '1'); INSERT INTO metadata VALUES ('revision', '1');");
    });
  }
  return db;
}
module.exports = { openDatabase, transaction };
