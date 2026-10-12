import sys
import os
import tempfile
import threading
import time
from http.server import HTTPServer, BaseHTTPRequestHandler

sys.path.insert(0, "app/src/main/python")
import gallery_runner

PORT = 18923
PNG_BYTES = bytes([137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, 99, 52, 0, 0, 0, 10, 73, 68, 65, 84, 120, 156, 99, 0, 1, 0, 0, 5, 0, 1, 13, 10, 45, 180, 0, 0, 0, 0, 73, 69, 78, 68, 174, 66, 96, 130])

class AuthHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        cookie = self.headers.get("Cookie", "")
        if "auth_token=super_secret_cookie_value_xyz" in cookie:
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Content-Length", str(len(PNG_BYTES)))
            self.end_headers()
            self.wfile.write(PNG_BYTES)
        else:
            self.send_response(401)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"Unauthorized: Cookie missing or invalid")

    def log_message(self, format, *args):
        pass

server = HTTPServer(("127.0.0.1", PORT), AuthHandler)
thread = threading.Thread(target=server.serve_forever)
thread.daemon = True
thread.start()
time.sleep(0.5)

test_url = f"http://127.0.0.1:{PORT}/image.png"

# Test A: Without cookie -> should fail (HTTP 401)
td_a = tempfile.mkdtemp()
cb_a = gallery_runner.StreamEmitter(None)
code_a = gallery_runner.run_gallery_dl(["-d", td_a, test_url], cb_a)
files_a = os.listdir(td_a)
print(f"TEST_A_NO_COOKIE: exit_code={code_a}, files_downloaded={len(files_a)}")

# Test B: With valid Netscape cookies.txt -> should succeed and download
cookie_file = os.path.join(tempfile.mkdtemp(), "cookies.txt")
with open(cookie_file, "w") as f:
    f.write("# Netscape HTTP Cookie File\n")
    f.write(f"127.0.0.1\tFALSE\t/\tFALSE\t2147483647\tauth_token\tsuper_secret_cookie_value_xyz\n")

td_b = tempfile.mkdtemp()
cb_b = gallery_runner.StreamEmitter(None)
code_b = gallery_runner.run_gallery_dl(["-d", td_b, "-C", cookie_file, test_url], cb_b)
files_b = []
for r, d, fs in os.walk(td_b):
    for f in fs:
        files_b.append(os.path.join(r, f))
file_size_b = os.path.getsize(files_b[0]) if files_b else 0
print(f"TEST_B_VALID_COOKIE: exit_code={code_b}, files_downloaded={len(files_b)}, file_size={file_size_b}")

# Test C: Corrupted cookie format -> should fail authentication
cookie_bad = os.path.join(tempfile.mkdtemp(), "bad_cookies.txt")
with open(cookie_bad, "w") as f:
    f.write("# Corrupted file without valid tokens\n")

td_c = tempfile.mkdtemp()
cb_c = gallery_runner.StreamEmitter(None)
code_c = gallery_runner.run_gallery_dl(["-d", td_c, "-C", cookie_bad, test_url], cb_c)
print(f"TEST_C_CORRUPT_COOKIE: exit_code={code_c}")

server.shutdown()
