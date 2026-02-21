import socket
import time

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(('localhost', 4221))

req1 = "GET /echo/first HTTP/1.1\r\nHost: localhost\r\n\r\n"
req2 = "GET /echo/second HTTP/1.1\r\nHost: localhost\r\n\r\n"

# Send both requests in ONE syscall — they land in the TCP buffer together
s.sendall((req1 + req2).encode())

s.settimeout(3.0)
try:
  response1 = s.recv(8192)
  print("Response 1:", response1.decode())

  response2 = s.recv(8192)          # ← this will timeout
  print("Response 2:", response2.decode())
except socket.timeout:
  print("TIMED OUT — server hung on second request")

s.close()