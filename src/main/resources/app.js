var vertx = require('vertx')
var console = require('vertx/console')
var container = require('vertx/container');

var config = container.config;


container.deployVerticle("es.xan.servant.parrot.BrainVerticle", config);
container.deployVerticle("es.xan.servant.parrot.ParrotVerticle", config);
container.deployVerticle("es.xan.servant.parrot.ThermostatVerticle", config);
container.deployVerticle("es.xan.servant.parrot.CoreVerticle", config);
container.deployVerticle("es.xan.servant.parrot.WebServerVerticle", config);
container.deployVerticle("es.xan.servant.parrot.TemperatureVerticle", config);
