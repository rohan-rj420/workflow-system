import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 20,          // number of concurrent users
  iterations: 200,  // total workflow requests
};

export default function () {

  const payload = JSON.stringify({
    steps: [
      {
        externalUrl: "http://localhost:8081/execute"
      }
    ]
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  const res = http.post(
    "http://localhost:8080/api/v1/workflow",
    payload,
    params
  );

  check(res, {
    "workflow created": (r) => r.status === 201 || r.status === 200,
  });

  sleep(0.05);
}