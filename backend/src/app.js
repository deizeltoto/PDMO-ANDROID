const express = require('express');
const path = require('node:path');
const { CatalogRepository } = require('./repositories/catalogRepository');
const { CatalogService } = require('./services/catalogService');
const { catalogRoutes } = require('./routes/catalogRoutes');
const { adminAuth } = require('./middleware/auth');
function createApp(db, { adminToken = '' } = {}) {
  const app = express();
  app.disable('x-powered-by');
  app.use((req, res, next) => {
    res.set('X-Content-Type-Options', 'nosniff');
    res.set('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
    next();
  });
  app.use(express.json({ limit: '1mb' }));
  app.use('/api', (req, res, next) => { res.set('Cache-Control', 'no-store'); next(); });
  const repository = new CatalogRepository(db);
  app.use('/api', catalogRoutes(new CatalogService(repository), repository, adminAuth(adminToken)));
  app.use(express.static(path.join(__dirname, '../public')));
  app.use((req, res) => res.status(404).json({ error: 'Rota não encontrada.' }));
  app.use((error, req, res, next) => {
    if (error.code?.startsWith('SQLITE_CONSTRAINT') || error.code === 'ERR_SQLITE_ERROR' && /constraint/i.test(error.message)) return res.status(409).json({ error: 'Registo duplicado ou associado a outros registos. Verifique o número, a ordem ou a referência bíblica.' });
    const status = error.status || 500;
    if (status === 500) console.error(error);
    res.status(status).json({ error: status === 500 ? 'Erro interno do servidor.' : error.type === 'entity.parse.failed' ? 'JSON inválido.' : error.message });
  });
  return app;
}
module.exports = { createApp };
