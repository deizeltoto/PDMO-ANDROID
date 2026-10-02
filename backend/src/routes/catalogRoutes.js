const { Router } = require('express');
const { catalogController } = require('../controllers/catalogController');
function catalogRoutes(service, repository, auth) {
  const router = Router();
  const controller = catalogController(service);
  router.get('/health', (req, res) => res.json({ status: 'ok', service: 'pdmo' }));
  router.post('/admin/session', auth, (req, res) => res.json({ authenticated: true }));
  router.get('/sync', (req, res) => res.json(repository.snapshot()));
  router.get('/:resource', controller.list);
  router.get('/:resource/:id', controller.get);
  router.post('/:resource', auth, controller.create);
  router.put('/:resource/:id', auth, controller.update);
  router.delete('/:resource/:id', auth, controller.remove);
  return router;
}
module.exports = { catalogRoutes };
