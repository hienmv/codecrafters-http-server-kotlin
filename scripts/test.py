import requests
import socket
import threading
import sys

BASE    = "http://localhost:4221"
HOST    = "localhost"
PORT    = 4221
failures = 0

def check(name, condition, actual=""):
    global failures
    if condition:
        print(f"  PASS  {name}")
    else:
        print(f"  FAIL  {name} — got: {actual}")
        failures += 1

# ── helper: read one full HTTP response from a raw socket ─────────────────────
def recv_response(sock):
    data = b""
    while b"\r\n\r\n" not in data:
        chunk = sock.recv(4096)
        if not chunk:
            break
        data += chunk
    header_raw, body = data.split(b"\r\n\r\n", 1)
    content_length = 0
    for line in header_raw.decode().split("\r\n")[1:]:
        if line.lower().startswith("content-length:"):
            content_length = int(line.split(":")[1].strip())
    while len(body) < content_length:
        chunk = sock.recv(4096)
        if not chunk:
            break
        body += chunk
    status_code = int(header_raw.decode().split("\r\n")[0].split(" ")[1])
    return status_code, header_raw.decode(), body

# ── GET / ─────────────────────────────────────────────────────────────────────
print("GET /")
r = requests.get(f"{BASE}/")
check("status 200", r.status_code == 200, r.status_code)

# ── GET /echo/{text} ──────────────────────────────────────────────────────────
print("\nGET /echo/{text}")
r = requests.get(f"{BASE}/echo/hello")
check("status 200",      r.status_code == 200, r.status_code)
check("body = hello",    r.text == "hello",    r.text)

r = requests.get(f"{BASE}/echo/hello world")
check("echo with space", r.text == "hello world", r.text)

# ── GET /echo/{text} with gzip ────────────────────────────────────────────────
print("\nGET /echo/{text}  Accept-Encoding: gzip")
r = requests.get(f"{BASE}/echo/hello", headers={"Accept-Encoding": "gzip"})
check("status 200",              r.status_code == 200,                         r.status_code)
check("Content-Encoding: gzip",  r.headers.get("Content-Encoding") == "gzip", r.headers.get("Content-Encoding"))
check("body decoded = hello",    r.text == "hello",                            r.text)

# ── wrong / unsupported encoding ──────────────────────────────────────────────
print("\nGET /echo/{text}  Accept-Encoding: br (unsupported)")
r = requests.get(f"{BASE}/echo/hello", headers={"Accept-Encoding": "br"})
check("status 200",                r.status_code == 200,                              r.status_code)
check("no Content-Encoding",       "Content-Encoding" not in r.headers,              r.headers.get("Content-Encoding"))
check("body = hello (plain)",      r.content == b"hello",                            r.content)

print("\nGET /echo/{text}  Accept-Encoding: gzip, br (gzip preferred)")
r = requests.get(f"{BASE}/echo/hello", headers={"Accept-Encoding": "gzip, br"})
check("Content-Encoding: gzip",    r.headers.get("Content-Encoding") == "gzip",     r.headers.get("Content-Encoding"))
check("body decoded = hello",      r.text == "hello",                                r.text)

print("\nGET /echo/{text}  Accept-Encoding: invalid-encoding")
r = requests.get(f"{BASE}/echo/hello", headers={"Accept-Encoding": "invalid-encoding"})
check("status 200",                r.status_code == 200,                             r.status_code)
check("no Content-Encoding",       "Content-Encoding" not in r.headers,             r.headers.get("Content-Encoding"))

# ── GET /user-agent ───────────────────────────────────────────────────────────
print("\nGET /user-agent")
r = requests.get(f"{BASE}/user-agent", headers={"User-Agent": "test-agent/1.0"})
check("status 200",             r.status_code == 200,        r.status_code)
check("body = test-agent/1.0",  r.text == "test-agent/1.0", r.text)

# ── POST /files/{name} ────────────────────────────────────────────────────────
print("\nPOST /files/test.txt")
r = requests.post(f"{BASE}/files/test.txt", data=b"hello file content")
check("status 201", r.status_code == 201, r.status_code)

# ── GET /files/{name} ─────────────────────────────────────────────────────────
print("\nGET /files/test.txt")
r = requests.get(f"{BASE}/files/test.txt")
check("status 200",                r.status_code == 200,              r.status_code)
check("body = hello file content", r.content == b"hello file content", r.content)

# ── GET /files/{name} not found ───────────────────────────────────────────────
print("\nGET /files/nonexistent.txt")
r = requests.get(f"{BASE}/files/nonexistent.txt")
check("status 404", r.status_code == 404, r.status_code)

# ── unknown route ─────────────────────────────────────────────────────────────
print("\nGET /unknown")
r = requests.get(f"{BASE}/unknown")
check("status 404", r.status_code == 404, r.status_code)

# ── Connection: close ─────────────────────────────────────────────────────────
print("\nGET /  Connection: close")
r = requests.get(f"{BASE}/", headers={"Connection": "close"})
check("status 200",        r.status_code == 200,                              r.status_code)
check("Connection: close", r.headers.get("Connection", "").lower() == "close", r.headers.get("Connection"))

# ── keep-alive: multiple requests on same TCP connection ──────────────────────
print("\nkeep-alive — 3 sequential requests on same connection")
with socket.create_connection((HOST, PORT)) as s:
    s.settimeout(5.0)
    for i, path in enumerate(["/", "/echo/ping", "/user-agent"], start=1):
        req = f"GET {path} HTTP/1.1\r\nHost: {HOST}\r\nUser-Agent: ka-test\r\n\r\n"
        s.sendall(req.encode())
        code, _, _ = recv_response(s)
        check(f"request {i} ({path}) status 200", code == 200, code)

print("\nkeep-alive — connection stays open after multiple requests")
with socket.create_connection((HOST, PORT)) as s:
    s.settimeout(5.0)
    for _ in range(3):
        s.sendall(f"GET / HTTP/1.1\r\nHost: {HOST}\r\n\r\n".encode())
        recv_response(s)
    # connection should still be open — send one more
    s.sendall(f"GET /echo/alive HTTP/1.1\r\nHost: {HOST}\r\n\r\n".encode())
    code, _, body = recv_response(s)
    check("connection still open after 3 requests", code == 200, code)
    check("4th response body = alive", body == b"alive", body)

print("\nkeep-alive — Connection: close closes after response")
with socket.create_connection((HOST, PORT)) as s:
    s.settimeout(5.0)
    s.sendall(f"GET / HTTP/1.1\r\nHost: {HOST}\r\nConnection: close\r\n\r\n".encode())
    code, headers, _ = recv_response(s)
    check("status 200",        code == 200,                                  code)
    check("Connection: close", "connection: close" in headers.lower(),       headers)
    # server must have closed — next recv returns empty
    closed = s.recv(1) == b""
    check("server closed connection", closed)

# ── concurrent requests ───────────────────────────────────────────────────────
print("\nconcurrent — 20 simultaneous requests")
CONCURRENCY = 20
results = [None] * CONCURRENCY

def fetch(index):
    try:
        r = requests.get(f"{BASE}/echo/req-{index}")
        results[index] = (r.status_code, r.text)
    except Exception as e:
        results[index] = (0, str(e))

threads = [threading.Thread(target=fetch, args=(i,)) for i in range(CONCURRENCY)]
for t in threads:
    t.start()
for t in threads:
    t.join()

for i, (code, body) in enumerate(results):
    check(f"request {i:02d} status 200",       code == 200,              code)
    check(f"request {i:02d} body = req-{i}",   body == f"req-{i}",       body)

print("\nconcurrent — mixed routes under load")
mixed = ["/", "/echo/x", "/user-agent", "/files/nonexistent.txt", "/unknown"]
mixed_results = [None] * len(mixed)

def fetch_mixed(index, path):
    try:
        r = requests.get(f"{BASE}{path}", headers={"User-Agent": "mixed-test"})
        mixed_results[index] = r.status_code
    except Exception as e:
        mixed_results[index] = 0

expected = [200, 200, 200, 404, 404]
threads = [threading.Thread(target=fetch_mixed, args=(i, p)) for i, p in enumerate(mixed)]
for t in threads:
    t.start()
for t in threads:
    t.join()

for i, (path, exp, got) in enumerate(zip(mixed, expected, mixed_results)):
    check(f"{path} status {exp}", got == exp, got)

# ─────────────────────────────────────────────────────────────────────────────
print(f"\n{'All tests passed.' if failures == 0 else f'{failures} test(s) failed.'}")
sys.exit(0 if failures == 0 else 1)
