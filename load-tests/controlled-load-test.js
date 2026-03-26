import http from "k6/http"; import { sleep, check } from "k6";
// CONFIG export const options = { scenarios: { steady_load: { executor: "constant-vus", vus: 20, // increase to 50 later duration: "3m", // enough time to observe steady state }, }, };
// MAIN TEST export default function () { const url = "http://localhost:8080/api/v1/workflow";
const payload = JSON.stringify({ steps: [ { externalUrl: "http://localhost:8081/execute?mode=random" } ] });
const params = { headers: { "Content-Type": "application/json", }, };
const res = http.post(url, payload, params);
check(res, { "status is 200/201": (r) => r.status === 200 || r.status === 201, });
sleep(0.2); // important: keeps load realistic }