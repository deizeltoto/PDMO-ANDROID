const { timingSafeEqual } = require('node:crypto');
function adminAuth(token) {
  return (req, res, next) => {
    if (!token) return res.status(503).json({ error: 'Configure ADMIN_TOKEN no servidor para habilitar a gestão.' });
    const expected = Buffer.from(`Bearer ${token}`);
    const received = Buffer.from(req.get('authorization') || '');
    if (received.length !== expected.length || !timingSafeEqual(received, expected)) return res.status(401).json({ error: 'Chave de administração inválida.' });
    next();
  };
}
module.exports = { adminAuth };
