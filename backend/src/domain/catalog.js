// Os nomes e os tipos são os mesmos utilizados pelas entidades Room no Android.
const resources = {
  contents: { table: 'contents', fields: { title: 'text', description: 'text', body: 'text', author: 'text', type: ['ESTUDO', 'Pregação', 'ARTIGO'], imageUrl: 'url?', createdAt: 'timestamp' }, order: 'createdAt DESC, id DESC' },
  songs: { table: 'songs', fields: { number: 'integer', title: 'text', category: 'text', lyrics: 'text', author: 'text?', audioUrl: 'url?' }, order: 'number, id' },
  'daily-messages': { table: 'daily_messages', fields: { message: 'text', bibleReference: 'text', date: 'date' }, order: 'id DESC' },
  books: { table: 'bible_books', fields: { name: 'text', abbreviation: 'text', testament: ['OLD_TESTAMENT', 'NEW_TESTAMENT'], bookOrder: 'integer', chapterCount: 'integer' }, order: 'bookOrder, id' },
  verses: { table: 'bible_verses', fields: { bookId: 'integer', chapter: 'integer', verse: 'integer', text: 'text' }, order: 'bookId, chapter, verse' }
};
class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
module.exports = { resources, HttpError };
