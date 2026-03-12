import http from "k6/http";

export const options = {
  vus: 200,
  duration: "5s",
};

export default function () {

  const key = "contention-test-" + (__VU % 20);

  http.post(
    "http://localhost:8081/execute?delay=500",
    null,
    {
      headers: {
        "Idempotency-Key": key
      }
    }
  );
}