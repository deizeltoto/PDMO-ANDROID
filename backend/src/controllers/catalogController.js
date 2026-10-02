function catalogController(service) {
  return {
    list: (req, res) => res.json(service.list(req.params.resource, req.query)),
    get: (req, res) => res.json(service.get(req.params.resource, req.params.id)),
    create: (req, res) => res.status(201).json(service.save(req.params.resource, req.body)),
    update: (req, res) => res.json(service.save(req.params.resource, req.body, req.params.id)),
    remove: (req, res) => { service.delete(req.params.resource, req.params.id); res.status(204).end(); }
  };
}
module.exports = { catalogController };
