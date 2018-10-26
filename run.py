import subprocess
import json
import threading
import queue
import json
import sys
import os
import time

subprocess.call('g++ --std=c++0x -W -Wall -Wno-narrowing -O2 -s -pipe -mmmx -msse -msse2 -msse3 -o out/main.out {}'.format(sys.argv[1]), shell=True)
subprocess.call('javac -d out src/ProductInventoryVis.java', shell=True)

scorefile = "best-score.json"
start = 5
case = 5000
end = start + case
scores = [0]

try:
    with open(scorefile, "r") as f:
        scores = json.loads(f.read())
except FileNotFoundError:
    scores = [1] * end

if end > len(scores):
    scores = [1] * end

def solve(seed):
    return int(subprocess.check_output('java -cp out ProductInventoryVis -exec out/main.out -seed {}'.format(seed), shell=True))

class State:
    count = 0
    rate = 0
    runt = 0
    lock = threading.Lock()

    def add(self, seed):
        runt  = time.time()
        score = solve(seed)
        runt  = time.time() - runt
        if scores[seed] < score:
            scores[seed] = score
        nom = score / scores[seed]
        with self.lock:
            self.count = self.count + 1
            self.rate = (self.rate * (self.count - 1) + nom ) / self.count
            self.runt = (self.runt * (self.count - 1) + runt) / self.count
            print('{}\t{:6d}\t{:.4f}\t{:.4f}\t{:.4f}'.format(seed, score, nom, self.rate, self.runt))

state = State()
q = queue.Queue()

def worker():
    while True:
        seed = q.get()
        if seed is None:
            break
        try:
            state.add(seed)
            q.task_done()
        except ValueError as err:
            print("seed {} : {}".format(seed, err))
            os._exit(1)

num_worker_threads = 4
threads = []
for i in range(num_worker_threads):
    t = threading.Thread(target=worker)
    t.start()
    threads.append(t)

for seed in range(start, start + case):
    q.put(seed)

q.join()

for i in range(num_worker_threads):
    q.put(None)
for t in threads:
    t.join()

with open(scorefile, "w") as f:
    f.write(json.dumps(scores))