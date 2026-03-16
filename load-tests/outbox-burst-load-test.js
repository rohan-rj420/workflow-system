import http from "k6/http";

export const options = {
  vus: 50,
  iterations: 500
};

export default function () {

  const payload = JSON.stringify({
    steps: [
      {
        externalUrl: "http://localhost:8081/execute?delay=200"
      }
    ]
  });

  http.post(
    "http://localhost:8080/api/v1/workflow",
    payload,
    { headers: { "Content-Type": "application/json" } }
  );
}