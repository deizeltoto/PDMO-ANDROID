const path = require('node:path');
const { openDatabase } = require('./src/database/connection');
const { createApp } = require('./src/app');
const db = openDatabase(path.resolve(__dirname, process.env.DB_PATH || 'data/pdmo.sqlite'));
const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || '0.0.0.0';
const server = createApp(db, { adminToken: process.env.ADMIN_TOKEN }).listen(port, host, () => {
  console.log(`Painel PDMO: http://localhost:${port}`);
  console.log('Android (emulador): http://10.0.2.2:' + port);
  if (!process.env.ADMIN_TOKEN) console.log('Modo de leitura: configure ADMIN_TOKEN em .env para editar.');
});
server.on('error', error => { console.error(error.message); db.close(); process.exitCode = 1; });
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => server.close(() => { db.close(); process.exit(0); }));
