// Panel de métricas en la terminal, una línea por segundo.
//   node herramientas/metricas.mjs
// Lee el Actuator en el puerto 9090, que tiene sus propios hilos:
// sigue respondiendo aunque la API (8080) esté saturada.

// Segunda instancia: PUERTO_API=8082 PUERTO_ACTUATOR=9092 node herramientas/metricas.mjs
const PUERTO_API = process.env.PUERTO_API || '8080';
const BASE = `http://localhost:${process.env.PUERTO_ACTUATOR || '9090'}/actuator/metrics`;

async function metrica(nombre, tag) {
  const url = `${BASE}/${nombre}${tag ? `?tag=${encodeURIComponent(tag)}` : ''}`;
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(800) });
    if (!res.ok) return null;
    const json = await res.json();
    const m = json.measurements.find((x) => x.statistic === 'VALUE' || x.statistic === 'COUNT');
    return m ? m.value : null;
  } catch {
    return null;
  }
}

const fmt = (v, ancho = 4) => (v === null ? '—' : String(Math.round(v))).padStart(ancho);
const pct = (v) => (v === null ? '  —' : `${Math.round(v * 100)}%`.padStart(4));

let anterior = null;

console.log('hora      | conexiones BD  | hilos Tomcat | req/s | CPU proc | heap MB');
console.log('----------+----------------+--------------+-------+----------+--------');

setInterval(async () => {
  const [activas, max, pendientes, hilos, hilosMax, total, cpu, heap] = await Promise.all([
    metrica('hikaricp.connections.active'),
    metrica('hikaricp.connections.max'),
    metrica('hikaricp.connections.pending'),
    metrica('tomcat.threads.busy', `name:http-nio-${PUERTO_API}`),
    metrica('tomcat.threads.config.max', `name:http-nio-${PUERTO_API}`),
    metrica('http.server.requests', 'uri:/api/pedidos/{codigo}'),
    metrica('process.cpu.usage'),
    metrica('jvm.memory.used', 'area:heap'),
  ]);

  const rps = anterior !== null && total !== null ? total - anterior : null;
  if (total !== null) anterior = total;

  const hora = new Date().toLocaleTimeString('es-PE', { hour12: false });
  console.log(
    `${hora}  | ${fmt(activas, 2)}/${fmt(max, 2)} · cola ${fmt(pendientes)} | ${fmt(hilos, 3)}/${fmt(hilosMax, 3)}    | ${fmt(rps, 5)} | ${pct(cpu)}     | ${fmt(heap === null ? null : heap / 1048576, 6)}`
  );
}, 1000);
