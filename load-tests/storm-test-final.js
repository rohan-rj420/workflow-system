import http from "k6/http";

export const options = {
  vus: 50,
  iterations: 50
};

export default function () {
  http.post("http://localhost:8081/execute", null, {
    headers: {
      "Idempotency-Key": "storm-test10"
    }
  });
}