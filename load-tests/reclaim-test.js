import http from "k6/http";

export const options = {
  vus: 20,
  iterations: 20
};

export default function () {
  http.post("http://localhost:8081/execute", null, {
    headers: {
      "Idempotency-Key": "reclaim-test2"
    }
  });
}