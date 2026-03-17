import http from "k6/http";
import { sleep } from "k6";

// CONFIG
export const options = {
  scenarios: {
    steady_load: {
      executor: "constant-vus",
      vus: 10,          // number of concurrent users
      duration: "2m",   // test duration
    },
  },
};

// MAIN TEST
export default function () {

  const url = "http://localhost:8080/api/v1/workflow";

  const payload = JSON.stringify({
    steps: [
      {
        externalUrl: "http://localhost:8081/execute?mode=random"
      }
    ]
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  const res = http.post(url, payload, params);

  sleep(0.2); // small think time
}