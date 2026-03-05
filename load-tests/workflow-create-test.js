import http from 'k6/http';

export const options = {
  vus: 10,
  duration: '30s',
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
    {
      headers: { "Content-Type": "application/json" }
    }
  );
}