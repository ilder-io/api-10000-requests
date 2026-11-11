// Prueba de carga del laboratorio: ~10 000 requests por minuto (≈ 167 por segundo).
//   k6 run k6/carga.js
//   k6 run -e RPS=50 k6/carga.js        (otra tasa, para ensayar)
//   k6 run -e DURACION=1m k6/carga.js   (tramo sostenido más corto)
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const RPS = Number(__ENV.RPS || 167);
const DURACION = __ENV.DURACION || '2m';
// Una o varias instancias separadas por comas (reparto por turnos, como un balanceador):
//   k6 run -e URLS=http://localhost:8080,http://localhost:8082 k6/carga.js
const URLS = (__ENV.URLS || __ENV.URL || 'http://localhost:8080').split(',');

export const options = {
  scenarios: {
    diez_mil_por_minuto: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
      stages: [
        { target: RPS, duration: '1m' },   // subimos hasta la tasa objetivo
        { target: RPS, duration: DURACION }, // y la sostenemos
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // el 95 % responde en menos de 500 ms
    http_req_failed: ['rate<0.01'],   // menos del 1 % de errores
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const codigo = `PED-${Math.floor(Math.random() * 1000000) + 1}`;
  const base = URLS[exec.scenario.iterationInTest % URLS.length];
  const res = http.get(`${base}/api/pedidos/${codigo}`, { timeout: '30s' });
  check(res, { 'status 200': (r) => r.status === 200 });
}
