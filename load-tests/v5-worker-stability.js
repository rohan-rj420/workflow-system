import http from "k6/http";
import { sleep } from "k6";

export const options = {
  vus: 30,        // 20 concurrent users
  iterations: 200 // create 100 workflows total
};

export default function () {

  const payload = JSON.stringify({
    steps: [
      {
        externalUrl: "http://localhost:8081/execute?fail=true"
      }
    ]
  });

  const params = {
    headers: {
      "Content-Type": "application/json"
    }
  };

  http.post(
    "http://localhost:8080/api/v1/workflow",
    payload,
    params
  );

  sleep(0.1);
}