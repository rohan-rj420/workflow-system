import http from "k6/http";

export const options = {
  vus: 50,
  duration: "10s",
};

export default function () {

  const key = "storm-test1-user-" + (__VU % 10);

  http.post(
    "http://localhost:8081/execute?delay=200",
    null,
    {
      headers: {
        "Idempotency-Key": key
      }
    }
  );
}